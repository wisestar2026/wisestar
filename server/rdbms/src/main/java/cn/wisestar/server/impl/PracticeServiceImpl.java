package cn.wisestar.server.impl;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.core.uitls.AnswerJudgeUtil;
import cn.wisestar.server.core.uitls.SecurityContextUtils;
import cn.wisestar.server.domain.dto.PracticeResultView;
import cn.wisestar.server.domain.dto.PracticeSubmitRequest;
import cn.wisestar.server.domain.dto.SurveySchema;
import cn.wisestar.server.domain.dto.WrongQuestionQuery;
import cn.wisestar.server.domain.dto.WrongQuestionView;
import cn.wisestar.server.domain.model.PracticeDetail;
import cn.wisestar.server.domain.model.PracticeRecord;
import cn.wisestar.server.domain.model.Template;
import cn.wisestar.server.mapper.PracticeDetailMapper;
import cn.wisestar.server.mapper.PracticeRecordMapper;
import cn.wisestar.server.service.BaseService;
import cn.wisestar.server.service.PracticeService;
import cn.wisestar.server.impl.TemplateServiceImpl;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 练习服务实现（交卷落库 + 错题标记）。
 *
 * <p><b>核心数据流</b>：</p>
 * 前端交卷 → PracticeApi.submitPractice → 本类 submitPractice：
 * <ol>
 *   <li>按 items 中的 questionId 批量回源题目（t_template）；</li>
 *   <li>逐题用 {@link AnswerJudgeUtil} 复核判分（与前端判分语义一致，防篡改）；</li>
 *   <li>汇总得分/答对数后写 t_practice_record，逐题写 t_practice_detail
 *       （is_correct=0 即错题，供阶段二错题本查询）；</li>
 * </ol>
 *
 * <p><b>判分与计分约定</b>（与前端 practiceHelpers 一致）：</p>
 * - 每题分值 = attribute.examScore，无则 1 分；
 * - 答对得分 = 题分；答错/未作答 = 0 分；无标准答案 = 不计分也不当错题；
 * - total_score = 全部题分值之和；score = 答对题分值之和；correct_count = 判对题数。
 *
 * <p><b>健壮性</b>：某题回源失败/判分异常时跳过该题不阻断整单落库（练习记录是学习数据底座，
 * 不能因单题异常丢失整次练习）。</p>
 *
 * @author zhanghaiyang
 */
@Slf4j
@Service
public class PracticeServiceImpl extends BaseService<PracticeRecordMapper, PracticeRecord>
		implements PracticeService {

	/**
	 * 逐题明细 Mapper（t_practice_detail）。
	 */
	private final PracticeDetailMapper practiceDetailMapper;

	/**
	 * 题目服务（回源题目 schema 用于判分）。
	 */
	private final TemplateServiceImpl templateService;

	/**
	 * 构造器注入。
	 *
	 * @param practiceDetailMapper 逐题明细 Mapper
	 * @param templateService      题目服务
	 */
	public PracticeServiceImpl(PracticeDetailMapper practiceDetailMapper, TemplateServiceImpl templateService) {
		this.practiceDetailMapper = practiceDetailMapper;
		this.templateService = templateService;
	}

	/**
	 * 提交一次练习（交卷落库 + 错题标记）。
	 *
	 * @param request 练习交卷请求
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public PracticeResultView submitPractice(PracticeSubmitRequest request) {
		if (request == null || CollectionUtils.isEmpty(request.getItems())) {
			log.warn("practice submit skipped: empty items");
			return new PracticeResultView();
		}
		String userId = SecurityContextUtils.getUserId();

		// 1. 批量回源题目，避免逐题查询
		List<String> questionIds = request.getItems().stream()
				.map(PracticeSubmitRequest.PracticeItem::getQuestionId)
				.filter(java.util.Objects::nonNull)
				.collect(Collectors.toList());
		Map<String, Template> templateMap = templateService.list(
				Wrappers.<Template>lambdaQuery().in(Template::getId, questionIds)).stream()
				.collect(Collectors.toMap(Template::getId, Function.identity(), (a, b) -> a));

		// 2. 逐题复核判分
		List<PracticeDetail> details = new ArrayList<>();
		int correctCount = 0;
		double score = 0;
		double totalScore = 0;
		for (PracticeSubmitRequest.PracticeItem item : request.getItems()) {
			Template template = item.getQuestionId() == null ? null : templateMap.get(item.getQuestionId());
			if (template == null || template.getTemplate() == null) {
				log.warn("practice submit: question {} not found, skipped", item.getQuestionId());
				continue;
			}
			SurveySchema schema = template.getTemplate();
			Integer correct;
			try {
				correct = AnswerJudgeUtil.evaluate(schema, item.getAnswer());
			} catch (Exception e) {
				log.warn("practice submit: judge question {} failed, skipped", item.getQuestionId(), e);
				continue;
			}
			// 每题分值：attribute.examScore，无则 1 分
			double point = schema.getAttribute() != null && schema.getAttribute().getExamScore() != null
					? schema.getAttribute().getExamScore() : 1;
			totalScore += point;
			if (Integer.valueOf(1).equals(correct)) {
				score += point;
				correctCount++;
			}

			PracticeDetail detail = new PracticeDetail();
			detail.setQuestionId(template.getId());
			detail.setQuestionType(template.getQuestionType() == null ? null : template.getQuestionType().name());
			detail.setUserAnswer(AnswerJudgeUtil.formatAnswer(schema, item.getAnswer()));
			detail.setIsCorrect(correct);
			detail.setScore(Integer.valueOf(1).equals(correct) ? point : 0);
			details.add(detail);
		}

		// 3. 落库：练习会话 + 逐题明细
		PracticeRecord record = new PracticeRecord();
		record.setUserId(userId);
		record.setMode(request.getMode());
		record.setRepoId(request.getRepoId());
		record.setTotalQuestions(details.size());
		record.setCorrectCount(correctCount);
		record.setScore(Math.round(score * 100) / 100.0);
		record.setTotalScore(Math.round(totalScore * 100) / 100.0);
		record.setDurationMs(request.getDurationMs());
		save(record);

		// 4. 落库逐题明细
		details.forEach(detail -> {
			detail.setPracticeId(record.getId());
			practiceDetailMapper.insert(detail);
		});
		log.info("practice submitted: userId={}, mode={}, total={}, correct={}, score={}/{}",
				userId, request.getMode(), details.size(), correctCount, record.getScore(), record.getTotalScore());

		// 5. 组装判分结果（含标准答案，供学员端即时反馈）
		PracticeResultView result = new PracticeResultView();
		result.setScore(Math.round(score * 100) / 100.0);
		result.setTotalScore(Math.round(totalScore * 100) / 100.0);
		result.setCorrectCount(correctCount);
		result.setTotal(details.size());
		for (PracticeSubmitRequest.PracticeItem item : request.getItems()) {
			Template template = item.getQuestionId() == null ? null : templateMap.get(item.getQuestionId());
			Integer correct = null;
			String correctAnswer = null;
			if (template != null && template.getTemplate() != null) {
				SurveySchema schema = template.getTemplate();
				try {
					correct = AnswerJudgeUtil.evaluate(schema, item.getAnswer());
				}
				catch (Exception ignored) {
					// 未判
				}
				List<String> answers = AnswerJudgeUtil.extractCorrectAnswers(schema);
				if (answers != null) {
					correctAnswer = String.join(" / ", answers);
				}
			}
			result.getItems().add(new PracticeResultView.PracticeResultItem(
					item.getQuestionId(), correct, correctAnswer));
		}
		return result;
	}

	/**
	 * 分页查询错题库（题目 × 学员聚合）。
	 *
	 * @param query 筛选条件（题库/题型/关键词/做错时间范围 + 分页）
	 * @return 聚合错题分页结果；查询条件为空时返回空列表（默认无错题数据）
	 */
	@Override
	public PaginationResponse<WrongQuestionView> listWrongQuestions(WrongQuestionQuery query) {
		if (query == null) {
			query = new WrongQuestionQuery();
		}
		Page<WrongQuestionView> page = new Page<>(query.getCurrent(), query.getPageSize());
		IPage<WrongQuestionView> result = practiceDetailMapper.selectWrongQuestions(page, query);
		return new PaginationResponse<>(result.getTotal(), result.getRecords());
	}
}

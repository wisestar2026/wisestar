package cn.wisestar.server.impl;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.core.constant.StudentRewardConstants;
import cn.wisestar.server.core.uitls.AnswerJudgeUtil;
import cn.wisestar.server.core.uitls.SecurityContextUtils;
import cn.wisestar.server.domain.dto.PracticeResultView;
import cn.wisestar.server.domain.dto.WrongReasonRequest;
import cn.wisestar.server.domain.dto.PracticeSubmitRequest;
import cn.wisestar.server.domain.dto.SurveySchema;
import cn.wisestar.server.domain.dto.WrongQuestionQuery;
import cn.wisestar.server.domain.dto.WrongQuestionView;
import cn.wisestar.server.domain.dto.student.PracticeEvaluationContext;
import cn.wisestar.server.domain.dto.student.RewardContext;
import cn.wisestar.server.domain.model.Chapter;
import cn.wisestar.server.domain.model.KnowledgePoint;
import cn.wisestar.server.domain.model.PracticeDetail;
import cn.wisestar.server.domain.model.PracticeRecord;
import cn.wisestar.server.domain.model.Section;

import javax.validation.ValidationException;
import cn.wisestar.server.domain.model.Student;
import cn.wisestar.server.domain.model.PracticeRecord;
import cn.wisestar.server.domain.model.Template;
import cn.wisestar.server.mapper.ChapterMapper;
import cn.wisestar.server.mapper.KnowledgePointMapper;
import cn.wisestar.server.mapper.PracticeDetailMapper;
import cn.wisestar.server.mapper.SectionMapper;
import cn.wisestar.server.mapper.StudentMapper;
import cn.wisestar.server.mapper.PracticeRecordMapper;
import cn.wisestar.server.service.BaseService;
import cn.wisestar.server.service.EvaluationService;
import cn.wisestar.server.service.PracticeService;
import cn.wisestar.server.service.RewardService;
import cn.wisestar.server.impl.TemplateServiceImpl;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
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
 * - 单项填空/多项填空：有标准答案时逐空判分（attribute.examBlankScores 为空时按
 *   整题分 ÷ 空位数 均摊），按正确空位分值累加计分（部分给分）；其余题型整题判分：
 *   答对得分 = 题分；答错/未作答 = 0 分；无标准答案 = 不计分也不当错题；
 * - is_correct=1 仅表示该题全对（填空中每空均正确）；
 * - total_score = 全部题分值之和；score = 各题得分之和（含填空部分分）；
 *   correct_count = 判对（全对）题数。
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

	private final StudentMapper studentMapper;

	/**
	 * 题目服务（回源题目 schema 用于判分）。
	 */
	private final TemplateServiceImpl templateService;

	/** 学习评价服务（掌握度/薄弱点刷新）。 */
	private final EvaluationService evaluationService;

	/** 积分·学币结算服务。 */
	private final RewardService rewardService;

	/** 知识点 Mapper（学科归属解析）。 */
	private final KnowledgePointMapper knowledgePointMapper;

	/** 小节 Mapper（学科归属解析）。 */
	private final SectionMapper sectionMapper;

	/** 章节 Mapper（学科归属解析）。 */
	private final ChapterMapper chapterMapper;

	/**
	 * 构造器注入。
	 *
	 * @param practiceDetailMapper 逐题明细 Mapper
	 * @param templateService      题目服务
	 * @param studentMapper        学员 Mapper
	 * @param evaluationService    学习评价服务
	 * @param rewardService        结算服务
	 * @param knowledgePointMapper 知识点 Mapper
	 * @param sectionMapper        小节 Mapper
	 * @param chapterMapper        章节 Mapper
	 */
	public PracticeServiceImpl(PracticeDetailMapper practiceDetailMapper, TemplateServiceImpl templateService,
			StudentMapper studentMapper, EvaluationService evaluationService, RewardService rewardService,
			KnowledgePointMapper knowledgePointMapper, SectionMapper sectionMapper, ChapterMapper chapterMapper) {
		this.practiceDetailMapper = practiceDetailMapper;
		this.templateService = templateService;
		this.studentMapper = studentMapper;
		this.evaluationService = evaluationService;
		this.rewardService = rewardService;
		this.knowledgePointMapper = knowledgePointMapper;
		this.sectionMapper = sectionMapper;
		this.chapterMapper = chapterMapper;
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
			// 每题分值：attribute.examScore，无则 1 分
			double point = schema.getAttribute() != null && schema.getAttribute().getExamScore() != null
					? schema.getAttribute().getExamScore() : 1;
			totalScore += point;

			// 计分：填空类题（有标准答案时）按逐空判分累加每空分值，实现部分给分；
			// 其余题型维持整题判分（答对得整题分，否则 0 分）
			double earned = 0;
			Integer correct;
			try {
				int[] blankResult = AnswerJudgeUtil.evaluateBlanks(schema, item.getAnswer());
				if (blankResult.length > 1) {
					int totalBlanks = blankResult[0];
					List<Double> blankScores = schema.getAttribute() == null ? null
							: schema.getAttribute().getExamBlankScores();
					boolean perBlankConfigured = blankScores != null && blankScores.size() == totalBlanks;
					boolean allCorrect = true;
					for (int i = 1; i < blankResult.length; i++) {
						if (blankResult[i] != 1) {
							allCorrect = false;
							continue;
						}
						double blankPoint = perBlankConfigured
								? (blankScores.get(i - 1) == null ? 0 : blankScores.get(i - 1))
								: Math.round(point / totalBlanks * 100) / 100.0;
						earned += blankPoint;
					}
					// 兜底防御：得分不超过该题整题分（避免均摊/配置导致的小数溢出）
					earned = Math.min(earned, point);
					correct = allCorrect ? 1 : 0;
				} else {
					correct = AnswerJudgeUtil.evaluate(schema, item.getAnswer());
					if (Integer.valueOf(1).equals(correct)) {
						earned = point;
					}
				}
			} catch (Exception e) {
				log.warn("practice submit: judge question {} failed, skipped", item.getQuestionId(), e);
				continue;
			}
			score += earned;
			if (Integer.valueOf(1).equals(correct)) {
				correctCount++;
			}

			PracticeDetail detail = new PracticeDetail();
			detail.setQuestionId(template.getId());
			detail.setQuestionType(template.getQuestionType() == null ? null : template.getQuestionType().name());
			detail.setUserAnswer(AnswerJudgeUtil.formatAnswer(schema, item.getAnswer()));
			detail.setIsCorrect(correct);
			detail.setScore(Math.round(earned * 100) / 100.0);
			details.add(detail);
		}

		// 3. 落库：练习会话 + 逐题明细
		PracticeRecord record = new PracticeRecord();
		record.setUserId(userId);
		record.setMode(request.getMode());
		record.setRepoId(request.getRepoId());
		record.setKnowledgePointId(request.getKnowledgePointId());
		record.setSectionId(request.getSectionId());
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

		// 4.1 学习评价刷新（掌握度/薄弱点；异常不阻断交卷）
		try {
			PracticeEvaluationContext evalCtx = new PracticeEvaluationContext();
			evalCtx.setUserId(userId);
			evalCtx.setPracticeId(record.getId());
			evalCtx.setKnowledgePointId(record.getKnowledgePointId());
			evalCtx.setSectionId(record.getSectionId());
			for (PracticeDetail detail : details) {
				PracticeEvaluationContext.Item item = new PracticeEvaluationContext.Item();
				item.setQuestionId(detail.getQuestionId());
				item.setCorrect(Integer.valueOf(1).equals(detail.getIsCorrect()));
				Template template = templateMap.get(detail.getQuestionId());
				if (template != null && template.getKnowledgePoint() != null) {
					item.setKnowledgePointNames(java.util.Arrays.asList(template.getKnowledgePoint()));
				}
				evalCtx.getItems().add(item);
			}
			evaluationService.recordPractice(evalCtx);
		}
		catch (Exception e) {
			log.warn("practice submit: evaluation refresh failed, ignored", e);
		}

		// 4.2 积分·学币结算（练习/试炼；异常不阻断交卷）
		int rate = details.isEmpty() ? 0 : (int) Math.round(correctCount * 100.0 / details.size());
		boolean trial = "trial".equalsIgnoreCase(request.getMode());
		RewardContext rc = new RewardContext();
		rc.setUserId(userId);
		rc.setActionType(trial ? StudentRewardConstants.ACTION_TRIAL : StudentRewardConstants.ACTION_PRACTICE);
		rc.setSubjectId(resolveSubjectId(record.getKnowledgePointId(), record.getSectionId()));
		rc.setKnowledgePointId(record.getKnowledgePointId());
		rc.setSectionId(record.getSectionId());
		rc.setRefId(record.getId());
		rc.setDurationMs(record.getDurationMs());
		rc.setCorrectRate(rate);
		cn.wisestar.server.domain.dto.student.StudentPreviewCompleteView reward = null;
		try {
			reward = rewardService.settle(rc);
			// 试炼优秀额外奖励（正确率≥90%），独立幂等键
			if (trial && rate >= StudentRewardConstants.TRIAL_BONUS_RATE) {
				RewardContext bonusCtx = new RewardContext();
				bonusCtx.setUserId(userId);
				bonusCtx.setActionType(StudentRewardConstants.ACTION_TRIAL_BONUS);
				bonusCtx.setSubjectId(rc.getSubjectId());
				bonusCtx.setKnowledgePointId(record.getKnowledgePointId());
				bonusCtx.setSectionId(record.getSectionId());
				bonusCtx.setRefId(record.getId() + ":bonus");
				cn.wisestar.server.domain.dto.student.StudentPreviewCompleteView bonus = rewardService.settle(bonusCtx);
				if (bonus != null && bonus.isOk()) {
					reward.setCoins(reward.getCoins() + bonus.getCoins());
					reward.setPoints(reward.getPoints() + bonus.getPoints());
				}
			}
		}
		catch (Exception e) {
			log.warn("practice submit: reward settle failed, ignored", e);
		}

		// 5. 组装判分结果（含标准答案，供学员端即时反馈）
		PracticeResultView result = new PracticeResultView();
		result.setScore(Math.round(score * 100) / 100.0);
		result.setTotalScore(Math.round(totalScore * 100) / 100.0);
		result.setCorrectCount(correctCount);
		result.setTotal(details.size());
		if (reward != null) {
			result.setCoins(reward.getCoins());
			result.setPoints(reward.getPoints());
			result.setTitleUpgraded(reward.isTitleUpgraded());
			result.setTitleName(reward.getTitleName());
			result.setCoinsCapped(reward.isCoinsCapped());
			result.setMessage(reward.getMessage());
		}
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
			String detailId = details.stream()
					.filter(d -> item.getQuestionId() != null && item.getQuestionId().equals(d.getQuestionId()))
					.findFirst().map(PracticeDetail::getId).orElse(null);
			result.getItems().add(new PracticeResultView.PracticeResultItem(
					item.getQuestionId(), correct, correctAnswer, detailId));
		}
		return result;
	}

	/**
	 * 由知识点/小节反查所属学科ID（奖励上限归属学科）。
	 */
	private String resolveSubjectId(String knowledgePointId, String sectionId) {
		String effectiveSectionId = sectionId;
		if (StringUtils.hasText(knowledgePointId)) {
			KnowledgePoint kp = knowledgePointMapper.selectById(knowledgePointId);
			if (kp != null && StringUtils.hasText(kp.getSectionId())) {
				effectiveSectionId = kp.getSectionId();
			}
		}
		if (!StringUtils.hasText(effectiveSectionId)) {
			return null;
		}
		Section section = sectionMapper.selectById(effectiveSectionId);
		if (section == null || !StringUtils.hasText(section.getChapterId())) {
			return null;
		}
		Chapter chapter = chapterMapper.selectById(section.getChapterId());
		return chapter == null ? null : chapter.getSubjectId();
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
		// 学员身份：强制只看自己的错题（管理端不限）
		String userId = SecurityContextUtils.getUserId();
		if (userId != null && studentMapper.selectById(userId) != null && !StringUtils.hasText(query.getUserId())) {
			query.setUserId(userId);
		}
		Page<WrongQuestionView> page = new Page<>(query.getCurrent(), query.getPageSize());
		IPage<WrongQuestionView> result = practiceDetailMapper.selectWrongQuestions(page, query);
		return new PaginationResponse<>(result.getTotal(), result.getRecords());
	}

	/**
	 * 保存错题错误归因（校验明细属于当前学员）。
	 */
	@Override
	public void saveWrongReason(WrongReasonRequest request) {
		if (request.getDetailId() == null || !StringUtils.hasText(request.getReason())) {
			throw new ValidationException("明细ID与归因不能为空");
		}
		PracticeDetail detail = practiceDetailMapper.selectById(request.getDetailId());
		if (detail == null) {
			throw new ValidationException("错题明细不存在");
		}
		String userId = SecurityContextUtils.getUserId();
		PracticeRecord record = this.baseMapper.selectById(detail.getPracticeId());
		if (record == null || !userId.equals(record.getUserId())) {
			throw new ValidationException("无权修改该错题归因");
		}
		detail.setWrongReason(request.getReason());
		practiceDetailMapper.updateById(detail);
	}
}

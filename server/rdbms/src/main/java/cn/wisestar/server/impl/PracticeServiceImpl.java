package cn.wisestar.server.impl;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.core.uitls.AnswerJudgeUtil;
import cn.wisestar.server.core.uitls.SecurityContextUtils;
import cn.wisestar.server.domain.dto.PracticeMasteryView;
import cn.wisestar.server.domain.dto.PracticeResultView;
import cn.wisestar.server.domain.dto.WrongReasonBatchRequest;
import cn.wisestar.server.domain.dto.WrongReasonRequest;
import cn.wisestar.server.domain.dto.PracticeSubmitRequest;
import cn.wisestar.server.domain.dto.SurveySchema;
import cn.wisestar.server.domain.dto.WrongQuestionQuery;
import cn.wisestar.server.domain.dto.WrongQuestionView;
import cn.wisestar.server.domain.model.PracticeDetail;
import cn.wisestar.server.domain.model.PracticeRecord;

import javax.validation.ValidationException;
import cn.wisestar.server.domain.model.Student;
import cn.wisestar.server.domain.model.Template;
import cn.wisestar.server.domain.model.KnowledgePoint;
import cn.wisestar.server.domain.model.KnowledgePointQuestion;
import cn.wisestar.server.domain.model.Section;
import cn.wisestar.server.mapper.PracticeDetailMapper;
import cn.wisestar.server.mapper.StudentMapper;
import cn.wisestar.server.mapper.PracticeRecordMapper;
import cn.wisestar.server.mapper.KnowledgePointMapper;
import cn.wisestar.server.mapper.KnowledgePointQuestionMapper;
import cn.wisestar.server.mapper.SectionMapper;
import cn.wisestar.server.service.BaseService;
import cn.wisestar.server.service.PracticeService;
import cn.wisestar.server.impl.TemplateServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

	/**
	 * 知识点 Mapper（掌握度/逐题知识点归属回填）。
	 */
	private final KnowledgePointMapper knowledgePointMapper;

	/**
	 * 知识点-题目绑定 Mapper（题目归属知识点反查）。
	 */
	private final KnowledgePointQuestionMapper knowledgePointQuestionMapper;

	/**
	 * 小节 Mapper（掌握度范围锚定/名称回填）。
	 */
	private final SectionMapper sectionMapper;

	/**
	 * 构造器注入。
	 *
	 * @param practiceDetailMapper 逐题明细 Mapper
	 * @param templateService      题目服务
	 */
	public PracticeServiceImpl(PracticeDetailMapper practiceDetailMapper, TemplateServiceImpl templateService,
			StudentMapper studentMapper, KnowledgePointMapper knowledgePointMapper,
			KnowledgePointQuestionMapper knowledgePointQuestionMapper, SectionMapper sectionMapper) {
		this.practiceDetailMapper = practiceDetailMapper;
		this.templateService = templateService;
		this.studentMapper = studentMapper;
		this.knowledgePointMapper = knowledgePointMapper;
		this.knowledgePointQuestionMapper = knowledgePointQuestionMapper;
		this.sectionMapper = sectionMapper;
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

		// 5. 组装判分结果（含标准答案，供学员端交卷反馈；题目归属知识点供掌握度/错因归纳展示）
		Map<String, KnowledgePoint> kpByQuestion = loadKpMap(questionIds, request.getKnowledgePointId(),
				request.getSectionId());
		PracticeResultView result = new PracticeResultView();
		result.setRecordId(record.getId());
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
			String detailId = details.stream()
					.filter(d -> item.getQuestionId() != null && item.getQuestionId().equals(d.getQuestionId()))
					.findFirst().map(PracticeDetail::getId).orElse(null);
			PracticeResultView.PracticeResultItem resultItem = new PracticeResultView.PracticeResultItem(
					item.getQuestionId(), correct, correctAnswer, detailId);
			KnowledgePoint kp = item.getQuestionId() == null ? null : kpByQuestion.get(item.getQuestionId());
			if (kp != null) {
				resultItem.setKnowledgePointId(kp.getId());
				resultItem.setKnowledgePointName(kp.getName());
			}
			result.getItems().add(resultItem);
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
		applyWrongReason(request);
	}

	/**
	 * 批量保存错题错误归因（交卷后强制逐题归因：任一条目非法即整批失败）。
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void saveWrongReasons(WrongReasonBatchRequest request) {
		if (request == null || CollectionUtils.isEmpty(request.getItems())) {
			throw new ValidationException("归因条目不能为空");
		}
		for (WrongReasonRequest item : request.getItems()) {
			applyWrongReason(item);
		}
	}

	/** 单条归因落库（明细存在 + 归属当前学员校验） */
	private void applyWrongReason(WrongReasonRequest request) {
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

	/**
	 * 学员练习掌握度汇总（小节/知识点范围二选一，基于该范围全部练习历史）。
	 */
	@Override
	public PracticeMasteryView mastery(String sectionId, String knowledgePointId) {
		boolean sectionScope = StringUtils.hasText(sectionId);
		boolean kpScope = StringUtils.hasText(knowledgePointId);
		if (sectionScope == kpScope) {
			throw new ValidationException("小节与知识点范围必须二选一");
		}
		String userId = SecurityContextUtils.getUserId();
		PracticeMasteryView view = new PracticeMasteryView();
		// 范围锚定：知识点白名单 + 记录过滤列
		Set<String> allowedKpIds = new HashSet<>();
		LambdaQueryWrapper<PracticeRecord> recordQuery = Wrappers.<PracticeRecord>lambdaQuery()
				.eq(PracticeRecord::getUserId, userId);
		if (kpScope) {
			KnowledgePoint point = knowledgePointMapper.selectById(knowledgePointId);
			if (point == null) {
				throw new ValidationException("知识点不存在");
			}
			view.setScopeType("knowledgePoint");
			view.setScopeId(knowledgePointId);
			view.setScopeName(point.getName());
			allowedKpIds.add(knowledgePointId);
			recordQuery.eq(PracticeRecord::getKnowledgePointId, knowledgePointId);
		} else {
			Section section = sectionMapper.selectById(sectionId);
			if (section == null) {
				throw new ValidationException("小节不存在");
			}
			view.setScopeType("section");
			view.setScopeId(sectionId);
			view.setScopeName(section.getName());
			List<String> kpIds = knowledgePointMapper.selectList(Wrappers.<KnowledgePoint>lambdaQuery()
							.eq(KnowledgePoint::getSectionId, sectionId))
					.stream().map(KnowledgePoint::getId).collect(Collectors.toList());
			allowedKpIds.addAll(kpIds);
			recordQuery.eq(PracticeRecord::getSectionId, sectionId);
		}
		recordQuery.orderByDesc(PracticeRecord::getCreateAt);
		List<PracticeRecord> records = this.baseMapper.selectList(recordQuery);
		view.setPracticeCount((long) records.size());
		if (records.isEmpty()) {
			view.setQuestionCount(0L);
			view.setRightCount(0L);
			view.setAccuracy(0);
			return view;
		}
		// 上次练习结果（最近一条锚定记录）
		fillLastRecord(view, records.get(0));
		// 明细级统计：判分题次（is_correct ∈ {0,1}）→ 按题目归属知识点聚合
		List<String> recordIds = records.stream().map(PracticeRecord::getId).collect(Collectors.toList());
		List<PracticeDetail> details = practiceDetailMapper.selectList(Wrappers.<PracticeDetail>lambdaQuery()
				.in(PracticeDetail::getPracticeId, recordIds)
				.in(PracticeDetail::getIsCorrect, 0, 1));
		if (details.isEmpty()) {
			view.setQuestionCount(0L);
			view.setRightCount(0L);
			view.setAccuracy(0);
			return view;
		}
		Set<String> questionIds = details.stream().map(PracticeDetail::getQuestionId)
				.filter(StringUtils::hasText).collect(Collectors.toSet());
		Map<String, KnowledgePoint> kpByQuestion = new HashMap<>();
		if (!questionIds.isEmpty()) {
			List<KnowledgePointQuestion> bindings = knowledgePointQuestionMapper.selectList(
					Wrappers.<KnowledgePointQuestion>lambdaQuery().in(KnowledgePointQuestion::getQuestionId, questionIds));
			Set<String> needKpIds = bindings.stream().map(KnowledgePointQuestion::getKnowledgePointId)
					.filter(allowedKpIds::contains).collect(Collectors.toSet());
			if (!needKpIds.isEmpty()) {
				Map<String, KnowledgePoint> kpMap = knowledgePointMapper.selectBatchIds(needKpIds).stream()
						.collect(Collectors.toMap(KnowledgePoint::getId, Function.identity(), (a, b) -> a));
				for (KnowledgePointQuestion binding : bindings) {
					if (!allowedKpIds.contains(binding.getKnowledgePointId())) {
						continue;
					}
					KnowledgePoint kp = kpMap.get(binding.getKnowledgePointId());
					if (kp != null && !kpByQuestion.containsKey(binding.getQuestionId())) {
						kpByQuestion.put(binding.getQuestionId(), kp);
					}
				}
			}
		}
		// 记录级汇总 + 知识点维度汇总（空绑定题只计入记录级）
		Map<String, Date> recordTimeById = records.stream().filter(r -> r.getCreateAt() != null)
				.collect(Collectors.toMap(PracticeRecord::getId, PracticeRecord::getCreateAt, (a, b) -> a));
		Map<String, PracticeMasteryView.KpMastery> kpAgg = new HashMap<>();
		long totalAttempts = 0;
		long totalRight = 0;
		for (PracticeDetail detail : details) {
			if (detail.getIsCorrect() == null) {
				continue;
			}
			totalAttempts++;
			if (detail.getIsCorrect() == 1) {
				totalRight++;
			}
			KnowledgePoint kp = detail.getQuestionId() == null ? null : kpByQuestion.get(detail.getQuestionId());
			if (kp == null) {
				continue;
			}
			PracticeMasteryView.KpMastery mastery = kpAgg.computeIfAbsent(kp.getId(),
					k -> {
						PracticeMasteryView.KpMastery m = new PracticeMasteryView.KpMastery();
						m.setKnowledgePointId(kp.getId());
						m.setKnowledgePointName(kp.getName());
						m.setAttempts(0L);
						m.setRight(0L);
						m.setAccuracy(0);
						return m;
					});
			mastery.setAttempts(mastery.getAttempts() + 1);
			if (detail.getIsCorrect() == 1) {
				mastery.setRight(mastery.getRight() + 1);
			}
			// 最近练习时间 = 所在记录创建时间（明细行无时间列）
			Date ownerTime = detail.getPracticeId() == null ? null : recordTimeById.get(detail.getPracticeId());
			if (ownerTime != null && (mastery.getLastPracticedAt() == null || ownerTime.after(mastery.getLastPracticedAt()))) {
				mastery.setLastPracticedAt(ownerTime);
			}
		}
		view.setQuestionCount(totalAttempts);
		view.setRightCount(totalRight);
		view.setAccuracy(totalAttempts == 0 ? 0 : (int) Math.round(totalRight * 100.0 / totalAttempts));
		view.setKps(kpAgg.values().stream()
				.peek(m -> m.setAccuracy(m.getAttempts() == 0 ? 0 : (int) Math.round(m.getRight() * 100.0 / m.getAttempts())))
				.sorted(Comparator.comparing(PracticeMasteryView.KpMastery::getAttempts).reversed())
				.collect(Collectors.toList()));
		return view;
	}

	/** 上次练习结果摘要回填（最近一条记录） */
	private void fillLastRecord(PracticeMasteryView view, PracticeRecord record) {
		view.setLastRecord(toLastRecord(record));
	}

	/** 练习记录 → 结果摘要视图（掌握度「上次结果」与历史列表共用） */
	private PracticeMasteryView.LastRecord toLastRecord(PracticeRecord record) {
		PracticeMasteryView.LastRecord last = new PracticeMasteryView.LastRecord();
		last.setRecordId(record.getId());
		last.setMode(record.getMode());
		last.setScore(record.getScore());
		last.setTotalScore(record.getTotalScore());
		last.setTotalQuestions(record.getTotalQuestions());
		last.setCorrectCount(record.getCorrectCount());
		last.setDurationMs(record.getDurationMs());
		last.setCreateAt(record.getCreateAt());
		if (record.getTotalScore() != null && record.getTotalScore() > 0) {
			last.setRate((int) Math.round((record.getScore() == null ? 0 : record.getScore())
					* 100.0 / record.getTotalScore()));
		} else if (record.getTotalQuestions() != null && record.getTotalQuestions() > 0) {
			int c = record.getCorrectCount() == null ? 0 : record.getCorrectCount();
			last.setRate((int) Math.round(c * 100.0 / record.getTotalQuestions()));
		}
		return last;
	}

	/**
	 * 范围内近期练习记录（掌握变化对比用，最多 20 条，倒序）。
	 */
	@Override
	public List<PracticeMasteryView.LastRecord> history(String sectionId, String knowledgePointId) {
		boolean sectionScope = StringUtils.hasText(sectionId);
		boolean kpScope = StringUtils.hasText(knowledgePointId);
		if (sectionScope == kpScope) {
			throw new ValidationException("小节与知识点范围必须二选一");
		}
		String userId = SecurityContextUtils.getUserId();
		LambdaQueryWrapper<PracticeRecord> recordQuery = Wrappers.<PracticeRecord>lambdaQuery()
				.eq(PracticeRecord::getUserId, userId);
		if (kpScope) {
			recordQuery.eq(PracticeRecord::getKnowledgePointId, knowledgePointId);
		} else {
			recordQuery.eq(PracticeRecord::getSectionId, sectionId);
		}
		recordQuery.orderByDesc(PracticeRecord::getCreateAt).last("LIMIT 20");
		return this.baseMapper.selectList(recordQuery).stream().map(this::toLastRecord)
				.collect(Collectors.toList());
	}

	/**
	 * 题目 → 知识点归属映射（提交结果/掌握度按知识点归纳展示用）。
	 * 归属范围优先取数据源上下文（知识点练习=自身；小节练习=该小节知识点），
	 * 无上下文时取题目任意未删绑定；未绑定返回空 Map。
	 */
	private Map<String, KnowledgePoint> loadKpMap(List<String> questionIds, String knowledgePointId, String sectionId) {
		Map<String, KnowledgePoint> result = new HashMap<>();
		if (CollectionUtils.isEmpty(questionIds)) {
			return result;
		}
		Set<String> allowedKpIds = null;
		if (StringUtils.hasText(knowledgePointId)) {
			allowedKpIds = new HashSet<>(Collections.singletonList(knowledgePointId));
		} else if (StringUtils.hasText(sectionId)) {
			allowedKpIds = knowledgePointMapper.selectList(Wrappers.<KnowledgePoint>lambdaQuery()
							.eq(KnowledgePoint::getSectionId, sectionId))
					.stream().map(KnowledgePoint::getId).collect(Collectors.toSet());
			if (allowedKpIds.isEmpty()) {
				return result;
			}
		}
		List<KnowledgePointQuestion> bindings = knowledgePointQuestionMapper.selectList(
				Wrappers.<KnowledgePointQuestion>lambdaQuery().in(KnowledgePointQuestion::getQuestionId, questionIds));
		final Set<String> kpWhitelist = allowedKpIds;
		Set<String> needKpIds = bindings.stream().map(KnowledgePointQuestion::getKnowledgePointId)
				.filter(kpId -> kpWhitelist == null || kpWhitelist.contains(kpId))
				.collect(Collectors.toSet());
		if (needKpIds.isEmpty()) {
			return result;
		}
		Map<String, KnowledgePoint> kpMap = knowledgePointMapper.selectBatchIds(needKpIds).stream()
				.collect(Collectors.toMap(KnowledgePoint::getId, Function.identity(), (a, b) -> a));
		for (KnowledgePointQuestion binding : bindings) {
			if (allowedKpIds != null && !allowedKpIds.contains(binding.getKnowledgePointId())) {
				continue;
			}
			KnowledgePoint kp = kpMap.get(binding.getKnowledgePointId());
			if (kp != null && !result.containsKey(binding.getQuestionId())) {
				result.put(binding.getQuestionId(), kp);
			}
		}
		return result;
	}
}

package cn.wisestar.server.impl;

import cn.wisestar.server.core.constant.StudentRewardConstants;
import cn.wisestar.server.domain.dto.student.RewardContext;
import cn.wisestar.server.domain.dto.student.StudentPreviewCompleteView;
import cn.wisestar.server.domain.model.PracticeRecord;
import cn.wisestar.server.domain.model.SubjectSemester;
import cn.wisestar.server.domain.model.UserLearningRecord;
import cn.wisestar.server.domain.model.UserPoints;
import cn.wisestar.server.mapper.PracticeRecordMapper;
import cn.wisestar.server.mapper.SubjectSemesterMapper;
import cn.wisestar.server.mapper.UserLearningRecordMapper;
import cn.wisestar.server.mapper.UserPointsMapper;
import cn.wisestar.server.service.RewardService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * 学员积分·学币统一结算实现。
 *
 * <p>【核心逻辑】校验行为 → 学期幂等判定（ref_id 统一加学期前缀）→ 单科 10000 上限裁剪 →
 * 写行为记录 → 累加学海积分并重算头衔 → 检查每日里程碑。奖励金额只由行为类型决定，与内容配置无关。</p>
 *
 * @author wisestar
 * @date 2026/9/10
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class RewardServiceImpl implements RewardService {

	private final UserPointsMapper userPointsMapper;

	private final SubjectSemesterMapper subjectSemesterMapper;

	private final UserLearningRecordMapper learningRecordMapper;

	private final PracticeRecordMapper practiceRecordMapper;

	@Override
	public StudentPreviewCompleteView settle(RewardContext context) {
		StudentPreviewCompleteView view = new StudentPreviewCompleteView();
		if (context == null || !StringUtils.hasText(context.getUserId())
				|| !StringUtils.hasText(context.getActionType())) {
			view.setOk(false);
			view.setMessage("缺少结算参数");
			return view;
		}
		String action = context.getActionType();
		int[] reward = StudentRewardConstants.reward(action, context.getStage());
		if (reward == null) {
			view.setOk(false);
			view.setMessage("未知学习行为：" + action);
			return view;
		}
		String semester = StudentRewardConstants.currentSemester();
		// 学期幂等键：ref_id 统一加学期前缀，使内容型奖励「每学期每内容一次」，跨学期自动可再发
		String effectiveRef = null;
		if (StringUtils.hasText(context.getRefId())) {
			effectiveRef = semester + ":" + context.getRefId();
			if (effectiveRef.length() > 64) {
				throw new IllegalArgumentException("奖励幂等键超长：" + effectiveRef);
			}
		}

		// 幂等：同一学期同一业务关联ID不重复发放
		if (StringUtils.hasText(effectiveRef) && existsRef(context.getUserId(), action, effectiveRef)) {
			return blocked(view, context.getUserId(), "该奖励本学期已发放");
		}

		int baseCoins = reward[0];
		int basePoints = reward[1];

		// 单科 10000 上限裁剪（只影响学习币，积分照发）
		int actualCoins = baseCoins;
		boolean coinsCapped = false;
		if (StringUtils.hasText(context.getSubjectId()) && baseCoins > 0) {
			SubjectSemester ss = getOrCreateSemester(context.getUserId(), context.getSubjectId(), semester);
			int current = ss.getCoins() == null ? 0 : ss.getCoins();
			int remain = StudentRewardConstants.SUBJECT_COIN_LIMIT - current;
			if (remain <= 0) {
				actualCoins = 0;
				coinsCapped = true;
			}
			else if (baseCoins > remain) {
				actualCoins = remain;
				coinsCapped = true;
			}
			int newCoins = Math.min(StudentRewardConstants.SUBJECT_COIN_LIMIT, current + actualCoins);
			ss.setCoins(newCoins);
			if (newCoins >= StudentRewardConstants.SUBJECT_COIN_LIMIT) {
				ss.setReachedLimit(true);
			}
			subjectSemesterMapper.updateById(ss);
		}

		// 累加学海积分 + 头衔晋升
		UserPoints up = getOrCreatePoints(context.getUserId());
		int oldLevel = up.getTitleLevel() == null ? 1 : up.getTitleLevel();
		int oldPoints = up.getPoints() == null ? 0 : up.getPoints();
		int newPoints = oldPoints + basePoints;
		int newLevel = Math.max(oldLevel, StudentRewardConstants.titleLevel(newPoints));
		up.setPoints(newPoints);
		up.setTitleLevel(newLevel);
		up.setTitleName(StudentRewardConstants.titleName(newLevel));
		userPointsMapper.updateById(up);

		// 写学习行为记录
		UserLearningRecord record = new UserLearningRecord();
		record.setUserId(context.getUserId());
		record.setSubjectId(context.getSubjectId());
		record.setKnowledgePointId(context.getKnowledgePointId());
		record.setSectionId(context.getSectionId());
		record.setActionType(action);
		record.setRefId(effectiveRef);
		record.setCoins(actualCoins);
		record.setPoints(basePoints);
		record.setSemester(semester);
		record.setLearnedAt(new Date());
		try {
			learningRecordMapper.insert(record);
		}
		catch (DuplicateKeyException e) {
			// 并发穿透：唯一索引兜底，回滚本次余额与积分变更，按「已发放」返回
			TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
			log.warn("reward settle duplicate: user={}, action={}, ref={}", context.getUserId(), action, effectiveRef);
			return blocked(view, context.getUserId(), "该奖励本学期已发放");
		}

		view.setOk(true);
		view.setFirstTime(true);
		view.setCoins(actualCoins);
		view.setPoints(basePoints);
		view.setCoinsCapped(coinsCapped);
		view.setTotalPoints(newPoints);
		view.setTitleLevel(newLevel);
		view.setTitleName(up.getTitleName());
		view.setTitleUpgraded(newLevel > oldLevel);
		if (coinsCapped) {
			view.setMessage("本学科本学期学习币已达上限，可继续学习积累学海积分");
		}

		// 每日里程碑（每日行为本身不再触发，避免递归）
		if (!action.startsWith("daily_")) {
			checkDailyMilestones(context.getUserId(), context.getSubjectId());
		}
		return view;
	}

	@Override
	public int currentPoints(String userId) {
		if (!StringUtils.hasText(userId)) {
			return 0;
		}
		UserPoints up = userPointsMapper.selectOne(
				Wrappers.<UserPoints>lambdaQuery().eq(UserPoints::getUserId, userId).last("limit 1"));
		return up == null || up.getPoints() == null ? 0 : up.getPoints();
	}

	// ---------------- 内部方法 ----------------

	private StudentPreviewCompleteView blocked(StudentPreviewCompleteView view, String userId, String message) {
		view.setOk(true);
		view.setFirstTime(false);
		view.setCoins(0);
		view.setPoints(0);
		UserPoints up = userPointsMapper.selectOne(
				Wrappers.<UserPoints>lambdaQuery().eq(UserPoints::getUserId, userId).last("limit 1"));
		int level = up == null || up.getTitleLevel() == null ? 1 : up.getTitleLevel();
		view.setTotalPoints(up == null || up.getPoints() == null ? 0 : up.getPoints());
		view.setTitleLevel(level);
		view.setTitleName(up == null || up.getTitleName() == null
				? StudentRewardConstants.titleName(level) : up.getTitleName());
		view.setMessage(message);
		return view;
	}

	private boolean existsRef(String userId, String action, String refId) {
		Long count = learningRecordMapper.selectCount(Wrappers.<UserLearningRecord>lambdaQuery()
				.eq(UserLearningRecord::getUserId, userId).eq(UserLearningRecord::getActionType, action)
				.eq(UserLearningRecord::getRefId, refId));
		return count != null && count > 0;
	}

	private SubjectSemester getOrCreateSemester(String userId, String subjectId, String semester) {
		SubjectSemester ss = subjectSemesterMapper.selectOne(Wrappers.<SubjectSemester>lambdaQuery()
				.eq(SubjectSemester::getUserId, userId).eq(SubjectSemester::getSubjectId, subjectId)
				.eq(SubjectSemester::getSemester, semester).last("limit 1"));
		if (ss == null) {
			ss = new SubjectSemester();
			ss.setUserId(userId);
			ss.setSubjectId(subjectId);
			ss.setSemester(semester);
			ss.setCoins(0);
			ss.setReachedLimit(false);
			subjectSemesterMapper.insert(ss);
		}
		return ss;
	}

	private UserPoints getOrCreatePoints(String userId) {
		UserPoints up = userPointsMapper.selectOne(
				Wrappers.<UserPoints>lambdaQuery().eq(UserPoints::getUserId, userId).last("limit 1"));
		if (up == null) {
			up = new UserPoints();
			up.setUserId(userId);
			up.setPoints(0);
			up.setTitleLevel(1);
			up.setTitleName(StudentRewardConstants.titleName(1));
			userPointsMapper.insert(up);
		}
		return up;
	}

	/** 每日里程碑：完成 2 个知识点练习 / 订正错题 ≥3 道 / 有效学习 30 分钟。 */
	private void checkDailyMilestones(String userId, String subjectId) {
		String today = LocalDate.now().toString();
		Date todayStart = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
		List<PracticeRecord> records = practiceRecordMapper
				.selectList(Wrappers.<PracticeRecord>lambdaQuery().eq(PracticeRecord::getUserId, userId)
						.ge(PracticeRecord::getCreateAt, todayStart));
		long distinctKp = records.stream()
				.map(r -> StringUtils.hasText(r.getKnowledgePointId()) ? r.getKnowledgePointId() : r.getSectionId())
				.filter(StringUtils::hasText).distinct().count();
		long totalMs = records.stream().mapToLong(r -> r.getDurationMs() == null ? 0 : r.getDurationMs()).sum();
		Long wrongCorrect = learningRecordMapper.selectCount(Wrappers.<UserLearningRecord>lambdaQuery()
				.eq(UserLearningRecord::getUserId, userId)
				.eq(UserLearningRecord::getActionType, StudentRewardConstants.ACTION_WRONG_CORRECT)
				.ge(UserLearningRecord::getLearnedAt, todayStart));

		if (distinctKp >= 2) {
			settleDaily(userId, subjectId, StudentRewardConstants.ACTION_DAILY_KP, today);
		}
		if (wrongCorrect != null && wrongCorrect >= 3) {
			settleDaily(userId, subjectId, StudentRewardConstants.ACTION_DAILY_WRONG, today);
		}
		if (totalMs >= StudentRewardConstants.DAILY_TIME_MILLIS) {
			settleDaily(userId, subjectId, StudentRewardConstants.ACTION_DAILY_TIME, today);
		}
	}

	private void settleDaily(String userId, String subjectId, String action, String dateRef) {
		RewardContext ctx = new RewardContext();
		ctx.setUserId(userId);
		ctx.setSubjectId(subjectId);
		ctx.setActionType(action);
		ctx.setRefId(dateRef);
		try {
			settle(ctx);
		}
		catch (Exception e) {
			log.warn("daily milestone settle failed: user={}, action={}", userId, action, e);
		}
	}

}

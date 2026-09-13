package cn.wisestar.server.impl;

import cn.wisestar.server.core.constant.StudentRewardConstants;
import cn.wisestar.server.domain.dto.student.RewardContext;
import cn.wisestar.server.domain.dto.student.StudentOnlineChestClaimView;
import cn.wisestar.server.domain.dto.student.StudentOnlineChestView;
import cn.wisestar.server.domain.dto.student.StudentPreviewCompleteView;
import cn.wisestar.server.domain.model.StudySession;
import cn.wisestar.server.domain.model.UserLearningRecord;
import cn.wisestar.server.mapper.StudySessionMapper;
import cn.wisestar.server.mapper.UserLearningRecordMapper;
import cn.wisestar.server.service.OnlineChestService;
import cn.wisestar.server.service.RewardService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.validation.ValidationException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 在线时长宝箱服务实现。
 *
 * <p>在线时长 = 当日 {@code t_study_session.duration_ms} 求和；领取状态 = 当日
 * {@code t_user_learning_record} 中 online_chest_* 行为；领取复用 {@link RewardService#settle}
 * 实现幂等与单科上限裁剪。</p>
 *
 * @author wisestar
 * @date 2026/9/12
 */
@Service
@RequiredArgsConstructor
public class OnlineChestServiceImpl implements OnlineChestService {

	private final StudySessionMapper studySessionMapper;

	private final UserLearningRecordMapper learningRecordMapper;

	private final StudentSubjectResolver subjectResolver;

	private final RewardService rewardService;

	@Override
	public StudentOnlineChestView view(String userId) {
		StudentOnlineChestView view = new StudentOnlineChestView();
		if (!StringUtils.hasText(userId)) {
			fillChests(view, 0L, new HashSet<>());
			return view;
		}
		long minutes = todayOnlineMinutes(userId);
		view.setOnlineMinutes(minutes);
		fillChests(view, minutes, claimedTiers(userId));
		return view;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public StudentOnlineChestClaimView claim(String userId, Integer tierMinutes, String subjectId) {
		String action = tierMinutes == null ? null : StudentRewardConstants.onlineChestAction(tierMinutes);
		if (!StringUtils.hasText(userId) || action == null) {
			throw new ValidationException("未知宝箱档位");
		}
		if (todayOnlineMinutes(userId) < tierMinutes) {
			throw new ValidationException("在线时长未达标");
		}
		String subject = StringUtils.hasText(subjectId) ? subjectId : subjectResolver.firstActiveSubject(userId);
		if (!StringUtils.hasText(subject)) {
			throw new ValidationException("当前学员无有效学科权限");
		}

		String today = LocalDate.now().toString();
		RewardContext context = new RewardContext();
		context.setUserId(userId);
		context.setSubjectId(subject);
		context.setActionType(action);
		context.setRefId(today + ":" + tierMinutes);
		StudentPreviewCompleteView settled = rewardService.settle(context);

		StudentOnlineChestClaimView result = new StudentOnlineChestClaimView();
		result.setOk(settled.isOk());
		result.setFirstTime(settled.isFirstTime());
		result.setCoins(settled.getCoins());
		result.setCoinsCapped(settled.isCoinsCapped());
		if (!settled.isFirstTime()) {
			result.setMessage(StringUtils.hasText(settled.getMessage()) ? settled.getMessage() : "今日该宝箱已领取");
		} else if (settled.isCoinsCapped()) {
			result.setMessage(settled.getMessage());
		} else {
			result.setMessage("宝箱已开启，学习币 +" + settled.getCoins());
		}
		result.setOnlineChest(view(userId));
		return result;
	}

	// ---------------- 内部方法 ----------------

	/** 当日在线分钟（向下取整）。 */
	private long todayOnlineMinutes(String userId) {
		String today = LocalDate.now().toString();
		List<StudySession> sessions = studySessionMapper.selectList(Wrappers.<StudySession>lambdaQuery()
				.eq(StudySession::getStudentId, userId)
				.eq(StudySession::getSessionDate, today));
		long ms = sessions.stream().mapToLong(s -> s.getDurationMs() == null ? 0L : s.getDurationMs()).sum();
		return ms / 60000L;
	}

	/** 当日已领取档位集合。 */
	private Set<Integer> claimedTiers(String userId) {
		Date todayStart = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
		List<UserLearningRecord> records = learningRecordMapper.selectList(Wrappers.<UserLearningRecord>lambdaQuery()
				.eq(UserLearningRecord::getUserId, userId)
				.in(UserLearningRecord::getActionType, StudentRewardConstants.ACTION_ONLINE_CHEST_30,
						StudentRewardConstants.ACTION_ONLINE_CHEST_60,
						StudentRewardConstants.ACTION_ONLINE_CHEST_120)
				.ge(UserLearningRecord::getLearnedAt, todayStart));
		Set<Integer> tiers = new HashSet<>();
		for (UserLearningRecord record : records) {
			Integer tier = tierOfAction(record.getActionType());
			if (tier != null) {
				tiers.add(tier);
			}
		}
		return tiers;
	}

	private Integer tierOfAction(String action) {
		if (StudentRewardConstants.ACTION_ONLINE_CHEST_30.equals(action)) {
			return 30;
		}
		if (StudentRewardConstants.ACTION_ONLINE_CHEST_60.equals(action)) {
			return 60;
		}
		if (StudentRewardConstants.ACTION_ONLINE_CHEST_120.equals(action)) {
			return 120;
		}
		return null;
	}

	/** 按当日分钟与已领集合填充三档状态。 */
	private void fillChests(StudentOnlineChestView view, long minutes, Set<Integer> claimed) {
		for (int tier : StudentRewardConstants.ONLINE_CHEST_TIERS) {
			int[] reward = StudentRewardConstants.reward(StudentRewardConstants.onlineChestAction(tier), null);
			int coins = reward == null ? 0 : reward[0];
			String state;
			if (claimed.contains(tier)) {
				state = "claimed";
			} else if (minutes >= tier) {
				state = "claimable";
			} else {
				state = "locked";
			}
			view.getChests().add(new StudentOnlineChestView.Chest(tier, coins, state));
		}
	}

}

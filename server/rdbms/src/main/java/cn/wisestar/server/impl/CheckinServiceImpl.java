package cn.wisestar.server.impl;

import cn.wisestar.server.core.constant.StudentRewardConstants;
import cn.wisestar.server.domain.dto.student.RewardContext;
import cn.wisestar.server.domain.dto.student.StudentCheckinView;
import cn.wisestar.server.domain.dto.student.StudentPreviewCompleteView;
import cn.wisestar.server.domain.model.UserLearningRecord;
import cn.wisestar.server.mapper.UserLearningRecordMapper;
import cn.wisestar.server.service.CheckinService;
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

/**
 * 学员每日签到实现。
 *
 * <p>签到固定发放学习币，按自然日幂等；无学科语义，计入学员首个有效权限学科的单学期上限。</p>
 *
 * @author wisestar
 * @date 2026/9/12
 */
@Service
@RequiredArgsConstructor
public class CheckinServiceImpl implements CheckinService {

	private final UserLearningRecordMapper learningRecordMapper;

	private final StudentSubjectResolver subjectResolver;

	private final RewardService rewardService;

	@Override
	public StudentCheckinView view(String userId) {
		StudentCheckinView view = new StudentCheckinView();
		boolean checked = isCheckedToday(userId);
		view.setCheckedToday(checked);
		view.setMessage(checked ? "今日已签到" : "签到可得学习币 +" + view.getCoins());
		return view;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public StudentCheckinView checkin(String userId) {
		if (!StringUtils.hasText(userId)) {
			throw new ValidationException("当前用户不是学员");
		}
		StudentCheckinView view = new StudentCheckinView();
		if (isCheckedToday(userId)) {
			view.setCheckedToday(true);
			view.setFirstTime(false);
			view.setCoins(0);
			view.setMessage("今日已签到");
			return view;
		}
		String subject = subjectResolver.firstActiveSubject(userId);
		if (!StringUtils.hasText(subject)) {
			throw new ValidationException("当前学员无有效学科权限");
		}
		RewardContext context = new RewardContext();
		context.setUserId(userId);
		context.setSubjectId(subject);
		context.setActionType(StudentRewardConstants.ACTION_DAILY_CHECKIN);
		context.setRefId(LocalDate.now().toString());
		StudentPreviewCompleteView settled = rewardService.settle(context);

		view.setCheckedToday(true);
		view.setFirstTime(settled.isFirstTime());
		view.setCoins(settled.getCoins());
		view.setCoinsCapped(settled.isCoinsCapped());
		if (settled.isFirstTime() && settled.getCoins() > 0) {
			view.setMessage("签到成功，学习币 +" + settled.getCoins());
		}
		else if (settled.isCoinsCapped()) {
			view.setMessage(settled.getMessage());
		}
		else {
			view.setMessage("今日已签到");
		}
		return view;
	}

	/** 当日是否已签到。 */
	private boolean isCheckedToday(String userId) {
		if (!StringUtils.hasText(userId)) {
			return false;
		}
		Date todayStart = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
		Long count = learningRecordMapper.selectCount(Wrappers.<UserLearningRecord>lambdaQuery()
				.eq(UserLearningRecord::getUserId, userId)
				.eq(UserLearningRecord::getActionType, StudentRewardConstants.ACTION_DAILY_CHECKIN)
				.ge(UserLearningRecord::getLearnedAt, todayStart));
		return count != null && count > 0;
	}

}

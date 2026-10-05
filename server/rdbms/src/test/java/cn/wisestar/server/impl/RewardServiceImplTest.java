package cn.wisestar.server.impl;

import cn.wisestar.server.core.constant.StudentRewardConstants;
import cn.wisestar.server.domain.dto.student.RewardContext;
import cn.wisestar.server.domain.dto.student.StudentPreviewCompleteView;
import cn.wisestar.server.domain.model.UserLearningRecord;
import cn.wisestar.server.domain.model.UserPoints;
import cn.wisestar.server.mapper.PracticeRecordMapper;
import cn.wisestar.server.mapper.SubjectSemesterMapper;
import cn.wisestar.server.mapper.UserLearningRecordMapper;
import cn.wisestar.server.mapper.UserPointsMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 奖励结算幂等单元测试（任务 6.4）。
 *
 * <p>重点验证「攻克薄弱点/薄弱小节」为终身一次：幂等键不附带学期前缀；普通行为仍按学期一次。
 * 并发兜底由 {@code t_user_learning_record} 唯一索引保证，属数据库层，不在纯 Mockito 覆盖范围。</p>
 *
 * @author wisestar
 * @date 2026/10/5
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RewardServiceImplTest {

	@Mock
	private UserPointsMapper userPointsMapper;

	@Mock
	private SubjectSemesterMapper subjectSemesterMapper;

	@Mock
	private UserLearningRecordMapper learningRecordMapper;

	@Mock
	private PracticeRecordMapper practiceRecordMapper;

	private RewardServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new RewardServiceImpl(userPointsMapper, subjectSemesterMapper, learningRecordMapper,
				practiceRecordMapper);
		when(practiceRecordMapper.selectList(any())).thenReturn(Collections.emptyList());
	}

	@Test
	void settle_lifetimeWeakConquer_usesRefWithoutSemesterPrefix() {
		when(learningRecordMapper.selectCount(any())).thenReturn(0L);
		when(userPointsMapper.selectOne(any())).thenReturn(points("u1", 0, 1));

		RewardContext ctx = context("u1", StudentRewardConstants.ACTION_WEAK_CONQUER, "weak:s1:1001:conquered");
		StudentPreviewCompleteView view = service.settle(ctx);

		assertThat(view.isOk()).isTrue();
		assertThat(view.isFirstTime()).isTrue();
		assertThat(view.getCoins()).isEqualTo(25);
		assertThat(view.getPoints()).isEqualTo(12);

		ArgumentCaptor<UserLearningRecord> captor = ArgumentCaptor.forClass(UserLearningRecord.class);
		verify(learningRecordMapper).insert(captor.capture());
		// 终身行为：ref_id 原样，不带「学期:」前缀
		assertThat(captor.getValue().getRefId()).isEqualTo("weak:s1:1001:conquered");
	}

	@Test
	void settle_lifetimeSectionConquer_usesRefWithoutSemesterPrefix() {
		when(learningRecordMapper.selectCount(any())).thenReturn(0L);
		when(userPointsMapper.selectOne(any())).thenReturn(points("u1", 0, 1));

		RewardContext ctx = context("u1", StudentRewardConstants.ACTION_WEAK_SECTION_CONQUER, "weak-section:s1:2001");
		service.settle(ctx);

		ArgumentCaptor<UserLearningRecord> captor = ArgumentCaptor.forClass(UserLearningRecord.class);
		verify(learningRecordMapper).insert(captor.capture());
		assertThat(captor.getValue().getRefId()).isEqualTo("weak-section:s1:2001");
	}

	@Test
	void settle_semesterAction_prefixesRefWithSemester() {
		when(learningRecordMapper.selectCount(any())).thenReturn(0L);
		when(userPointsMapper.selectOne(any())).thenReturn(points("u1", 0, 1));

		RewardContext ctx = context("u1", StudentRewardConstants.ACTION_PRACTICE, "practice:1001");
		service.settle(ctx);

		String semester = StudentRewardConstants.currentSemester();
		ArgumentCaptor<UserLearningRecord> captor = ArgumentCaptor.forClass(UserLearningRecord.class);
		verify(learningRecordMapper).insert(captor.capture());
		assertThat(captor.getValue().getRefId()).isEqualTo(semester + ":practice:1001");
		assertThat(captor.getValue().getSemester()).isEqualTo(semester);
	}

	@Test
	void settle_duplicateLifetimeReward_isBlockedWithoutInsert() {
		when(learningRecordMapper.selectCount(any())).thenReturn(1L);
		when(userPointsMapper.selectOne(any())).thenReturn(points("u1", 500, 2));

		RewardContext ctx = context("u1", StudentRewardConstants.ACTION_WEAK_CONQUER, "weak:s1:1001:conquered");
		StudentPreviewCompleteView view = service.settle(ctx);

		assertThat(view.isOk()).isTrue();
		assertThat(view.isFirstTime()).isFalse();
		assertThat(view.getCoins()).isZero();
		assertThat(view.getMessage()).isEqualTo("该奖励已发放");
		verify(learningRecordMapper, never()).insert(any());
	}

	@Test
	void settle_rejectsUnknownAction() {
		RewardContext ctx = context("u1", "not_an_action", "x");
		StudentPreviewCompleteView view = service.settle(ctx);

		assertThat(view.isOk()).isFalse();
		verify(learningRecordMapper, never()).insert(any());
	}

	private RewardContext context(String userId, String action, String refId) {
		RewardContext ctx = new RewardContext();
		ctx.setUserId(userId);
		ctx.setActionType(action);
		ctx.setRefId(refId);
		return ctx;
	}

	private UserPoints points(String userId, int points, int level) {
		UserPoints up = new UserPoints();
		up.setUserId(userId);
		up.setPoints(points);
		up.setTitleLevel(level);
		up.setTitleName(StudentRewardConstants.titleName(level));
		return up;
	}

}

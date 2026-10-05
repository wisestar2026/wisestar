package cn.wisestar.server.impl;

import cn.wisestar.server.domain.dto.growth.GrowthCompareView;
import cn.wisestar.server.domain.dto.growth.GrowthEventContext;
import cn.wisestar.server.domain.model.KnowledgePoint;
import cn.wisestar.server.domain.model.LearningGrowth;
import cn.wisestar.server.domain.model.StudentArchive;
import cn.wisestar.server.domain.model.Subject;
import cn.wisestar.server.domain.model.UserKnowledgeProgress;
import cn.wisestar.server.domain.model.UserWeakKnowledge;
import cn.wisestar.server.mapper.DetectRecordMapper;
import cn.wisestar.server.mapper.KnowledgePointMapper;
import cn.wisestar.server.mapper.LearningGrowthMapper;
import cn.wisestar.server.mapper.StudentArchiveMapper;
import cn.wisestar.server.mapper.StudentMapper;
import cn.wisestar.server.mapper.SubjectMapper;
import cn.wisestar.server.mapper.UserKnowledgeProgressMapper;
import cn.wisestar.server.mapper.UserWeakKnowledgeMapper;
import cn.wisestar.server.service.StudySummaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 成长档案逻辑单元测试：轨迹写入幂等（任务 2.5）与成长对比计算（任务 8.4）。
 *
 * <p>对应任务清单 2.5 / 8.4 验收项，纯 Mockito 隔离，不依赖 Spring 容器。</p>
 *
 * @author wisestar
 * @date 2026/10/5
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GrowthArchiveServiceImplTest {

	@Mock
	private LearningGrowthMapper learningGrowthMapper;

	@Mock
	private StudentArchiveMapper archiveMapper;

	@Mock
	private DetectRecordMapper detectRecordMapper;

	@Mock
	private StudentMapper studentMapper;

	@Mock
	private SubjectMapper subjectMapper;

	@Mock
	private KnowledgePointMapper knowledgePointMapper;

	@Mock
	private UserWeakKnowledgeMapper weakMapper;

	@Mock
	private UserKnowledgeProgressMapper progressMapper;

	@Mock
	private StudySummaryService studySummaryService;

	private GrowthArchiveServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new GrowthArchiveServiceImpl(learningGrowthMapper, archiveMapper, detectRecordMapper, studentMapper,
				subjectMapper, knowledgePointMapper, weakMapper, progressMapper, studySummaryService);
	}

	// ------------------------------------------------------------------ 2.5 轨迹幂等

	@Test
	void record_ignoresIncompleteContext() {
		GrowthEventContext ctx = new GrowthEventContext();
		ctx.setStudentId("s1");
		// 缺 sourceType / sourceId
		service.record(ctx);
		verify(learningGrowthMapper, never()).insert(any());
		verify(learningGrowthMapper, never()).updateById(any());
	}

	@Test
	void record_insertsOnFirstSightOnly() {
		when(learningGrowthMapper.selectOne(any())).thenReturn(null);
		GrowthEventContext ctx = context("practice", "p9");
		ctx.setQuestionCount(5);
		ctx.setCorrectCount(4);
		ctx.setPoints(20);
		ctx.setCoins(6);
		ctx.setKnowledgePoints(Arrays.asList("分数"));

		service.record(ctx);

		ArgumentCaptor<LearningGrowth> captor = ArgumentCaptor.forClass(LearningGrowth.class);
		verify(learningGrowthMapper).insert(captor.capture());
		LearningGrowth saved = captor.getValue();
		assertThat(saved.getStudentId()).isEqualTo("s1");
		assertThat(saved.getSourceType()).isEqualTo("practice");
		assertThat(saved.getSourceId()).isEqualTo("p9");
		// sourceType 未显式给 eventType 时按来源推导
		assertThat(saved.getEventType()).isEqualTo("PRACTICE");
		assertThat(saved.getQuestionCount()).isEqualTo(5);
		assertThat(saved.getCorrectCount()).isEqualTo(4);
		assertThat(saved.getAccuracy()).isEqualTo(80);
		assertThat(saved.getEventDate()).isNotBlank();
	}

	@Test
	void record_isIdempotentForSameSource() {
		LearningGrowth existing = new LearningGrowth();
		existing.setId("g1");
		existing.setSourceType("practice");
		existing.setSourceId("p9");
		when(learningGrowthMapper.selectOne(any())).thenReturn(existing);

		service.record(context("practice", "p9"));

		verify(learningGrowthMapper, never()).insert(any());
		// 非语法来源重复触发不累加
		verify(learningGrowthMapper, never()).updateById(any());
	}

	@Test
	void record_accumulatesGrammarBySameSource() {
		LearningGrowth existing = new LearningGrowth();
		existing.setId("g1");
		existing.setSourceType("grammar");
		existing.setSourceId("gr1:2026-10-05");
		existing.setQuestionCount(8);
		existing.setCorrectCount(4);
		existing.setPoints(6);
		existing.setCoins(20);
		when(learningGrowthMapper.selectOne(any())).thenReturn(existing);

		GrowthEventContext ctx = context("grammar", "gr1:2026-10-05");
		ctx.setQuestionCount(2);
		ctx.setCorrectCount(2);
		ctx.setPoints(3);
		ctx.setCoins(10);
		service.record(ctx);

		ArgumentCaptor<LearningGrowth> captor = ArgumentCaptor.forClass(LearningGrowth.class);
		verify(learningGrowthMapper).updateById(captor.capture());
		LearningGrowth merged = captor.getValue();
		assertThat(merged.getId()).isEqualTo("g1");
		assertThat(merged.getQuestionCount()).isEqualTo(10);
		assertThat(merged.getCorrectCount()).isEqualTo(6);
		assertThat(merged.getAccuracy()).isEqualTo(60);
		assertThat(merged.getPoints()).isEqualTo(9);
		assertThat(merged.getCoins()).isEqualTo(30);
		verify(learningGrowthMapper, never()).insert(any());
	}

	// ------------------------------------------------------------------ 8.4 成长对比

	@Test
	void compare_newObjectSnapshot_computesDeltaAndResolved() {
		StudentArchive archive = new StudentArchive();
		archive.setBaselineDetectId("d1");
		archive.setProfileSnapshot("{\"detectId\":\"d1\",\"accuracy\":42,\"weakPoints\":["
				+ "{\"kpId\":\"k1\",\"name\":\"分数\",\"accuracy\":30},"
				+ "{\"name\":\"小数\",\"accuracy\":80}]}");
		when(archiveMapper.selectOne(any())).thenReturn(archive);

		when(progressMapper.selectList(any()))
				.thenReturn(Arrays.asList(progress("k1", 75), progress("k2", 50)));
		// 基线第二项无 kpId，按名称回查
		KnowledgePoint kp = new KnowledgePoint();
		kp.setId("k2");
		kp.setName("小数");
		when(knowledgePointMapper.selectOne(any())).thenReturn(kp);
		UserWeakKnowledge newlyWeak = new UserWeakKnowledge();
		newlyWeak.setKnowledgePointId("k9");
		newlyWeak.setStatus("active");
		when(weakMapper.selectList(any())).thenReturn(Collections.singletonList(newlyWeak));
		when(learningGrowthMapper.selectList(any()))
				.thenReturn(Arrays.asList(event(10, 5), event(10, 9)));

		GrowthCompareView view = service.compare("s1", "sub1", "2026-1");

		assertThat(view.getHasBaseline()).isTrue();
		assertThat(view.getBaselineDetectId()).isEqualTo("d1");
		assertThat(view.getBaselineAccuracy()).isEqualTo(42);
		assertThat(view.getBaselineWeakCount()).isEqualTo(2);
		assertThat(view.getResolvedCount()).isEqualTo(1);
		assertThat(view.getRemainingCount()).isEqualTo(1);
		assertThat(view.getNewlyWeakCount()).isEqualTo(1);
		assertThat(view.getEventCount()).isEqualTo(2);
		assertThat(view.getTotalQuestionCount()).isEqualTo(20);
		assertThat(view.getCurrentAccuracy()).isEqualTo(70);

		List<GrowthCompareView.DeltaItem> deltas = view.getDeltas();
		assertThat(deltas).hasSize(2);
		GrowthCompareView.DeltaItem first = deltas.get(0);
		assertThat(first.getKpId()).isEqualTo("k1");
		assertThat(first.getBaselineAccuracy()).isEqualTo(30);
		assertThat(first.getCurrentMastery()).isEqualTo(75);
		assertThat(first.getDelta()).isEqualTo(45);
		assertThat(first.getResolved()).isTrue();

		GrowthCompareView.DeltaItem second = deltas.get(1);
		assertThat(second.getName()).isEqualTo("小数");
		assertThat(second.getBaselineAccuracy()).isEqualTo(80);
		assertThat(second.getCurrentMastery()).isEqualTo(50);
		assertThat(second.getDelta()).isEqualTo(-30);
		assertThat(second.getResolved()).isFalse();
	}

	@Test
	void compare_legacyArraySnapshot_resolvesKpIdByName() {
		StudentArchive archive = new StudentArchive();
		archive.setProfileSnapshot("[{\"name\":\"分数\",\"accuracy\":20}]");
		when(archiveMapper.selectOne(any())).thenReturn(archive);

		KnowledgePoint kp = new KnowledgePoint();
		kp.setId("k1");
		kp.setName("分数");
		when(knowledgePointMapper.selectOne(any())).thenReturn(kp);
		when(progressMapper.selectList(any())).thenReturn(Collections.singletonList(progress("k1", 20)));
		when(weakMapper.selectList(any())).thenReturn(Collections.emptyList());
		when(learningGrowthMapper.selectList(any())).thenReturn(Collections.emptyList());

		GrowthCompareView view = service.compare("s1", "sub1", "2026-1");

		assertThat(view.getHasBaseline()).isTrue();
		assertThat(view.getBaselineAccuracy()).isNull();
		assertThat(view.getBaselineWeakCount()).isEqualTo(1);
		assertThat(view.getResolvedCount()).isZero();
		assertThat(view.getRemainingCount()).isEqualTo(1);
		assertThat(view.getDeltas().get(0).getKpId()).isNull();
		assertThat(view.getDeltas().get(0).getCurrentMastery()).isEqualTo(20);
	}

	@Test
	void compare_withoutArchive_hasNoBaseline() {
		when(archiveMapper.selectOne(any())).thenReturn(null);
		when(progressMapper.selectList(any())).thenReturn(Collections.emptyList());
		when(weakMapper.selectList(any())).thenReturn(Collections.emptyList());
		when(learningGrowthMapper.selectList(any())).thenReturn(Collections.emptyList());

		GrowthCompareView view = service.compare("s1", "sub1", "2026-1");

		assertThat(view.getHasBaseline()).isFalse();
		assertThat(view.getBaselineWeakCount()).isZero();
		assertThat(view.getResolvedCount()).isZero();
		assertThat(view.getDeltas()).isEmpty();
	}

	// ------------------------------------------------------------------ 辅助

	private GrowthEventContext context(String sourceType, String sourceId) {
		GrowthEventContext ctx = new GrowthEventContext();
		ctx.setStudentId("s1");
		ctx.setSubjectId("sub1");
		ctx.setSourceType(sourceType);
		ctx.setSourceId(sourceId);
		return ctx;
	}

	private UserKnowledgeProgress progress(String kpId, int mastery) {
		UserKnowledgeProgress p = new UserKnowledgeProgress();
		p.setUserId("s1");
		p.setSubjectId("sub1");
		p.setKnowledgePointId(kpId);
		p.setMastery(mastery);
		return p;
	}

	private LearningGrowth event(int question, int correct) {
		LearningGrowth g = new LearningGrowth();
		g.setQuestionCount(question);
		g.setCorrectCount(correct);
		return g;
	}

	@SuppressWarnings("unused")
	private Subject subject(String id, String name) {
		Subject s = new Subject();
		s.setId(id);
		s.setName(name);
		return s;
	}

}

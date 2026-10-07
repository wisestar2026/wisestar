package cn.wisestar.server.impl;

import cn.wisestar.server.domain.dto.student.PracticeEvaluationContext;
import cn.wisestar.server.domain.model.Chapter;
import cn.wisestar.server.domain.model.KnowledgePoint;
import cn.wisestar.server.domain.model.Section;
import cn.wisestar.server.domain.model.UserKnowledgeProgress;
import cn.wisestar.server.domain.model.UserWeakKnowledge;
import cn.wisestar.server.domain.model.WeakPointEvent;
import cn.wisestar.server.event.WeakPointEventRecorder;
import cn.wisestar.server.mapper.ChapterMapper;
import cn.wisestar.server.mapper.KnowledgePointMapper;
import cn.wisestar.server.mapper.SectionMapper;
import cn.wisestar.server.mapper.UserKnowledgeProgressMapper;
import cn.wisestar.server.mapper.UserWeakKnowledgeMapper;
import cn.wisestar.server.service.RewardService;
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
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 基线薄弱点播种幂等单元测试（任务 4.4）。
 *
 * <p>验证：仅播种低于薄弱阈值且尚未登记的知识点；重复播种不产生重复记录与事件。</p>
 *
 * @author wisestar
 * @date 2026/10/5
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EvaluationServiceImplTest {

	@Mock
	private UserKnowledgeProgressMapper progressMapper;

	@Mock
	private UserWeakKnowledgeMapper weakMapper;

	@Mock
	private KnowledgePointMapper knowledgePointMapper;

	@Mock
	private SectionMapper sectionMapper;

	@Mock
	private ChapterMapper chapterMapper;

	@Mock
	private RewardService rewardService;

	@Mock
	private WeakPointEventRecorder weakPointEventRecorder;

	private EvaluationServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new EvaluationServiceImpl(progressMapper, weakMapper, knowledgePointMapper, sectionMapper,
				chapterMapper, rewardService, weakPointEventRecorder);
	}

	@Test
	void seedWeakFromBaseline_insertsOnlyWeakAndKnownPoints() {
		stubKnowledgeTree();
		when(weakMapper.selectOne(any())).thenReturn(null);

		Map<String, Integer> baseline = new LinkedHashMap<>();
		baseline.put("分数", 30);      // 低于阈值 55，登记
		baseline.put("小数", 80);      // 已达标，跳过
		baseline.put("缺失知识点", 10); // 学科内不存在，跳过

		service.seedWeakFromBaseline("u1", "sub1", "detect:d1", baseline);

		ArgumentCaptor<UserWeakKnowledge> captor = ArgumentCaptor.forClass(UserWeakKnowledge.class);
		verify(weakMapper).insert(captor.capture());
		UserWeakKnowledge saved = captor.getValue();
		assertThat(saved.getUserId()).isEqualTo("u1");
		assertThat(saved.getSubjectId()).isEqualTo("sub1");
		assertThat(saved.getKnowledgePointId()).isEqualTo("k1");
		assertThat(saved.getStatus()).isEqualTo("active");
		assertThat(saved.getConquerTimes()).isZero();
		assertThat(saved.getFirstWeakAt()).isNotNull();

		ArgumentCaptor<WeakPointEvent> eventCaptor = ArgumentCaptor.forClass(WeakPointEvent.class);
		verify(weakPointEventRecorder).publish(eventCaptor.capture());
		assertThat(eventCaptor.getValue().getKnowledgePointId()).isEqualTo("k1");
	}

	@Test
	void seedWeakFromBaseline_isIdempotentWhenWeakAlreadyExists() {
		stubKnowledgeTree();
		UserWeakKnowledge existing = new UserWeakKnowledge();
		existing.setId("w1");
		existing.setKnowledgePointId("k1");
		when(weakMapper.selectOne(any())).thenReturn(existing);

		Map<String, Integer> baseline = new LinkedHashMap<>();
		baseline.put("分数", 30);

		service.seedWeakFromBaseline("u1", "sub1", "detect:d1", baseline);

		verify(weakMapper, never()).insert(any());
		verify(weakPointEventRecorder, never()).publish(any(WeakPointEvent.class));
	}

	@Test
	void seedWeakFromBaseline_ignoresBlankInput() {
		service.seedWeakFromBaseline("u1", "sub1", "detect:d1", Collections.emptyMap());
		service.seedWeakFromBaseline("", "sub1", "detect:d1", Collections.singletonMap("分数", 30));

		verify(chapterMapper, never()).selectList(any());
		verify(weakMapper, never()).insert(any());
	}

	@Test
	void recordPractice_createsWeakWhenMasteryBelowThreshold() {
		stubResolvableContext();
		when(progressMapper.selectOne(any())).thenReturn(null);
		when(weakMapper.selectOne(any())).thenReturn(null);
		when(progressMapper.selectList(any())).thenReturn(Collections.emptyList());

		service.recordPractice(oneItemContext("k1", false));

		ArgumentCaptor<UserWeakKnowledge> captor = ArgumentCaptor.forClass(UserWeakKnowledge.class);
		verify(weakMapper).insert(captor.capture());
		UserWeakKnowledge saved = captor.getValue();
		assertThat(saved.getStatus()).isEqualTo("active");
		assertThat(saved.getKnowledgePointId()).isEqualTo("k1");
		assertThat(saved.getSubjectId()).isEqualTo("sub1");
	}

	@Test
	void recordPractice_conquersWeakWhenMasteryReachesThreshold() {
		stubResolvableContext();
		UserKnowledgeProgress progress = new UserKnowledgeProgress();
		progress.setId("pg1");
		progress.setUserId("u1");
		progress.setSubjectId("sub1");
		progress.setVersionId("v1");
		progress.setKnowledgePointId("k1");
		progress.setMastery(0);
		progress.setRecentRates("0");
		progress.setTimes(1);
		when(progressMapper.selectOne(any())).thenReturn(progress);
		UserWeakKnowledge weak = new UserWeakKnowledge();
		weak.setId("w1");
		weak.setUserId("u1");
		weak.setKnowledgePointId("k1");
		weak.setStatus("active");
		weak.setConquerTimes(0);
		when(weakMapper.selectOne(any())).thenReturn(weak);
		when(progressMapper.selectList(any())).thenReturn(Collections.emptyList());
		when(knowledgePointMapper.selectList(any()))
				.thenReturn(Collections.singletonList(knowledgePoint("k1", "分数", "sec1")));

		service.recordPractice(oneItemContext("k1", true));

		ArgumentCaptor<UserWeakKnowledge> captor = ArgumentCaptor.forClass(UserWeakKnowledge.class);
		verify(weakMapper).updateById(captor.capture());
		assertThat(captor.getValue().getStatus()).isEqualTo("conquered");
	}

	private void stubResolvableContext() {
		when(knowledgePointMapper.selectById("k1")).thenReturn(knowledgePoint("k1", "分数", "sec1"));
		Section section = new Section();
		section.setId("sec1");
		section.setChapterId("ch1");
		when(sectionMapper.selectById("sec1")).thenReturn(section);
		Chapter chapter = new Chapter();
		chapter.setId("ch1");
		chapter.setSubjectId("sub1");
		chapter.setVersion("v1");
		when(chapterMapper.selectById("ch1")).thenReturn(chapter);
	}

	private KnowledgePoint knowledgePoint(String id, String name, String sectionId) {
		KnowledgePoint kp = new KnowledgePoint();
		kp.setId(id);
		kp.setName(name);
		kp.setSectionId(sectionId);
		return kp;
	}

	private PracticeEvaluationContext oneItemContext(String kpId, boolean correct) {
		PracticeEvaluationContext context = new PracticeEvaluationContext();
		context.setUserId("u1");
		context.setKnowledgePointId(kpId);
		PracticeEvaluationContext.Item item = new PracticeEvaluationContext.Item();
		item.setQuestionId("q1");
		item.setCorrect(correct);
		context.getItems().add(item);
		return context;
	}

	private void stubKnowledgeTree() {
		Chapter chapter = new Chapter();
		chapter.setId("ch1");
		chapter.setSubjectId("sub1");
		when(chapterMapper.selectList(any())).thenReturn(Collections.singletonList(chapter));

		Section section = new Section();
		section.setId("sec1");
		section.setChapterId("ch1");
		when(sectionMapper.selectList(any())).thenReturn(Collections.singletonList(section));

		KnowledgePoint kp1 = new KnowledgePoint();
		kp1.setId("k1");
		kp1.setName("分数");
		KnowledgePoint kp2 = new KnowledgePoint();
		kp2.setId("k2");
		kp2.setName("小数");
		when(knowledgePointMapper.selectList(any())).thenReturn(Arrays.asList(kp1, kp2));
	}

}

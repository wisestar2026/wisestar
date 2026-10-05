package cn.wisestar.server.impl;

import cn.wisestar.server.domain.dto.UserInfo;
import cn.wisestar.server.domain.dto.detect.DetectReportView;
import cn.wisestar.server.domain.dto.detect.DetectSubmitRequest;
import cn.wisestar.server.domain.model.DetectRecord;
import cn.wisestar.server.mapper.ChapterMapper;
import cn.wisestar.server.mapper.ChapterRepoMapper;
import cn.wisestar.server.mapper.DetectRecordMapper;
import cn.wisestar.server.mapper.SectionMapper;
import cn.wisestar.server.mapper.SectionRepoMapper;
import cn.wisestar.server.mapper.SubjectMapper;
import cn.wisestar.server.mapper.TemplateMapper;
import cn.wisestar.server.service.EvaluationService;
import cn.wisestar.server.service.GrowthArchiveService;
import cn.wisestar.server.service.StudentArchiveService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 检测落库与基线判定单元测试（任务 1.5）。
 *
 * <p>验证：某学员某学科某学期首测标 PRE + {@code is_baseline=1} 且写基线唯一键；
 * 已有 PRE 后转为 STAGE；重复 clientToken 复用既有记录不再落库。</p>
 *
 * @author wisestar
 * @date 2026/10/5
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DetectionServiceImplTest {

	@Mock
	private SubjectMapper subjectMapper;

	@Mock
	private ChapterMapper chapterMapper;

	@Mock
	private TemplateMapper templateMapper;

	@Mock
	private ChapterRepoMapper chapterRepoMapper;

	@Mock
	private SectionMapper sectionMapper;

	@Mock
	private SectionRepoMapper sectionRepoMapper;

	@Mock
	private DetectRecordMapper detectRecordMapper;

	@Mock
	private EvaluationService evaluationService;

	@Mock
	private GrowthArchiveService growthArchiveService;

	@Mock
	private StudentArchiveService studentArchiveService;

	private DetectionServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new DetectionServiceImpl(subjectMapper, chapterMapper, templateMapper, chapterRepoMapper,
				sectionMapper, sectionRepoMapper, detectRecordMapper, evaluationService, growthArchiveService,
				studentArchiveService, new ObjectMapper());
		UserInfo user = new UserInfo();
		user.setUserId("s1");
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void persistRecord_marksFirstDetectAsBaseline() throws Exception {
		when(detectRecordMapper.selectCount(any())).thenReturn(0L);
		DetectSubmitRequest request = request();
		DetectReportView report = report();

		invokePersist(request, report);

		ArgumentCaptor<DetectRecord> captor = ArgumentCaptor.forClass(DetectRecord.class);
		verify(detectRecordMapper).insert(captor.capture());
		DetectRecord saved = captor.getValue();
		assertThat(saved.getStudentId()).isEqualTo("s1");
		assertThat(saved.getSubjectId()).isEqualTo("sub1");
		assertThat(saved.getSemester()).isEqualTo("2026-1");
		assertThat(saved.getDetectType()).isEqualTo("PRE");
		assertThat(saved.getIsBaseline()).isTrue();
		assertThat(saved.getBaselineKey()).isEqualTo("s1:sub1:2026-1");

		assertThat(report.getDetectType()).isEqualTo("PRE");
		assertThat(report.isBaseline()).isTrue();
	}

	@Test
	void persistRecord_marksLaterDetectAsStage() throws Exception {
		when(detectRecordMapper.selectCount(any())).thenReturn(1L);
		DetectSubmitRequest request = request();
		DetectReportView report = report();

		invokePersist(request, report);

		ArgumentCaptor<DetectRecord> captor = ArgumentCaptor.forClass(DetectRecord.class);
		verify(detectRecordMapper).insert(captor.capture());
		DetectRecord saved = captor.getValue();
		assertThat(saved.getDetectType()).isEqualTo("STAGE");
		assertThat(saved.getIsBaseline()).isFalse();
		assertThat(saved.getBaselineKey()).isNull();
		assertThat(report.isBaseline()).isFalse();
	}

	@Test
	void persistRecord_reusesExistingRecordForSameClientToken() throws Exception {
		DetectRecord existing = new DetectRecord();
		existing.setId("rec1");
		existing.setDetectType("PRE");
		existing.setIsBaseline(true);
		when(detectRecordMapper.selectOne(any())).thenReturn(existing);

		DetectSubmitRequest request = request();
		request.setClientToken("token-1");
		DetectReportView report = report();

		invokePersist(request, report);

		verify(detectRecordMapper, never()).insert(any());
		assertThat(report.getRecordId()).isEqualTo("rec1");
		assertThat(report.isBaseline()).isTrue();
	}

	private void invokePersist(DetectSubmitRequest request, DetectReportView report) throws Exception {
		Method method = DetectionServiceImpl.class.getDeclaredMethod("persistRecord", DetectSubmitRequest.class,
				DetectReportView.class);
		method.setAccessible(true);
		method.invoke(service, request, report);
	}

	private DetectSubmitRequest request() {
		DetectSubmitRequest request = new DetectSubmitRequest();
		request.setSubjectId("sub1");
		request.setSemester("2026-1");
		request.setGrade("6");
		request.setTerm("2");
		request.setQuestionCount(10);
		request.setDurationMs(60_000L);
		return request;
	}

	private DetectReportView report() {
		DetectReportView report = new DetectReportView();
		report.setTotal(10);
		report.setCorrect(6);
		report.setAccuracy(60);
		return report;
	}

}

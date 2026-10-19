package cn.wisestar.server.api;

import cn.wisestar.server.core.uitls.SecurityContextUtils;
import cn.wisestar.server.domain.dto.detect.DetectReportView;
import cn.wisestar.server.domain.dto.english.EnglishDiagnosePaperView;
import cn.wisestar.server.domain.dto.english.EnglishDiagnoseRequest;
import cn.wisestar.server.domain.dto.english.EnglishDiagnoseSubmitRequest;
import cn.wisestar.server.domain.dto.english.EnglishGrammarView;
import cn.wisestar.server.domain.dto.english.EnglishPatternScenarioView;
import cn.wisestar.server.domain.dto.english.EnglishReadAloudView;
import cn.wisestar.server.domain.dto.english.EnglishReadingView;
import cn.wisestar.server.domain.dto.english.EnglishScenarioView;
import cn.wisestar.server.domain.dto.english.EnglishSentenceExercise;
import cn.wisestar.server.domain.dto.english.EnglishSentenceView;
import cn.wisestar.server.domain.dto.english.EnglishUnitPaperRequest;
import cn.wisestar.server.domain.dto.english.EnglishUnitPaperSubmitRequest;
import cn.wisestar.server.domain.dto.english.EnglishUnitProgressView;
import cn.wisestar.server.domain.dto.english.EnglishWrongQuestionView;
import cn.wisestar.server.domain.dto.english.ReviewSessionView;
import cn.wisestar.server.domain.dto.student.StudentPreviewCompleteView;
import cn.wisestar.server.domain.dto.student.StudentQuestionView;
import cn.wisestar.server.domain.dto.student.StudentWrongRedoRequest;
import cn.wisestar.server.domain.dto.student.StudentWrongRedoView;
import cn.wisestar.server.service.EnglishReadAloudService;
import cn.wisestar.server.service.EnglishReadingService;
import cn.wisestar.server.service.EnglishScenarioPackService;
import cn.wisestar.server.service.EnglishSentencePackService;
import cn.wisestar.server.service.EnglishStudentService;
import cn.wisestar.server.service.EnglishUnitPaperService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 英语学员学习接口（学生端学习中心）。
 *
 * @author wisestar
 * @date 2026/9/15
 */
@RestController
@RequestMapping("${api.prefix}/english/student")
@RequiredArgsConstructor
public class EnglishStudentApi {

	private final EnglishStudentService englishStudentService;

	private final EnglishScenarioPackService scenarioPackService;

	private final EnglishSentencePackService sentencePackService;

	private final EnglishReadingService readingService;

	private final EnglishReadAloudService readAloudService;

	private final EnglishUnitPaperService unitPaperService;

	/**
	 * 单元列表 + 学习进度。
	 */
	@GetMapping("/units")
	@PreAuthorize("isAuthenticated()")
	public List<EnglishUnitProgressView> units(@RequestParam(required = false) String version,
			@RequestParam(required = false) String grade,
			@RequestParam(required = false) String term) {
		return englishStudentService.unitProgress(SecurityContextUtils.getUserId(), version, grade, term);
	}

	/**
	 * 单元句子列表（含熟练度）。
	 */
	@GetMapping("/sentences")
	@PreAuthorize("isAuthenticated()")
	public List<EnglishSentenceView> sentences(@RequestParam(required = false) String version,
			@RequestParam(required = false) String grade,
			@RequestParam(required = false) String term,
			@RequestParam(required = false) String unit) {
		return englishStudentService.sentences(SecurityContextUtils.getUserId(), version, grade, term, unit);
	}

	/**
	 * 单元重点语法列表（含掌握度，学员端）。
	 */
	@GetMapping("/grammars")
	@PreAuthorize("isAuthenticated()")
	public List<EnglishGrammarView> grammars(@RequestParam(required = false) String version,
			@RequestParam(required = false) String grade,
			@RequestParam(required = false) String term,
			@RequestParam(required = false) String unit) {
		return englishStudentService.grammars(SecurityContextUtils.getUserId(), version, grade, term, unit);
	}

	/**
	 * 记录语法作答（{grammarId, correct}）。
	 */
	@PostMapping("/grammar/record")
	@PreAuthorize("isAuthenticated()")
	public void recordGrammar(@RequestBody Map<String, Object> request) {
		String userId = SecurityContextUtils.getUserId();
		String grammarId = String.valueOf(request.get("grammarId"));
		boolean correct = Boolean.parseBoolean(String.valueOf(request.get("correct")));
		englishStudentService.recordGrammar(userId, grammarId, correct);
	}

	/**
	 * 待学习/复习句子。
	 */
	@GetMapping("/sentence/study")
	@PreAuthorize("isAuthenticated()")
	public List<EnglishSentenceView> studySentences(@RequestParam(defaultValue = "10") Integer limit) {
		return englishStudentService.studySentences(SecurityContextUtils.getUserId(), limit);
	}

	/**
	 * 记录句子作答（{sentenceId, correct}）。
	 */
	@PostMapping("/sentence/record")
	@PreAuthorize("isAuthenticated()")
	public void recordSentence(@RequestBody Map<String, Object> request) {
		String userId = SecurityContextUtils.getUserId();
		String sentenceId = String.valueOf(request.get("sentenceId"));
		boolean correct = Boolean.parseBoolean(String.valueOf(request.get("correct")));
		englishStudentService.recordSentence(userId, sentenceId, correct);
	}

	/**
	 * 智能复习会话（单词 + 句子混合队列）。
	 */
	@GetMapping("/review")
	@PreAuthorize("isAuthenticated()")
	public List<ReviewSessionView> review(@RequestParam(defaultValue = "20") Integer limit) {
		return englishStudentService.reviewSession(SecurityContextUtils.getUserId(), limit);
	}

	/**
	 * 情景对话练习：读取该单元已固化的情景内容（按组合并展示，不触发实时 AI）。
	 */
	@GetMapping("/scenarios")
	@PreAuthorize("isAuthenticated()")
	public List<EnglishScenarioView> scenarios(@RequestParam(required = false) String version,
			@RequestParam(required = false) String grade,
			@RequestParam(required = false) String term,
			@RequestParam(required = false) String unit) {
		return scenarioPackService.getWordScenarios(version, grade, term, unit);
	}

	/**
	 * 重点句式情景练习：读取该单元已固化的情景内容（按组合并展示，不触发实时 AI）。
	 */
	@GetMapping("/pattern-scenarios")
	@PreAuthorize("isAuthenticated()")
	public List<EnglishPatternScenarioView> patternScenarios(@RequestParam(required = false) String version,
			@RequestParam(required = false) String grade,
			@RequestParam(required = false) String term,
			@RequestParam(required = false) String unit) {
		return scenarioPackService.getPatternScenarios(version, grade, term, unit);
	}

	/**
	 * 单元阅读理解：读取已固化的多篇文章；无内容时生成 1 篇并缓存。
	 */
	@GetMapping("/reading")
	@PreAuthorize("isAuthenticated()")
	public List<EnglishReadingView> reading(@RequestParam(required = false) String version,
			@RequestParam(required = false) String grade,
			@RequestParam(required = false) String term,
			@RequestParam(required = false) String unit) {
		return readingService.getReadings(version, grade, term, unit);
	}

	/**
	 * 单元课文跟读：读取课本篇章与逐句中英文。
	 */
	@GetMapping("/read-aloud")
	@PreAuthorize("isAuthenticated()")
	public List<EnglishReadAloudView> readAloud(@RequestParam(required = false) String version,
			@RequestParam(required = false) String grade,
			@RequestParam(required = false) String term,
			@RequestParam(required = false) String unit) {
		return readAloudService.getPassages(version, grade, term, unit);
	}

	/**
	 * 单元句子同步练习：读取该单元已固化的多题型练习（连词成句/句子默写/句子选择，不触发实时 AI）。
	 */
	@GetMapping("/sentence-exercises")
	@PreAuthorize("isAuthenticated()")
	public List<EnglishSentenceExercise> sentenceExercises(@RequestParam(required = false) String version,
			@RequestParam(required = false) String grade,
			@RequestParam(required = false) String term,
			@RequestParam(required = false) String unit) {
		return sentencePackService.getExercises(version, grade, term, unit);
	}

	/**
	 * 英语复习错题：聚合学员在英语学科检测记录里做错的题目（隐藏答案解析）。
	 */
	@GetMapping("/wrong-questions")
	@PreAuthorize("isAuthenticated()")
	public List<EnglishWrongQuestionView> wrongQuestions(@RequestParam(required = false) Integer limit) {
		return englishStudentService.wrongQuestions(SecurityContextUtils.getUserId(), limit);
	}

	/**
	 * 取单道英语错题的重做用题目（含标准答案，仅本人错题可读）。
	 */
	@GetMapping("/wrong-question")
	@PreAuthorize("isAuthenticated()")
	public StudentQuestionView wrongQuestion(@RequestParam String questionId) {
		return englishStudentService.wrongQuestion(SecurityContextUtils.getUserId(), questionId);
	}

	/**
	 * 英语错题重做：答对即消灭（幂等落库），之后不再出现在复习错题中。
	 */
	@PostMapping("/wrong/redo")
	@PreAuthorize("isAuthenticated()")
	public StudentWrongRedoView wrongRedo(@RequestBody StudentWrongRedoRequest request) {
		return englishStudentService.redoWrong(SecurityContextUtils.getUserId(), request);
	}

	/**
	 * 记录情景练习结果（{version, grade, term, unit, module, correct, total}）。
	 *
	 * <p>用于单词情景对话与重点句式情景：整组提交后按正确率纳入「单元·模块」掌握度。</p>
	 */
	@PostMapping("/scenario/record")
	@PreAuthorize("isAuthenticated()")
	public void recordScenario(@RequestBody Map<String, Object> request) {
		String userId = SecurityContextUtils.getUserId();
		englishStudentService.recordScenarioPractice(userId,
				stringValue(request.get("version")), stringValue(request.get("grade")),
				stringValue(request.get("term")), stringValue(request.get("unit")),
				stringValue(request.get("module")), parseInt(request.get("correct")),
				parseInt(request.get("total")));
	}

	/**
	 * 英语单元练习组卷（内容包阶梯卷，按知识点 D1→D3 递进取题）。
	 */
	@PostMapping("/unit-paper")
	@PreAuthorize("isAuthenticated()")
	public List<StudentQuestionView> unitPaper(@RequestBody EnglishUnitPaperRequest request) {
		return unitPaperService.generate(request);
	}

	/**
	 * 英语单元练习交卷：按内容键回读题库判分并生成诊断报告。
	 */
	@PostMapping("/unit-paper/submit")
	@PreAuthorize("isAuthenticated()")
	public DetectReportView submitUnitPaper(@RequestBody EnglishUnitPaperSubmitRequest request) {
		return unitPaperService.submit(request);
	}

	/**
	 * 英语学习诊断组卷：D1→D3 三档各 8 题（共 24 题），跨单元抽取知识点（家长向诊断卷）。
	 */
	@PostMapping("/diagnose-paper")
	@PreAuthorize("isAuthenticated()")
	public EnglishDiagnosePaperView diagnosePaper(@RequestBody EnglishDiagnoseRequest request) {
		return unitPaperService.diagnosePaper(request);
	}

	/**
	 * 英语学习诊断交卷：生成家长向报告并落诊断记录（含学习趋势）。
	 */
	@PostMapping("/diagnose-submit")
	@PreAuthorize("isAuthenticated()")
	public DetectReportView diagnoseSubmit(@RequestBody EnglishDiagnoseSubmitRequest request) {
		return unitPaperService.diagnoseSubmit(request);
	}

	/**
	 * 记录学习会话（{type, durationSeconds, correctCount}），返回奖励结算结果。
	 */
	@PostMapping("/session")
	@PreAuthorize("isAuthenticated()")
	public StudentPreviewCompleteView session(@RequestBody Map<String, Object> request) {
		String userId = SecurityContextUtils.getUserId();
		String type = String.valueOf(request.getOrDefault("type", "word"));
		int durationSeconds = parseInt(request.get("durationSeconds"));
		int correctCount = parseInt(request.get("correctCount"));
		return englishStudentService.recordSession(userId, type, durationSeconds, correctCount);
	}

	private int parseInt(Object value) {
		if (value == null) {
			return 0;
		}
		try {
			return (int) Double.parseDouble(String.valueOf(value));
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private String stringValue(Object value) {
		if (value == null) {
			return null;
		}
		String text = String.valueOf(value).trim();
		return text.isEmpty() ? null : text;
	}

}

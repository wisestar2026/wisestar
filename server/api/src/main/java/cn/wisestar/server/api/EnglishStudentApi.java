package cn.wisestar.server.api;

import cn.wisestar.server.core.uitls.SecurityContextUtils;
import cn.wisestar.server.domain.dto.english.EnglishGrammarView;
import cn.wisestar.server.domain.dto.english.EnglishSentenceView;
import cn.wisestar.server.domain.dto.english.EnglishUnitProgressView;
import cn.wisestar.server.domain.dto.english.ReviewSessionView;
import cn.wisestar.server.domain.dto.student.StudentPreviewCompleteView;
import cn.wisestar.server.service.EnglishStudentService;
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
	 * 单元重点语法列表（学员端只读）。
	 */
	@GetMapping("/grammars")
	@PreAuthorize("isAuthenticated()")
	public List<EnglishGrammarView> grammars(@RequestParam(required = false) String version,
			@RequestParam(required = false) String grade,
			@RequestParam(required = false) String term,
			@RequestParam(required = false) String unit) {
		return englishStudentService.grammars(version, grade, term, unit);
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

}

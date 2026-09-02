package cn.wisestar.server.api;

import cn.wisestar.server.domain.dto.english.ImportResult;
import cn.wisestar.server.service.EnglishWordAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 英语单词 AI 生成接口。
 *
 * @author wisestar
 * @date 2026/8/30
 */
@RestController
@RequestMapping("${api.prefix}/english/word-ai")
@RequiredArgsConstructor
public class EnglishWordAiApi {

	private final EnglishWordAiService wordAiService;

	/**
	 * 为单个单词生成 AI 内容（图片/音频/例句）。
	 */
	@PostMapping("/generate")
	@PreAuthorize("hasAuthority('english:word:update')")
	public void generateContent(@RequestParam String id) {
		wordAiService.batchGenerateContent(java.util.Arrays.asList(id));
	}

	/**
	 * 批量为单词生成 AI 内容。
	 */
	@PostMapping("/batch-generate")
	@PreAuthorize("hasAuthority('english:word:update')")
	public ImportResult batchGenerate(@RequestBody Map<String, Object> request) {
		@SuppressWarnings("unchecked")
		List<String> wordIds = (List<String>) request.get("wordIds");
		return wordAiService.batchGenerateContent(wordIds);
	}

	/**
	 * 按条件批量生成 AI 内容（版本/年级/单元）。
	 */
	@PostMapping("/generate-by-condition")
	@PreAuthorize("hasAuthority('english:word:update')")
	public ImportResult generateByCondition(
		@RequestParam(required = false) String version,
		@RequestParam(required = false) String grade,
		@RequestParam(required = false) String unit
	) {
		return wordAiService.batchGenerateByCondition(version, grade, unit);
	}

}

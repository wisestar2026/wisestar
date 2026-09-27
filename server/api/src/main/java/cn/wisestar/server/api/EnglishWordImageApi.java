package cn.wisestar.server.api;

import cn.wisestar.server.domain.dto.english.ImportResult;
import cn.wisestar.server.domain.dto.english.WordImageCandidateView;
import cn.wisestar.server.domain.dto.english.WordImageView;
import cn.wisestar.server.service.EnglishWordImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 英语单词配图接口（后台管理端）。
 *
 * <p>提供候选图抓取、确认入库、手动上传三种能力，权限沿用
 * {@code english:word:update}。</p>
 *
 * @author wisestar
 * @date 2026/9/18
 */
@RestController
@RequestMapping("${api.prefix}/english/word-image")
@RequiredArgsConstructor
public class EnglishWordImageApi {

	private final EnglishWordImageService wordImageService;

	/**
	 * 为若干单词抓取候选图片（只读）。
	 */
	@PostMapping("/candidates")
	@PreAuthorize("hasAuthority('english:word:update')")
	public List<WordImageCandidateView> candidates(@RequestBody Map<String, Object> request) {
		@SuppressWarnings("unchecked")
		List<String> wordIds = (List<String>) request.get("wordIds");
		return wordImageService.fetchCandidates(wordIds);
	}

	/**
	 * 确认候选图并入库（下载外部图片后回写单词图片地址）。
	 */
	@PostMapping("/confirm")
	@PreAuthorize("hasAuthority('english:word:update')")
	public WordImageView confirm(@RequestBody Map<String, String> request) {
		return wordImageService.confirmCandidate(request.get("wordId"), request.get("imageUrl"));
	}

	/**
	 * 手动上传图片并入库。
	 */
	@PostMapping("/upload")
	@PreAuthorize("hasAuthority('english:word:update')")
	public WordImageView upload(@RequestParam String wordId, @RequestParam MultipartFile file) {
		return wordImageService.uploadForWord(wordId, file);
	}

	/**
	 * 按条件批量补图：缺图且词性为名词或形容词的单词自动取候选图首图入库。
	 */
	@PostMapping("/auto-fill")
	@PreAuthorize("hasAuthority('english:word:update')")
	public ImportResult autoFill(@RequestBody(required = false) Map<String, String> request) {
		Map<String, String> body = request == null ? Collections.emptyMap() : request;
		return wordImageService.autoFillByCondition(
				body.get("version"), body.get("grade"), body.get("term"), body.get("unit"));
	}

}

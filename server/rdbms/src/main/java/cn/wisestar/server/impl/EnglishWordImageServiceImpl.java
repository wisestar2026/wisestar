package cn.wisestar.server.impl;

import cn.wisestar.server.domain.dto.FileView;
import cn.wisestar.server.domain.dto.UploadFileRequest;
import cn.wisestar.server.domain.dto.english.WordImageCandidateView;
import cn.wisestar.server.domain.dto.english.WordImageView;
import cn.wisestar.server.domain.model.EnglishWord;
import cn.wisestar.server.mapper.EnglishWordMapper;
import cn.wisestar.server.service.EnglishDictionaryService;
import cn.wisestar.server.service.EnglishWordImageService;
import cn.wisestar.server.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.ValidationException;
import java.util.ArrayList;
import java.util.List;

/**
 * 英语单词配图服务实现。
 *
 * <p>候选图来自词典接口的附图字段，确认入库时下载图片并复用
 * {@link FileService#upload} 落盘，单词 {@code imageUrl} 指向
 * {@code /api/file?id=<fileId>}（免登录可访问）。</p>
 *
 * @author wisestar
 * @date 2026/9/18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnglishWordImageServiceImpl implements EnglishWordImageService {

	/** 图片访问地址前缀（免登录） */
	private static final String FILE_URL_PREFIX = "/api/file?id=";

	private final EnglishWordMapper englishWordMapper;

	private final EnglishDictionaryService dictionaryService;

	private final FileService fileService;

	private final RestTemplate restTemplate = new RestTemplate();

	@Override
	public List<WordImageCandidateView> fetchCandidates(List<String> wordIds) {
		List<WordImageCandidateView> result = new ArrayList<>();
		if (wordIds == null || wordIds.isEmpty()) {
			return result;
		}
		for (String wordId : wordIds) {
			if (isBlank(wordId)) {
				continue;
			}
			EnglishWord word = englishWordMapper.selectById(wordId);
			if (word == null) {
				continue;
			}
			WordImageCandidateView view = new WordImageCandidateView();
			view.setWordId(word.getId());
			view.setSpell(word.getSpell());
			view.setMeaning(word.getMeaning());
			view.setCandidates(dictionaryService.lookupImages(word.getSpell()));
			result.add(view);
		}
		return result;
	}

	@Override
	public WordImageView confirmCandidate(String wordId, String imageUrl) {
		EnglishWord word = requireWord(wordId);
		if (isBlank(imageUrl)) {
			throw new ValidationException("请先选择候选图片");
		}
		byte[] bytes = download(imageUrl);
		if (bytes == null || bytes.length == 0) {
			throw new ValidationException("候选图片下载失败");
		}
		String storedUrl = store(word.getSpell(), bytes, guessContentType(imageUrl));
		word.setImageUrl(storedUrl);
		englishWordMapper.updateById(word);
		return toView(word);
	}

	@Override
	public WordImageView uploadForWord(String wordId, MultipartFile file) {
		EnglishWord word = requireWord(wordId);
		if (file == null || file.isEmpty()) {
			throw new ValidationException("请选择图片文件");
		}
		FileView fileView = fileService.upload(buildRequest(file));
		word.setImageUrl(FILE_URL_PREFIX + fileView.getId());
		englishWordMapper.updateById(word);
		return toView(word);
	}

	/**
	 * 下载外部图片字节，失败返回 {@code null}。
	 */
	private byte[] download(String url) {
		try {
			return restTemplate.getForObject(url, byte[].class);
		} catch (Exception e) {
			log.warn("候选图片下载失败 url={} err={}", url, e.getMessage());
			return null;
		}
	}

	/**
	 * 把图片字节存储到文件服务，返回访问地址。
	 */
	private String store(String spell, byte[] bytes, String contentType) {
		String originalName = (isBlank(spell) ? "word" : spell.trim()) + extensionOf(contentType);
		ByteArrayMultipartFile multipartFile = new ByteArrayMultipartFile("file", originalName, contentType, bytes);
		FileView fileView = fileService.upload(buildRequest(multipartFile));
		return FILE_URL_PREFIX + fileView.getId();
	}

	private UploadFileRequest buildRequest(MultipartFile file) {
		UploadFileRequest request = new UploadFileRequest();
		request.setFile(file);
		request.setFileType(0);
		return request;
	}

	private EnglishWord requireWord(String wordId) {
		if (isBlank(wordId)) {
			throw new ValidationException("单词 ID 不能为空");
		}
		EnglishWord word = englishWordMapper.selectById(wordId);
		if (word == null) {
			throw new ValidationException("单词不存在");
		}
		return word;
	}

	private WordImageView toView(EnglishWord word) {
		WordImageView view = new WordImageView();
		view.setWordId(word.getId());
		view.setSpell(word.getSpell());
		view.setImageUrl(word.getImageUrl());
		return view;
	}

	/**
	 * 依据图片地址后缀推断 Content-Type。
	 */
	private String guessContentType(String url) {
		String lower = url == null ? "" : url.toLowerCase();
		if (lower.contains(".png")) {
			return "image/png";
		}
		if (lower.contains(".gif")) {
			return "image/gif";
		}
		if (lower.contains(".webp")) {
			return "image/webp";
		}
		if (lower.contains(".jpeg") || lower.contains(".jpg")) {
			return "image/jpeg";
		}
		return "image/jpeg";
	}

	/**
	 * 依据 Content-Type 推断文件后缀。
	 */
	private String extensionOf(String contentType) {
		if (contentType == null) {
			return ".jpg";
		}
		switch (contentType.toLowerCase()) {
			case "image/png":
				return ".png";
			case "image/gif":
				return ".gif";
			case "image/webp":
				return ".webp";
			default:
				return ".jpg";
		}
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

}

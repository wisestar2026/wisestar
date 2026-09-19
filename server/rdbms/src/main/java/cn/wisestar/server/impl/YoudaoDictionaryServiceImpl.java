package cn.wisestar.server.impl;

import cn.wisestar.server.domain.dto.english.DictionaryEntryView;
import cn.wisestar.server.service.EnglishDictionaryService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 有道词典查询服务实现。
 *
 * <p>使用免费公开接口 {@code https://dict.youdao.com/jsonapi?q=<word>}，无需密钥，
 * 返回音标（ec.word[].usphone/ukphone）、中文释义（ec.word[].trs）、
 * 双语例句（blng_sents_part.sentence-pair[]）与附图（pic_dict.pic[]）。</p>
 *
 * @author wisestar
 * @date 2026/9/18
 */
@Slf4j
@Service
public class YoudaoDictionaryServiceImpl implements EnglishDictionaryService {

	/** 有道公开词典接口前缀 */
	private static final String API_PREFIX = "https://dict.youdao.com/jsonapi?q=";

	/** 单词候选图最多返回数量 */
	private static final int MAX_CANDIDATES = 6;

	private final RestTemplate restTemplate = new RestTemplate();

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public DictionaryEntryView lookup(String word) {
		JsonNode root = query(word);
		if (root == null) {
			return null;
		}
		DictionaryEntryView view = new DictionaryEntryView();
		view.setWord(word);
		view.setPhonetic(extractPhonetic(root));
		view.setMeaning(extractMeaning(root));
		view.setExampleSentence(extractExample(root));
		if (isBlank(view.getPhonetic()) && isBlank(view.getMeaning()) && isBlank(view.getExampleSentence())) {
			return null;
		}
		return view;
	}

	@Override
	public List<String> lookupImages(String word) {
		List<String> urls = new ArrayList<>();
		JsonNode root = query(word);
		if (root == null) {
			return urls;
		}
		JsonNode pics = root.path("pic_dict").path("pic");
		if (pics.isArray()) {
			for (JsonNode pic : pics) {
				String url = text(pic, "image");
				if (isBlank(url)) {
					url = text(pic, "url");
				}
				if (isBlank(url)) {
					continue;
				}
				url = url.trim();
				// 有道图片地址可能以 ? 结尾，去掉末尾问号
				while (url.endsWith("?")) {
					url = url.substring(0, url.length() - 1);
				}
				if (!url.isEmpty() && !urls.contains(url)) {
					urls.add(url);
				}
				if (urls.size() >= MAX_CANDIDATES) {
					break;
				}
			}
		}
		return urls;
	}

	/**
	 * 请求有道接口并解析为 JSON 树，失败返回 {@code null}。
	 */
	private JsonNode query(String word) {
		if (isBlank(word)) {
			return null;
		}
		try {
			String url = API_PREFIX + URLEncoder.encode(word.trim(), StandardCharsets.UTF_8);
			String body = restTemplate.getForObject(url, String.class);
			if (isBlank(body)) {
				return null;
			}
			return objectMapper.readTree(body);
		} catch (Exception e) {
			log.warn("有道词典查询失败 word={} err={}", word, e.getMessage());
			return null;
		}
	}

	/**
	 * 提取音标：优先美音 usphone，其次英音 ukphone。
	 */
	private String extractPhonetic(JsonNode root) {
		JsonNode word = firstWord(root.path("ec").path("word"));
		if (word == null) {
			word = firstWord(root.path("simple").path("word"));
		}
		if (word == null) {
			return null;
		}
		String phonetic = text(word, "usphone");
		if (isBlank(phonetic)) {
			phonetic = text(word, "ukphone");
		}
		return isBlank(phonetic) ? null : phonetic.trim();
	}

	/**
	 * 提取中文释义：拼接 ec.word[].trs[].tr[].l.i[]。
	 */
	private String extractMeaning(JsonNode root) {
		JsonNode word = firstWord(root.path("ec").path("word"));
		if (word == null) {
			return null;
		}
		List<String> parts = new ArrayList<>();
		JsonNode trs = word.path("trs");
		if (trs.isArray()) {
			for (JsonNode trsItem : trs) {
				JsonNode tr = trsItem.path("tr");
				if (!tr.isArray()) {
					continue;
				}
				for (JsonNode item : tr) {
					JsonNode lines = item.path("l").path("i");
					if (lines.isArray()) {
						for (JsonNode line : lines) {
							String value = line.asText();
							if (!isBlank(value)) {
								parts.add(value.trim());
							}
						}
					}
				}
			}
		}
		if (parts.isEmpty()) {
			return null;
		}
		return String.join("；", parts);
	}

	/**
	 * 提取英文例句：blng_sents_part.sentence-pair[0].sentence。
	 */
	private String extractExample(JsonNode root) {
		JsonNode pairs = root.path("blng_sents_part").path("sentence-pair");
		if (pairs.isArray() && pairs.size() > 0) {
			String sentence = text(pairs.get(0), "sentence");
			if (!isBlank(sentence)) {
				return sentence.trim();
			}
		}
		return null;
	}

	/**
	 * 取数组节点第一个元素，非数组或空数组返回 {@code null}。
	 */
	private JsonNode firstWord(JsonNode node) {
		return node.isArray() && node.size() > 0 ? node.get(0) : null;
	}

	private String text(JsonNode node, String field) {
		if (node == null) {
			return null;
		}
		JsonNode value = node.get(field);
		if (value == null || value.isNull()) {
			return null;
		}
		String text = value.asText();
		return text == null || text.isEmpty() ? null : text;
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

}

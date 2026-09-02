package cn.wisestar.server.impl;

import cn.wisestar.server.service.AiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

/**
 * AI 服务实现（TTS 语音 + DALL-E 图片 + GPT 文本）。
 *
 * @author wisestar
 * @date 2026/8/30
 */
@Slf4j
@Service
public class AiServiceImpl implements AiService {

	@Value("${ai.tts.enabled:false}")
	private boolean ttsEnabled;

	@Value("${ai.tts.api-url:}")
	private String ttsApiUrl;

	@Value("${ai.tts.api-key:}")
	private String ttsApiKey;

	@Value("${ai.image.enabled:false}")
	private boolean imageEnabled;

	@Value("${ai.image.api-url:}")
	private String imageUrl;

	@Value("${ai.image.api-key:}")
	private String imageApiKey;

	@Value("${ai.text.enabled:false}")
	private boolean textEnabled;

	@Value("${ai.text.api-url:}")
	private String textApiUrl;

	@Value("${ai.text.api-key:}")
	private String textApiKey;

	private final RestTemplate restTemplate = new RestTemplate();
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public String generateSpeech(String text, String lang) {
		if (!ttsEnabled || ttsApiUrl.isEmpty()) {
			log.warn("TTS 服务未配置，返回空音频 URL");
			return "";
		}

		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.set("Authorization", "Bearer " + ttsApiKey);

			Map<String, Object> body = new HashMap<>();
			body.put("input", text);
			body.put("voice", "alloy");
			body.put("response_format", "mp3");

			HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
			ResponseEntity<Map> response = restTemplate.postForEntity(ttsApiUrl, request, Map.class);

			if (response.getBody() != null && response.getBody().containsKey("url")) {
				return response.getBody().get("url").toString();
			}
		} catch (Exception e) {
			log.error("TTS 生成失败：{}", e.getMessage());
		}

		return "";
	}

	@Override
	public String generateImage(String prompt) {
		if (!imageEnabled || imageUrl.isEmpty()) {
			log.warn("DALL-E 服务未配置，返回空图片 URL");
			return "";
		}

		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.set("Authorization", "Bearer " + imageApiKey);

			Map<String, Object> body = new HashMap<>();
			body.put("prompt", prompt);
			body.put("n", 1);
			body.put("size", "1024x1024");

			HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
			ResponseEntity<Map> response = restTemplate.postForEntity(imageUrl, request, Map.class);

			if (response.getBody() != null) {
				JsonNode data = objectMapper.valueToTree(response.getBody()).get("data");
				if (data != null && data.isArray() && data.size() > 0) {
					JsonNode imgNode = data.get(0);
					if (imgNode.has("url")) {
						return imgNode.get("url").asText();
					} else if (imgNode.has("b64_json")) {
						// 返回 Base64，需要进一步处理
						return "data:image/png;base64," + imgNode.get("b64_json").asText();
					}
				}
			}
		} catch (Exception e) {
			log.error("DALL-E 图片生成失败：{}", e.getMessage());
		}

		return "";
	}

	@Override
	public String generateText(String prompt) {
		if (!textEnabled || textApiUrl.isEmpty()) {
			log.warn("GPT 服务未配置，返回空文本");
			return "";
		}

		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.set("Authorization", "Bearer " + textApiKey);

			Map<String, Object> body = new HashMap<>();
			body.put("model", "gpt-3.5-turbo");
			
			List<Map<String, String>> messages = new ArrayList<>();
			Map<String, String> message = new HashMap<>();
			message.put("role", "user");
			message.put("content", prompt);
			messages.add(message);
			
			body.put("messages", messages);
			body.put("max_tokens", 500);

			HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
			ResponseEntity<Map> response = restTemplate.postForEntity(textApiUrl, request, Map.class);

			if (response.getBody() != null) {
				JsonNode choices = objectMapper.valueToTree(response.getBody()).get("choices");
				if (choices != null && choices.isArray() && choices.size() > 0) {
					return choices.get(0).get("message").get("content").asText();
				}
			}
		} catch (Exception e) {
			log.error("GPT 文本生成失败：{}", e.getMessage());
		}

		return "";
	}

}

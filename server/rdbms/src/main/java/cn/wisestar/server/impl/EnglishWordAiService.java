package cn.wisestar.server.service;

import cn.wisestar.server.domain.dto.english.ImportResult;
import cn.wisestar.server.domain.model.EnglishWord;
import cn.wisestar.server.mapper.EnglishWordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 英语单词 AI 生成服务。
 *
 * @author wisestar
 * @date 2026/8/30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnglishWordAiService {

	private final AiService aiService;
	private final EnglishWordMapper englishWordMapper;

	/**
	 * 为单个单词生成 AI 内容（图片/音频/例句）。
	 */
	public void generateContent(EnglishWord word) {
		// 生成 TTS 音频
		if (word.getAudioUrl() == null || word.getAudioUrl().isEmpty()) {
			String audioUrl = aiService.generateSpeech(word.getSpell(), "en-US");
			if (audioUrl != null && !audioUrl.isEmpty()) {
				word.setAudioUrl(audioUrl);
				log.info("单词 {} 音频生成成功：{}", word.getSpell(), audioUrl);
			}
		}

		// 生成 DALL-E 图片
		if (word.getImageUrl() == null || word.getImageUrl().isEmpty()) {
			String prompt = "A simple, colorful illustration of the word: " + word.getSpell() + 
			               ", meaning: " + word.getMeaning() + 
			               ", cartoon style, white background, suitable for children learning English";
			String imageUrl = aiService.generateImage(prompt);
			if (imageUrl != null && !imageUrl.isEmpty()) {
				word.setImageUrl(imageUrl);
				log.info("单词 {} 图片生成成功：{}", word.getSpell(), imageUrl);
			}
		}

		// 生成例句
		if (word.getExampleSentence() == null || word.getExampleSentence().isEmpty()) {
			String prompt = "Generate a simple English sentence using the word: " + word.getSpell() + 
			               " (meaning: " + word.getMeaning() + "). The sentence should be simple and suitable for children learning English.";
			String example = aiService.generateText(prompt);
			if (example != null && !example.isEmpty()) {
				word.setExampleSentence(example.trim());
				log.info("单词 {} 例句生成成功：{}", word.getSpell(), example);
			}
		}

		// 更新数据库
		if (word.getId() != null) {
			englishWordMapper.updateById(word);
		}
	}

	/**
	 * 批量为单词生成 AI 内容。
	 *
	 * @param wordIds 单词 ID 列表
	 * @return 导入结果（包含成功/失败统计）
	 */
	public ImportResult batchGenerateContent(List<String> wordIds) {
		int total = wordIds.size();
		int success = 0;
		int failed = 0;
		List<String> errors = new ArrayList<>();

		for (String wordId : wordIds) {
			try {
				EnglishWord word = englishWordMapper.selectById(wordId);
				if (word != null) {
					generateContent(word);
					success++;
				} else {
					errors.add("单词 ID 不存在：" + wordId);
					failed++;
				}
			} catch (Exception e) {
				errors.add("单词 ID " + wordId + " 生成失败：" + e.getMessage());
				failed++;
				log.error("AI 生成失败：{}", e.getMessage());
			}
		}

		return new ImportResult(total, success, failed, errors);
	}

	/**
	 * 为指定版本/年级/单元的单词批量生成 AI 内容。
	 *
	 * @param version 版本
	 * @param grade 年级
	 * @param unit 单元
	 * @return 导入结果
	 */
	public ImportResult batchGenerateByCondition(String version, String grade, String unit) {
		List<EnglishWord> words = englishWordMapper.selectList(
			new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<EnglishWord>()
				.eq(version != null && !version.isEmpty(), EnglishWord::getVersion, version)
				.eq(grade != null && !grade.isEmpty(), EnglishWord::getGrade, grade)
				.eq(unit != null && !unit.isEmpty(), EnglishWord::getUnit, unit)
		);

		List<String> wordIds = words.stream()
			.map(EnglishWord::getId)
			.collect(Collectors.toList());

		return batchGenerateContent(wordIds);
	}

}

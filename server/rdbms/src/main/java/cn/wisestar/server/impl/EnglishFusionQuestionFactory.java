package cn.wisestar.server.impl;

import cn.wisestar.server.domain.dto.SurveySchema;
import cn.wisestar.server.domain.model.EnglishGrammar;
import cn.wisestar.server.domain.model.EnglishSentence;
import cn.wisestar.server.domain.model.EnglishWord;
import cn.wisestar.server.mapper.EnglishGrammarMapper;
import cn.wisestar.server.mapper.EnglishSentenceMapper;
import cn.wisestar.server.mapper.EnglishWordMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 英语单元融合题派生器。
 *
 * <p>不依赖管理端重新生成内容包，直接从单元句子、单词、语法例句派生三类题目：</p>
 * <ul>
 *   <li>单词：以单元重点句为题干，挖空句中的核心单词，给出四词词库；</li>
 *   <li>句式：原句短语挖空、换人称改写、生活情景选课本原句；</li>
 *   <li>语法：从本单元语法例句中挖出 am/is/are、do/does、情态动词、冠词、介词等语法词。</li>
 * </ul>
 *
 * <p>所有题目的内容键只依赖单元内稳定 ID 与题型的组合，保证组卷与交卷回读一致。</p>
 *
 * @author wisestar
 * @date 2026/10/18
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EnglishFusionQuestionFactory {

	private static final Pattern WORD_TOKEN = Pattern.compile("[A-Za-z][A-Za-z'’\\-]*");

	private static final Pattern BE = Pattern.compile("\\b(am|is|are|was|were)\\b", Pattern.CASE_INSENSITIVE);

	private static final Pattern MODAL = Pattern.compile("\\b(can|could|should|must|may|will|would)\\b",
			Pattern.CASE_INSENSITIVE);

	private static final Pattern DO = Pattern.compile("\\b(do|does|did)\\b", Pattern.CASE_INSENSITIVE);

	private static final Pattern ARTICLE = Pattern.compile("\\b(a|an|the)\\b", Pattern.CASE_INSENSITIVE);

	private static final Pattern PREPOSITION = Pattern.compile(
			"\\b(next to|behind|under|about|near|from|with|for|in|on|at|by|to)\\b", Pattern.CASE_INSENSITIVE);

	private static final Pattern PRONOUN = Pattern.compile(
			"\\b(i|you|he|she|it|we|they|my|your|his|her|its|our|their|this|that|these|those)\\b",
			Pattern.CASE_INSENSITIVE);

	private static final Pattern PHRASE = Pattern.compile(
			"\\b(?:next to|behind|under|about|near|from|with|for|in|on|at|by|to)\\s+"
					+ "[A-Za-z][A-Za-z'’\\-]*(?:\\s+[A-Za-z][A-Za-z'’\\-]*){0,3}(?=[,.!?;]|$)");

	private static final Set<String> STOPWORDS = new HashSet<>(Arrays.asList(
			"a", "an", "the", "and", "or", "but", "so", "because", "if", "then",
			"is", "am", "are", "was", "were", "be", "do", "does", "did", "can", "could",
			"should", "must", "may", "will", "would", "have", "has", "had",
			"i", "you", "he", "she", "it", "we", "they", "me", "him", "her", "us", "them",
			"my", "your", "his", "its", "our", "their", "this", "that", "these", "those",
			"in", "on", "at", "by", "with", "for", "to", "from", "of", "under", "behind",
			"near", "about", "some", "any", "much", "many", "very", "too", "not", "no", "yes",
			"what", "where", "who", "whose", "how", "why", "when", "here", "there",
			"don't", "doesn't", "didn't", "isn't", "aren't", "can't", "shouldn't", "i'm",
			"let's", "please", "thanks", "thank", "hello", "hi", "good", "nice", "ok", "okay"));

	private static final Set<String> VERBS = new HashSet<>(Arrays.asList(
			"eat", "drink", "have", "like", "love", "want", "need", "go", "come", "play", "read",
			"write", "draw", "sing", "dance", "run", "jump", "swim", "walk", "talk", "speak",
			"watch", "see", "look", "listen", "buy", "take", "bring", "give", "make", "get",
			"put", "open", "close", "stand", "sit", "help", "work", "study", "learn", "teach",
			"live", "stay", "visit", "clean", "wash", "cook", "ride", "drive", "fly", "use",
			"know", "think", "say", "tell", "ask", "answer", "start", "finish", "try", "wear",
			"carry", "catch", "find", "turn", "show", "share", "choose", "send", "wait", "wake"));

	private static final Map<String, String> IRREGULAR_THIRD = Map.of(
			"have", "has", "go", "goes", "do", "does", "be", "is", "say", "says");

	private static final Map<String, String> IRREGULAR_PAST = Map.ofEntries(
			Map.entry("go", "went"), Map.entry("do", "did"), Map.entry("have", "had"),
			Map.entry("eat", "ate"), Map.entry("drink", "drank"), Map.entry("come", "came"),
			Map.entry("see", "saw"), Map.entry("take", "took"), Map.entry("bring", "brought"),
			Map.entry("buy", "bought"), Map.entry("make", "made"), Map.entry("get", "got"),
			Map.entry("give", "gave"), Map.entry("write", "wrote"), Map.entry("read", "read"),
			Map.entry("run", "ran"), Map.entry("swim", "swam"), Map.entry("fly", "flew"),
			Map.entry("say", "said"), Map.entry("tell", "told"), Map.entry("think", "thought"),
			Map.entry("find", "found"), Map.entry("wear", "wore"), Map.entry("ride", "rode"));

	private final EnglishSentenceMapper sentenceMapper;

	private final EnglishWordMapper wordMapper;

	private final EnglishGrammarMapper grammarMapper;

	private final ObjectMapper objectMapper;

	/**
	 * 派生一个单元的三模块题目。
	 */
	public List<DerivedQuestion> build(String version, String grade, String term, String unit) {
		List<EnglishSentence> sentences = loadSentences(version, grade, term, unit);
		if (sentences.isEmpty()) {
			return Collections.emptyList();
		}
		List<EnglishWord> words = loadWords(version, grade, term, unit);
		List<EnglishGrammar> grammars = loadGrammars(grade, term, unit);
		List<DerivedQuestion> result = new ArrayList<>();
		Set<String> keys = new LinkedHashSet<>();
		result.addAll(wordCloze(version, grade, term, unit, sentences, words, keys));
		result.addAll(patternQuestions(version, grade, term, unit, sentences, words, keys));
		result.addAll(grammarQuestions(version, grade, term, unit, grammars, keys));
		return result;
	}

	private List<EnglishSentence> loadSentences(String version, String grade, String term, String unit) {
		return sentenceMapper.selectList(Wrappers.<EnglishSentence>lambdaQuery()
				.eq(StringUtils.hasText(version), EnglishSentence::getVersion, version)
				.eq(StringUtils.hasText(grade), EnglishSentence::getGrade, grade)
				.eq(StringUtils.hasText(term), EnglishSentence::getTerm, term)
				.eq(EnglishSentence::getUnit, unit)
				.orderByAsc(EnglishSentence::getSort)
				.orderByAsc(EnglishSentence::getId));
	}

	private List<EnglishWord> loadWords(String version, String grade, String term, String unit) {
		return wordMapper.selectList(Wrappers.<EnglishWord>lambdaQuery()
				.eq(StringUtils.hasText(version), EnglishWord::getVersion, version)
				.eq(StringUtils.hasText(grade), EnglishWord::getGrade, grade)
				.eq(StringUtils.hasText(term), EnglishWord::getTerm, term)
				.eq(EnglishWord::getUnit, unit)
				.orderByAsc(EnglishWord::getId));
	}

	private List<EnglishGrammar> loadGrammars(String grade, String term, String unit) {
		return grammarMapper.selectList(Wrappers.<EnglishGrammar>lambdaQuery()
				.eq(StringUtils.hasText(grade), EnglishGrammar::getGrade, grade)
				.eq(StringUtils.hasText(term), EnglishGrammar::getTerm, term)
				.eq(EnglishGrammar::getUnit, unit)
				.orderByAsc(EnglishGrammar::getSort)
				.orderByAsc(EnglishGrammar::getId));
	}

	// ============================ 单词：句内选词填空 ============================

	private List<DerivedQuestion> wordCloze(String version, String grade, String term, String unit,
			List<EnglishSentence> sentences, List<EnglishWord> words, Set<String> keys) {
		List<DerivedQuestion> result = new ArrayList<>();
		Map<String, EnglishWord> unitWords = new HashMap<>();
		for (EnglishWord word : words) {
			if (word != null && StringUtils.hasText(word.getSpell())) {
				unitWords.put(word.getSpell().trim().toLowerCase(Locale.ROOT), word);
			}
		}
		Set<String> pool = new LinkedHashSet<>();
		for (EnglishSentence sentence : sentences) {
			for (String part : splitSentences(sentence.getEn())) {
				pool.addAll(contentWords(part));
			}
		}
		unitWords.keySet().forEach(pool::add);

		int index = 0;
		for (EnglishSentence sentence : sentences) {
			for (String part : splitSentences(sentence.getEn())) {
				List<String> candidates = rankedWords(part, unitWords);
				if (candidates.isEmpty()) {
					continue;
				}
				String target = candidates.get(0);
				String key = contentKey("word", version, grade, term, unit, sentence.getId(), target);
				if (!keys.add(key)) {
					continue;
				}
				String blanked = blankOut(part, target);
				if (blanked == null) {
					continue;
				}
				List<String> bank = wordBank(target, pool, unitWords, key);
				if (bank.size() < 4) {
					continue;
				}
				String zh = StringUtils.hasText(sentence.getZh()) ? sentence.getZh().trim() : "根据句意选择单词";
				String title = "选词填空：选择合适的单词补全句子。（" + String.join(" / ", bank) + "）\n"
						+ blanked + " （" + zh + "）";
				result.add(new DerivedQuestion(key, "单词", unit + "·单词", "选词填空",
						"词汇", "理解", difficultyAtLeast(part, "D2"), title, SurveySchema.QuestionType.FillBlank,
						target, null, null, "根据句意和词库选择正确的单词。"));
				index++;
				// 每个句子最多派生两道单词题，避免同一句反复挖空。
				if (index % 2 == 0 && candidates.size() > 1) {
					String second = candidates.get(1);
					String secondKey = contentKey("word", version, grade, term, unit, sentence.getId(), second);
					String secondBlanked = blankOut(part, second);
					if (secondBlanked != null && keys.add(secondKey)) {
						List<String> secondBank = wordBank(second, pool, unitWords, secondKey);
						if (secondBank.size() >= 4) {
							String secondTitle = "选词填空：选择合适的单词补全句子。（" + String.join(" / ", secondBank) + "）\n"
									+ secondBlanked + " （" + zh + "）";
							result.add(new DerivedQuestion(secondKey, "单词", unit + "·单词", "选词填空",
									"词汇", "理解", difficultyAtLeast(part, "D2"), secondTitle,
									SurveySchema.QuestionType.FillBlank, second, null, null,
									"根据句意和词库选择正确的单词。"));
						}
					}
				}
			}
		}
		return result;
	}

	private List<String> rankedWords(String sentence, Map<String, EnglishWord> unitWords) {
		List<String> candidates = contentWords(sentence);
		if (candidates.isEmpty()) {
			return candidates;
		}
		PriorityQueue<String> queue = new PriorityQueue<>((a, b) -> {
			int score = Integer.compare(scoreWord(b, unitWords), scoreWord(a, unitWords));
			return score != 0 ? score : a.compareToIgnoreCase(b);
		});
		queue.addAll(candidates);
		List<String> ranked = new ArrayList<>();
		while (!queue.isEmpty()) {
			ranked.add(queue.poll());
		}
		return ranked.size() > 3 ? ranked.subList(0, 3) : ranked;
	}

	private int scoreWord(String word, Map<String, EnglishWord> unitWords) {
		String lower = word.toLowerCase(Locale.ROOT);
		int score = 0;
		EnglishWord unitWord = unitWords.get(lower);
		if (unitWord == null) {
			for (Map.Entry<String, EnglishWord> entry : unitWords.entrySet()) {
				if (lower.equals(entry.getKey() + "s") || lower.equals(entry.getKey() + "es")) {
					unitWord = entry.getValue();
					break;
				}
			}
		}
		if (unitWord != null) {
			score += 100;
			String meaning = unitWord.getMeaning() == null ? "" : unitWord.getMeaning().trim();
			if (meaning.startsWith("n.")) {
				score += 20;
			}
			else if (meaning.startsWith("v.")) {
				score += 18;
			}
			else if (meaning.startsWith("adj.")) {
				score += 16;
			}
			else if (meaning.startsWith("adv.")) {
				score += 12;
			}
		}
		score += Math.min(word.length(), 10);
		return score;
	}

	private boolean isWordBankChoice(String word) {
		if (!StringUtils.hasText(word) || word.length() < 3) {
			return false;
		}
		// 词库只放实词，缩写（They're / won't）和功能词会破坏选词填空的干扰项质量。
		if (word.contains("'") || word.contains("’") || word.contains("-")) {
			return false;
		}
		return word.matches("[A-Za-z]+") && !STOPWORDS.contains(word.toLowerCase(Locale.ROOT));
	}

	private List<String> wordBank(String target, Set<String> pool, Map<String, EnglishWord> unitWords,
			String key) {
		List<String> candidates = new ArrayList<>();
		for (String word : pool) {
			if (!word.equalsIgnoreCase(target) && isWordBankChoice(word)) {
				candidates.add(word);
			}
		}
		if (candidates.size() < 3) {
			candidates.addAll(unitWords.keySet());
		}
		candidates.removeIf(word -> word.equalsIgnoreCase(target) || !isWordBankChoice(word));
		candidates = new ArrayList<>(new LinkedHashSet<>(candidates));
		Collections.shuffle(candidates, new Random(key.hashCode()));
		List<String> bank = new ArrayList<>();
		bank.add(target);
		for (String word : candidates) {
			if (!bank.contains(word)) {
				bank.add(word);
			}
			if (bank.size() >= 4) {
				break;
			}
		}
		Collections.shuffle(bank, new Random(key.hashCode() * 31L));
		return bank;
	}

	// ============================ 句式：原句挖空 / 换人称 / 情景选句 ============================

	private List<DerivedQuestion> patternQuestions(String version, String grade, String term, String unit,
			List<EnglishSentence> sentences, List<EnglishWord> words, Set<String> keys) {
		List<DerivedQuestion> result = new ArrayList<>();
		Map<String, EnglishWord> unitWords = new HashMap<>();
		for (EnglishWord word : words) {
			if (word != null && StringUtils.hasText(word.getSpell())) {
				unitWords.put(word.getSpell().trim().toLowerCase(Locale.ROOT), word);
			}
		}
		List<String> originals = new ArrayList<>();
		for (EnglishSentence sentence : sentences) {
			originals.addAll(splitSentences(sentence.getEn()));
		}
		for (EnglishSentence sentence : sentences) {
			for (String part : splitSentences(sentence.getEn())) {
				String key = contentKey("pattern-cloze", version, grade, term, unit, sentence.getId(), part);
				if (keys.add(key)) {
					DerivedQuestion cloze = phraseCloze(key, unit, part, sentence.getZh());
					if (cloze != null) {
						result.add(cloze);
					}
				}
				String personKey = contentKey("pattern-person", version, grade, term, unit,
						sentence.getId(), part);
				if (keys.add(personKey)) {
					DerivedQuestion person = personQuestion(personKey, unit, part, unitWords);
					if (person != null) {
						result.add(person);
					}
				}
			}
		}
		int choiceCount = 0;
		for (EnglishSentence sentence : sentences) {
			for (String part : splitSentences(sentence.getEn())) {
				if (choiceCount >= 6) {
					break;
				}
				String key = contentKey("pattern-choose", version, grade, term, unit, sentence.getId(), part);
				if (!keys.add(key)) {
					continue;
				}
				DerivedQuestion choose = sceneChoice(key, unit, part, sentence.getZh(), originals);
				if (choose != null) {
					result.add(choose);
					choiceCount++;
				}
			}
		}
		return result;
	}

	private DerivedQuestion phraseCloze(String key, String unit, String sentence, String zh) {
		String phrase = phraseToBlank(sentence);
		if (!StringUtils.hasText(phrase)) {
			return null;
		}
		String blanked = blankOut(sentence, phrase);
		if (blanked == null) {
			return null;
		}
		String cue = StringUtils.hasText(zh) ? zh.trim() : "补全句子";
		String title = "原句挖空：根据中文提示补全句子。\n" + blanked + "\n（" + cue + "）";
		return new DerivedQuestion(key, "句式", unit + "·句式", "原句挖空",
				"语用", "理解", difficultyAtLeast(sentence, "D2"), title, SurveySchema.QuestionType.FillBlank,
				phrase, null, null, "回看单元原句，注意固定搭配和语序。");
	}

	private String phraseToBlank(String sentence) {
		Matcher phrase = PHRASE.matcher(sentence);
		while (phrase.find()) {
			String value = phrase.group().trim();
			if (value.split("\\s+").length >= 2 && value.split("\\s+").length <= 5) {
				return value;
			}
		}
		// 没有介词短语时挖句尾 1~3 个词，但不能把整句都挖空。
		String[] words = sentence.replaceAll("[.!?]+$", "").trim().split("\\s+");
		for (int size = Math.min(3, words.length - 1); size >= 1; size--) {
			String value = String.join(" ", Arrays.copyOfRange(words, words.length - size, words.length));
			if (!allStopwords(value)) {
				return value;
			}
		}
		return null;
	}

	private DerivedQuestion personQuestion(String key, String unit, String sentence,
			Map<String, EnglishWord> unitWords) {
		if (sentence.length() < 6 || sentence.endsWith("?") || sentence.endsWith("？")) {
			return null;
		}
		String expanded = expandContractions(sentence.trim());
		Matcher matcher = Pattern.compile("^(I|We|You|They|He|She|It)\\s+(.+)$", Pattern.CASE_INSENSITIVE)
				.matcher(expanded);
		if (!matcher.find()) {
			return null;
		}
		String subject = matcher.group(1);
		String rest = matcher.group(2).trim();
		String first = rest.split("\\s+")[0];
		String lowerFirst = first.toLowerCase(Locale.ROOT)
				.replaceAll("[.!?,;:]$", "");
		String targetSubject = isThirdPerson(subject) ? "They" : "He";
		String verb;
		String answer;
		String hint;
		if (Arrays.asList("am", "is", "are", "was", "were").contains(lowerFirst)) {
			answer = isThirdPerson(subject) ? "are" : "is";
			verb = lowerFirst;
			hint = "be 动词要跟着主语变化。";
		}
		else if ("have".equals(lowerFirst) || "has".equals(lowerFirst)) {
			answer = isThirdPerson(subject) ? "have" : "has";
			verb = lowerFirst;
			hint = "主语变成第三人称单数后，have 要变成 has。";
		}
		else if (VERBS.contains(lowerFirst) || isUnitVerb(lowerFirst, unitWords)) {
			verb = lowerFirst;
			answer = isThirdPerson(subject) ? baseForm(verb) : thirdPerson(verb);
			if (isThirdPerson(subject) && answer.equals(verb)) {
				return null;
			}
			hint = "一般现在时中，主语是第三人称单数时动词加 -s/-es。";
		}
		else {
			return null;
		}
		if (!rest.startsWith(first)) {
			return null;
		}
		String afterVerb = rest.substring(first.length()).trim();
		// 复数主语改成单数主语时，若表语是复数名词（friends / twins 等），改写后语义不成立，直接跳过。
		if (("am".equals(lowerFirst) || "are".equals(lowerFirst)) && !afterVerb.isEmpty()) {
			String predicate = afterVerb.split("\\s+")[0].toLowerCase(Locale.ROOT);
			if (predicate.length() > 3 && predicate.endsWith("s")) {
				return null;
			}
		}
		String blanked = targetSubject + " _____" + (afterVerb.isEmpty() ? "" : " " + afterVerb);
		String title = "换人称改写：把原句中的 " + subject + " 换成 " + targetSubject
				+ "，填写正确的动词形式。\n原句：" + sentence.trim() + "\n改写：" + blanked;
		return new DerivedQuestion(key, "句式", unit + "·句式", "换人称改写",
				"语法", "应用", "D2", title, SurveySchema.QuestionType.FillBlank,
				answer, null, null, hint);
	}

	private DerivedQuestion sceneChoice(String key, String unit, String sentence, String zh,
			List<String> originals) {
		List<String> options = new ArrayList<>();
		options.add(sentence);
		for (String other : originals) {
			if (!other.equalsIgnoreCase(sentence) && !options.contains(other)) {
				options.add(other);
			}
			if (options.size() >= 4) {
				break;
			}
		}
		if (options.size() < 2) {
			return null;
		}
		Collections.shuffle(options, new Random(key.hashCode()));
		String cue = StringUtils.hasText(zh) ? zh.trim() : "选出符合情景的句子";
		String title = "情景选原句：想表达“" + cue + "”时，应该用哪句英文？";
		return new DerivedQuestion(key, "句式", unit + "·句式", "情景选原句",
				"语用", "应用", "D1", title, SurveySchema.QuestionType.Radio,
				sentence, null, options, "回到单元原句，找到与中文意思对应的表达。");
	}

	// ============================ 语法：从单元语法例句派生 ============================

	private List<DerivedQuestion> grammarQuestions(String version, String grade, String term, String unit,
			List<EnglishGrammar> grammars, Set<String> keys) {
		List<DerivedQuestion> result = new ArrayList<>();
		if (grammars.isEmpty()) {
			return result;
		}
		for (EnglishGrammar grammar : grammars) {
			List<String> examples = parseExamples(grammar.getExamples());
			int index = 0;
			for (String example : examples) {
				for (String sentence : splitSentences(example)) {
					if (sentence.length() < 4) {
						continue;
					}
					String keyBase = contentKey("grammar", version, grade, term, unit,
							grammar.getId(), String.valueOf(index++));
					for (DerivedQuestion question : grammarCloze(keyBase, unit, grammar.getTitle(), sentence)) {
						if (keys.add(question.key())) {
							result.add(question);
						}
					}
				}
			}
		}
		return result;
	}

	/**
	 * 一句语法例句可安全挖多个语法点，最多派生 3 题，避免「语法」模块只有例句条数那么多题。
	 */
	private List<DerivedQuestion> grammarCloze(String keyBase, String unit, String grammarTitle, String sentence) {
		List<DerivedQuestion> result = new ArrayList<>();
		if (!StringUtils.hasText(grammarTitle)) {
			return result;
		}
		String title = grammarTitle.trim();
		String expanded = expandContractions(sentence);
		String lowerTitle = title.toLowerCase(Locale.ROOT);
		Target beTarget = findTarget(expanded, BE, "be 动词", List.of("am", "is", "are", "was"), keyBase);
		Target doTarget = findTarget(expanded, DO, "助动词", List.of("do", "does", "did", "doing"), keyBase);
		Target modalTarget = findTarget(expanded, MODAL, "情态动词",
				List.of("can", "should", "must", "will"), keyBase);
		Target articleTarget = findTarget(expanded, ARTICLE, "冠词",
				List.of("a", "an", "the", "some"), keyBase);
		Target prepositionTarget = findTarget(expanded, PREPOSITION, "介词",
				List.of("in", "on", "at", "for", "with"), keyBase);
		Target pronounTarget = findTarget(expanded, PRONOUN, "代词",
				List.of("I", "you", "he", "she", "they"), keyBase);
		Target verbTarget = (lowerTitle.contains("祈使") || lowerTitle.contains("一般现在时"))
				? findVerbTarget(expanded, keyBase) : null;

		List<Target> ordered = new ArrayList<>();
		addTarget(ordered, beTarget, lowerTitle.contains("be"));
		addTarget(ordered, modalTarget,
				lowerTitle.contains("情态") || lowerTitle.contains("can") || lowerTitle.contains("should")
						|| lowerTitle.contains("will"));
		addTarget(ordered, doTarget,
				lowerTitle.contains("一般现在时") || lowerTitle.contains("助动词") || lowerTitle.contains("do"));
		addTarget(ordered, articleTarget, lowerTitle.contains("冠词"));
		addTarget(ordered, prepositionTarget,
				lowerTitle.contains("介词") || lowerTitle.contains("方位"));
		addTarget(ordered, pronounTarget,
				lowerTitle.contains("代词") || lowerTitle.contains("物主"));
		addTarget(ordered, verbTarget, lowerTitle.contains("祈使") || lowerTitle.contains("一般现在时"));
		addTarget(ordered, beTarget, false);
		addTarget(ordered, doTarget, false);
		addTarget(ordered, modalTarget, false);
		addTarget(ordered, articleTarget, false);
		addTarget(ordered, prepositionTarget, false);
		addTarget(ordered, pronounTarget, false);

		Set<String> usedTokens = new LinkedHashSet<>();
		int variant = 0;
		for (Target target : ordered) {
			if (variant >= 3) {
				break;
			}
			String token = target.token();
			if (!usedTokens.add(token.toLowerCase(Locale.ROOT))) {
				continue;
			}
			String blanked = blankOut(expanded, token);
			if (blanked == null) {
				continue;
			}
			List<String> options = new ArrayList<>();
			options.add(token.toLowerCase(Locale.ROOT));
			for (String option : target.options()) {
				if (!options.contains(option.toLowerCase(Locale.ROOT))) {
					options.add(option.toLowerCase(Locale.ROOT));
				}
				if (options.size() >= 4) {
					break;
				}
			}
			if (options.size() < 2) {
				continue;
			}
			variant++;
			String key = keyBase + "-" + variant;
			Collections.shuffle(options, new Random(key.hashCode()));
			// 题干隐藏语法点，避免直接提示答案；语法点只在交卷后的答案解析中展示
			String questionTitle = "选词填空，选出合适的单词填入空中。\n" + blanked;
			result.add(new DerivedQuestion(key, "语法", unit + "·语法", "语法变形",
					"语法", "应用", "D2", questionTitle, SurveySchema.QuestionType.Radio,
					token, null, options, "语法点：" + title + "。结合该语法点判断正确形式。"));
		}
		return result;
	}

	private void addTarget(List<Target> targets, Target target, boolean priority) {
		if (target == null) {
			return;
		}
		if (priority) {
			targets.add(0, target);
		}
		else if (!targets.contains(target)) {
			targets.add(target);
		}
	}

	private Target findVerbTarget(String sentence, String key) {
		Matcher matcher = WORD_TOKEN.matcher(sentence);
		while (matcher.find()) {
			String token = matcher.group();
			String lower = token.toLowerCase(Locale.ROOT);
			if (!VERBS.contains(lower) && !VERBS.contains(baseForm(lower))) {
				continue;
			}
			String base = VERBS.contains(lower) ? lower : baseForm(lower);
			List<String> options = new ArrayList<>(List.of(base, thirdPerson(base), gerund(base), past(base)));
			Collections.shuffle(options, new Random(key.hashCode()));
			return new Target(token, "动词形式", options);
		}
		return null;
	}

	private Target findTarget(String sentence, Pattern pattern, String label, List<String> fallbackOptions,
			String key) {
		Matcher matcher = pattern.matcher(sentence);
		if (!matcher.find()) {
			return null;
		}
		String token = matcher.group(1);
		List<String> options = new ArrayList<>(fallbackOptions);
		if (!options.contains(token)) {
			options.add(token);
		}
		return new Target(token, label, options);
	}

	private List<String> parseExamples(String json) {
		List<String> result = new ArrayList<>();
		if (!StringUtils.hasText(json)) {
			return result;
		}
		try {
			JsonNode node = objectMapper.readTree(json);
			if (node.isArray()) {
				for (JsonNode item : node) {
					if (item != null && StringUtils.hasText(item.asText())) {
						result.add(item.asText());
					}
				}
			}
		}
		catch (Exception e) {
			log.debug("english fusion: parse grammar examples failed", e);
		}
		return result;
	}

	// ============================ 通用工具 ============================

	private List<String> splitSentences(String en) {
		if (!StringUtils.hasText(en)) {
			return Collections.emptyList();
		}
		List<String> result = new ArrayList<>();
		for (String part : en.trim().split("\\s+/\\s+")) {
			for (String sentence : part.split("(?<=[.!?])\\s+")) {
				String value = sentence.trim();
				if (StringUtils.hasText(value)) {
					result.add(value);
				}
			}
		}
		return result;
	}

	private List<String> contentWords(String sentence) {
		List<String> result = new ArrayList<>();
		Matcher matcher = WORD_TOKEN.matcher(sentence);
		while (matcher.find()) {
			String token = matcher.group();
			String lower = token.toLowerCase(Locale.ROOT).replace('’', '\'');
			if (lower.length() < 3 || STOPWORDS.contains(lower) || lower.endsWith("'s")) {
				continue;
			}
			if (result.stream().noneMatch(value -> value.equalsIgnoreCase(token))) {
				result.add(token);
			}
		}
		return result;
	}

	private boolean allStopwords(String phrase) {
		for (String token : phrase.split("\\s+")) {
			String lower = token.toLowerCase(Locale.ROOT);
			if (!STOPWORDS.contains(lower) && lower.length() >= 3) {
				return false;
			}
		}
		return true;
	}

	private String blankOut(String sentence, String target) {
		Pattern pattern = Pattern.compile("(?<![A-Za-z])" + Pattern.quote(target) + "(?![A-Za-z])",
				Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(sentence);
		if (!matcher.find()) {
			return null;
		}
		return sentence.substring(0, matcher.start()) + "_____" + sentence.substring(matcher.end());
	}

	private String expandContractions(String sentence) {
		if (!StringUtils.hasText(sentence)) {
			return sentence;
		}
		return sentence
				.replace("I’m", "I am").replace("I'm", "I am")
				.replace("He’s", "He is").replace("He's", "He is")
				.replace("She’s", "She is").replace("She's", "She is")
				.replace("It’s", "It is").replace("It's", "It is")
				.replace("We’re", "We are").replace("We're", "We are")
				.replace("They’re", "They are").replace("They're", "They are")
				.replace("You’re", "You are").replace("You're", "You are")
				.replace("isn't", "is not").replace("isn’t", "is not")
				.replace("aren't", "are not").replace("aren’t", "are not")
				.replace("don't", "do not").replace("don’t", "do not")
				.replace("doesn't", "does not").replace("doesn’t", "does not")
				.replace("can't", "can not").replace("can’t", "can not");
	}

	private boolean isThirdPerson(String subject) {
		return "He".equalsIgnoreCase(subject) || "She".equalsIgnoreCase(subject)
				|| "It".equalsIgnoreCase(subject);
	}

	private boolean isUnitVerb(String token, Map<String, EnglishWord> unitWords) {
		EnglishWord word = unitWords.get(token);
		return word != null && word.getMeaning() != null && word.getMeaning().trim().startsWith("v.");
	}

	private String thirdPerson(String verb) {
		String lower = verb.toLowerCase(Locale.ROOT);
		String irregular = IRREGULAR_THIRD.get(lower);
		if (irregular != null) {
			return irregular;
		}
		if (lower.endsWith("s") || lower.endsWith("x") || lower.endsWith("z")
				|| lower.endsWith("ch") || lower.endsWith("sh") || lower.endsWith("o")) {
			return lower + "es";
		}
		if (lower.endsWith("y") && lower.length() > 1
				&& "aeiou".indexOf(lower.charAt(lower.length() - 2)) < 0) {
			return lower.substring(0, lower.length() - 1) + "ies";
		}
		return lower + "s";
	}

	private String baseForm(String verb) {
		String lower = verb.toLowerCase(Locale.ROOT);
		for (Map.Entry<String, String> entry : IRREGULAR_THIRD.entrySet()) {
			if (entry.getValue().equals(lower)) {
				return entry.getKey();
			}
		}
		if (lower.endsWith("ies") && lower.length() > 3) {
			return lower.substring(0, lower.length() - 3) + "y";
		}
		if (lower.endsWith("es") && lower.length() > 2) {
			String base = lower.substring(0, lower.length() - 2);
			if (base.endsWith("s") || base.endsWith("x") || base.endsWith("z")
					|| base.endsWith("ch") || base.endsWith("sh") || base.endsWith("o")) {
				return base;
			}
		}
		if (lower.endsWith("s") && lower.length() > 1) {
			return lower.substring(0, lower.length() - 1);
		}
		return lower;
	}

	private String gerund(String verb) {
		String lower = verb.toLowerCase(Locale.ROOT);
		if (lower.endsWith("ie")) {
			return lower.substring(0, lower.length() - 2) + "ying";
		}
		if (lower.endsWith("e") && !lower.endsWith("ee")) {
			return lower.substring(0, lower.length() - 1) + "ing";
		}
		return lower + "ing";
	}

	private String past(String verb) {
		String lower = verb.toLowerCase(Locale.ROOT);
		String irregular = IRREGULAR_PAST.get(lower);
		if (irregular != null) {
			return irregular;
		}
		if (lower.endsWith("e")) {
			return lower + "d";
		}
		if (lower.endsWith("y") && lower.length() > 1
				&& "aeiou".indexOf(lower.charAt(lower.length() - 2)) < 0) {
			return lower.substring(0, lower.length() - 1) + "ied";
		}
		return lower + "ed";
	}

	private String difficultyAtLeast(String sentence, String floor) {
		String level = difficulty(sentence);
		return level.compareTo(floor) < 0 ? floor : level;
	}

	private String difficulty(String sentence) {
		int words = sentence == null ? 0 : sentence.trim().split("\\s+").length;
		if (words <= 7) {
			return "D1";
		}
		if (words <= 13) {
			return "D2";
		}
		return "D3";
	}

	private String contentKey(String type, String version, String grade, String term, String unit,
			String sourceId, String extra) {
		return "fusion:" + type + ":" + version + ":" + grade + ":" + term + ":" + unit + ":"
				+ sourceId + ":" + Math.abs(String.valueOf(extra).hashCode());
	}

	/**
	 * 派生题（供组卷服务转换为内部题目对象）。
	 */
	public static class DerivedQuestion {

		private final String key;

		private final String module;

		private final String knowledgePoint;

		private final String questionType;

		private final String languageSkill;

		private final String cognitiveLevel;

		private final String difficulty;

		private final String title;

		private final SurveySchema.QuestionType type;

		private final String answer;

		private final List<String> answers;

		private final List<String> options;

		private final String analysis;

		public DerivedQuestion(String key, String module, String knowledgePoint, String questionType,
				String languageSkill, String cognitiveLevel, String difficulty, String title,
				SurveySchema.QuestionType type, String answer, List<String> answers, List<String> options,
				String analysis) {
			this.key = key;
			this.module = module;
			this.knowledgePoint = knowledgePoint;
			this.questionType = questionType;
			this.languageSkill = languageSkill;
			this.cognitiveLevel = cognitiveLevel;
			this.difficulty = difficulty;
			this.title = title;
			this.type = type;
			this.answer = answer;
			this.answers = answers;
			this.options = options;
			this.analysis = analysis;
		}

		public String key() {
			return key;
		}

		public String module() {
			return module;
		}

		public String knowledgePoint() {
			return knowledgePoint;
		}

		public String questionType() {
			return questionType;
		}

		public String languageSkill() {
			return languageSkill;
		}

		public String cognitiveLevel() {
			return cognitiveLevel;
		}

		public String difficulty() {
			return difficulty;
		}

		public String title() {
			return title;
		}

		public SurveySchema.QuestionType type() {
			return type;
		}

		public String answer() {
			return answer;
		}

		public List<String> answers() {
			return answers;
		}

		public List<String> options() {
			return options;
		}

		public String analysis() {
			return analysis;
		}
	}

	private static class Target {

		private final String token;

		private final String label;

		private final List<String> options;

		Target(String token, String label, List<String> options) {
			this.token = token;
			this.label = label;
			this.options = options;
		}

		String token() {
			return token;
		}

		String label() {
			return label;
		}

		List<String> options() {
			return options;
		}
	}
}

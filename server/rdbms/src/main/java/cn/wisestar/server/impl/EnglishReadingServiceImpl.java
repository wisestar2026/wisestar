package cn.wisestar.server.impl;

import cn.wisestar.server.core.constant.AiPromptKeys;
import cn.wisestar.server.core.uitls.AiEndpointUtils;
import cn.wisestar.server.domain.dto.SystemInfo;
import cn.wisestar.server.domain.dto.english.EnglishReadingView;
import cn.wisestar.server.domain.model.EnglishScenarioPack;
import cn.wisestar.server.domain.model.EnglishSentence;
import cn.wisestar.server.domain.model.EnglishWord;
import cn.wisestar.server.mapper.EnglishScenarioPackMapper;
import cn.wisestar.server.mapper.EnglishSentenceMapper;
import cn.wisestar.server.mapper.EnglishWordMapper;
import cn.wisestar.server.service.EnglishReadingService;
import cn.wisestar.server.service.SystemService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.validation.ValidationException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

/**
 * 英语单元阅读理解服务实现。
 *
 * <p>首次读取某单元时，把单元核心单词与重点句子交给 AI 生成短文和 4 道选择题，
 * 结果缓存进 {@code t_english_scenario_pack.reading_content}，后续直接读取，避免重复调用。</p>
 *
 * @author wisestar
 * @date 2026/10/18
 */
@Slf4j
@Service
public class EnglishReadingServiceImpl implements EnglishReadingService {

	private static final String DEFAULT_MODEL = "deepseek-chat";

	private final SystemService systemService;
	private final EnglishScenarioPackMapper packMapper;
	private final EnglishWordMapper wordMapper;
	private final EnglishSentenceMapper sentenceMapper;
	private final ObjectMapper objectMapper;
	private final RestTemplate restTemplate;

	public EnglishReadingServiceImpl(SystemService systemService, EnglishScenarioPackMapper packMapper,
			EnglishWordMapper wordMapper, EnglishSentenceMapper sentenceMapper) {
		this.systemService = systemService;
		this.packMapper = packMapper;
		this.wordMapper = wordMapper;
		this.sentenceMapper = sentenceMapper;
		this.objectMapper = new ObjectMapper();
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(15_000);
		factory.setReadTimeout(180_000);
		this.restTemplate = new RestTemplate(factory);
	}

	@Override
	public List<EnglishReadingView> getReadings(String version, String grade, String term, String unit) {
		requireText(version, "教材版本");
		requireText(grade, "年级");
		requireText(term, "册别");
		requireText(unit, "单元");
		EnglishScenarioPack pack = packMapper.selectOne(Wrappers.<EnglishScenarioPack>lambdaQuery()
				.eq(EnglishScenarioPack::getVersion, version)
				.eq(EnglishScenarioPack::getGrade, grade)
				.eq(EnglishScenarioPack::getTerm, term)
				.eq(EnglishScenarioPack::getUnit, unit)
				.last("limit 1"));
		if (pack != null && StringUtils.hasText(pack.getReadingContent())) {
			List<EnglishReadingView> cached = parseCached(pack.getReadingContent());
			if (!cached.isEmpty()) {
				return shuffleQuestionOptions(cached);
			}
		}
		EnglishReadingView view = generate(version, grade, term, unit);
		view.setArticleNo(1);
		saveCache(pack, version, grade, term, unit, view);
		List<EnglishReadingView> result = new ArrayList<>();
		result.add(view);
		return shuffleQuestionOptions(result);
	}

	/**
	 * 打乱每道阅读题的选项顺序；AI 与内置素材都把正确项固定放在首位，这里按题目内容稳定乱序。
	 */
	private List<EnglishReadingView> shuffleQuestionOptions(List<EnglishReadingView> articles) {
		for (EnglishReadingView article : articles) {
			for (EnglishReadingView.Question question : article.getQuestions()) {
				String key = "reading:" + article.getTitle() + ":" + question.getStem() + ":"
						+ String.join("|", question.getOptions());
				question.setAnswerIndex(EnglishOptionShuffler.shuffle(question.getOptions(),
						question.getAnswerIndex(), key));
			}
		}
		return articles;
	}

	@Override
	public List<EnglishReadingView> generateArticles(String version, String grade, String term, String unit,
			int count) {
		requireText(version, "教材版本");
		requireText(grade, "年级");
		requireText(term, "册别");
		requireText(unit, "单元");
		int total = Math.max(1, Math.min(5, count));
		List<EnglishReadingView> articles = new ArrayList<>();
		List<String> avoid = new ArrayList<>();
		for (int no = 1; no <= total; no++) {
			EnglishReadingView view = null;
			// 同一单元多篇容易撞车，重复时最多重试 2 次
			for (int attempt = 0; attempt < 3; attempt++) {
				EnglishReadingView candidate = generate(version, grade, term, unit, avoid);
				if (!tooSimilar(candidate, articles)) {
					view = candidate;
					break;
				}
			}
			if (view == null) {
				view = generate(version, grade, term, unit, avoid);
			}
			view.setArticleNo(no);
			if (StringUtils.hasText(view.getTitle())) {
				avoid.add(view.getTitle());
			}
			if (StringUtils.hasText(view.getPassage())) {
				String head = view.getPassage().trim();
				avoid.add(head.length() > 40 ? head.substring(0, 40) + "..." : head);
			}
			// 需求口径：每篇固定 4 道选择题
			if (view.getQuestions() != null && view.getQuestions().size() > 4) {
				view.setQuestions(new ArrayList<>(view.getQuestions().subList(0, 4)));
			}
			articles.add(view);
		}
		EnglishScenarioPack pack = packMapper.selectOne(Wrappers.<EnglishScenarioPack>lambdaQuery()
				.eq(EnglishScenarioPack::getVersion, version)
				.eq(EnglishScenarioPack::getGrade, grade)
				.eq(EnglishScenarioPack::getTerm, term)
				.eq(EnglishScenarioPack::getUnit, unit)
				.last("limit 1"));
		saveArticles(pack, version, grade, term, unit, articles);
		return shuffleQuestionOptions(articles);
	}

	/** 判断新短文与已生成篇目的开头是否过于相似（按前 60 个字符的重合度）。 */
	private boolean tooSimilar(EnglishReadingView candidate, List<EnglishReadingView> existing) {
		if (candidate == null || !StringUtils.hasText(candidate.getPassage())) {
			return true;
		}
		String candidateTitle = normalizeTitle(candidate.getTitle());
		if (StringUtils.hasText(candidateTitle)) {
			for (EnglishReadingView article : existing) {
				if (candidateTitle.equals(normalizeTitle(article.getTitle()))) {
					return true;
				}
			}
		}
		String head = candidate.getPassage().trim().toLowerCase();
		head = head.length() > 60 ? head.substring(0, 60) : head;
		for (EnglishReadingView article : existing) {
			if (!StringUtils.hasText(article.getPassage())) {
				continue;
			}
			String other = article.getPassage().trim().toLowerCase();
			other = other.length() > 60 ? other.substring(0, 60) : other;
			if (head.equals(other) || commonPrefixLength(head, other) >= 30) {
				return true;
			}
		}
		return false;
	}

	/** 标题归一：去空格、标点并转小写，用于判断同单元是否撞标题。 */
	private String normalizeTitle(String title) {
		if (title == null) {
			return "";
		}
		return title.toLowerCase().replaceAll("[^a-z0-9\u4e00-\u9fa5]", "");
	}

	private int commonPrefixLength(String left, String right) {
		int limit = Math.min(left.length(), right.length());
		int i = 0;
		while (i < limit && left.charAt(i) == right.charAt(i)) {
			i++;
		}
		return i;
	}

	/** 整体覆盖写入单元阅读内容（多篇）。 */
	private void saveArticles(EnglishScenarioPack pack, String version, String grade, String term,
			String unit, List<EnglishReadingView> articles) {
		try {
			Map<String, Object> wrapper = new HashMap<>();
			wrapper.put("articles", articles);
			String json = objectMapper.writeValueAsString(wrapper);
			if (pack == null) {
				pack = new EnglishScenarioPack();
				pack.setVersion(version);
				pack.setGrade(grade);
				pack.setTerm(term);
				pack.setUnit(unit);
				pack.setReadingContent(json);
				pack.setWordCount(0);
				pack.setPatternCount(0);
				packMapper.insert(pack);
			}
			else {
				pack.setReadingContent(json);
				packMapper.updateById(pack);
			}
		}
		catch (Exception e) {
			log.warn("english reading batch cache save failed: {}", e.getMessage());
		}
	}


	/** 解析已缓存的阅读内容，兼容单篇对象与 {@code {"articles":[...]}} 两种格式。 */
	private List<EnglishReadingView> parseCached(String json) {
		List<EnglishReadingView> result = new ArrayList<>();
		try {
			JsonNode root = objectMapper.readTree(json);
			JsonNode articles = root.path("articles");
			if (articles.isArray()) {
				for (JsonNode node : articles) {
					result.add(objectMapper.treeToValue(node, EnglishReadingView.class));
				}
			}
			else if (root.isObject()) {
				result.add(objectMapper.treeToValue(root, EnglishReadingView.class));
			}
			result.sort((left, right) -> {
				int a = left.getArticleNo() == null ? Integer.MAX_VALUE : left.getArticleNo();
				int b = right.getArticleNo() == null ? Integer.MAX_VALUE : right.getArticleNo();
				return Integer.compare(a, b);
			});
		}
		catch (Exception e) {
			log.warn("english reading cache parse failed, regenerate: {}", e.getMessage());
			result.clear();
		}
		return result;
	}

	private EnglishReadingView generate(String version, String grade, String term, String unit) {
		return generate(version, grade, term, unit, new ArrayList<>());
	}

	/**
	 * 生成一篇阅读短文：同一单元最多尝试 3 次，按「词数、题数、词汇覆盖、抗拼句」四项硬校验。
	 *
	 * <p>AI（尤其是小模型）容易把单元词表与句式直接逐句拼接成文，读起来像句子大杂烩：
	 * 主线跳来跳去、同一篇里出现多个叙述者。这里接受的条件是：词数在年级区间内、
	 * 题目至少 4 道、短文命中本单元单词不少于 6 个、且逐字复用的单元原句不超过 1 句。多次不达标时取「硬性项达标、词数最接近」的一版，避免整单元失败。</p>
	 *
	 * @param avoid 已生成篇目的标题与开头句子，用于提示 AI 换一个主角/场景，避免多篇高度雷同
	 */
	private EnglishReadingView generate(String version, String grade, String term, String unit,
			List<String> avoid) {
		List<EnglishWord> words = wordMapper.selectList(Wrappers.<EnglishWord>lambdaQuery()
				.eq(EnglishWord::getVersion, version)
				.eq(EnglishWord::getGrade, grade)
				.eq(EnglishWord::getTerm, term)
				.eq(EnglishWord::getUnit, unit)
				.orderByAsc(EnglishWord::getSpell)
				.last("LIMIT 40"));
		List<EnglishSentence> sentences = sentenceMapper.selectList(Wrappers.<EnglishSentence>lambdaQuery()
				.eq(EnglishSentence::getVersion, version)
				.eq(EnglishSentence::getGrade, grade)
				.eq(EnglishSentence::getTerm, term)
				.eq(EnglishSentence::getUnit, unit)
				.orderByAsc(EnglishSentence::getSort)
				.last("LIMIT 30"));
		if (words.isEmpty() && sentences.isEmpty()) {
			throw new ValidationException("本单元暂无阅读素材，请先完善单元单词与句子");
		}
		int target = passageWordTarget(grade);
		int min = passageWordMin(grade);
		int max = passageWordMax(grade);
		int minHits = passageMinWordHits(grade);
		EnglishReadingView best = null;
		int bestScore = Integer.MAX_VALUE;
		for (int attempt = 0; attempt < 3; attempt += 1) {
			EnglishReadingView candidate = generateOnce(grade, unit, words, sentences, avoid);
			int wordTotal = countWords(candidate.getPassage());
			int questions = candidate.getQuestions() == null ? 0 : candidate.getQuestions().size();
			int hits = countUnitWordHits(candidate.getPassage(), words);
			int stitched = countVerbatimSentences(candidate.getPassage(), sentences);
			boolean lengthOk = wordTotal >= min && wordTotal <= max;
			boolean structureOk = questions >= 4 && hits >= minHits && stitched <= 1;
			if (lengthOk && structureOk) {
				return candidate;
			}
			int score = (lengthOk ? 0 : 1000) + (questions >= 4 ? 0 : 2000) + (hits >= minHits ? 0 : 1500)
					+ stitched * 200 + Math.abs(wordTotal - target);
			if (score < bestScore) {
				bestScore = score;
				best = candidate;
			}
			log.info("english reading rejected: {} words / {} questions / {} unit-words / {} stitched"
					+ " (target {} words, min {} hits), retry {}/3", wordTotal, questions, hits, stitched,
					target, minHits, attempt + 1);
		}
		return best;
	}

	/** 统计英文短文的单词数（按空白切分）。 */
	private int countWords(String passage) {
		if (!StringUtils.hasText(passage)) {
			return 0;
		}
		return passage.trim().split("\\s+").length;
	}

	/** 短文至少命中多少个本单元单词：初中 6 个，小学 5 个。 */
	private int passageMinWordHits(String grade) {
		String value = grade == null ? "" : grade.trim();
		return value.contains("初") || value.contains("七") || value.contains("八") || value.contains("九")
				? 6 : 5;
	}

	/** 统计短文覆盖的本单元单词数（大小写不敏感，允许复数/时态词形）。 */
	private int countUnitWordHits(String passage, List<EnglishWord> words) {
		Set<String> tokens = new HashSet<>();
		for (String token : passage.toLowerCase().split("[^a-z']+")) {
			if (!token.isEmpty()) {
				tokens.add(token);
			}
		}
		int hits = 0;
		for (EnglishWord word : words) {
			String spell = word.getSpell() == null ? "" : word.getSpell().trim().toLowerCase();
			if (spell.isEmpty() || spell.contains(" ")) {
				continue;
			}
			if (tokens.contains(spell) || tokens.contains(stemOf(spell))) {
				hits += 1;
			}
		}
		return hits;
	}

	/** 去掉常见词形后缀，用于「read/reads/reading」这类词形对齐。 */
	private String stemOf(String word) {
		String value = word;
		for (String suffix : new String[] { "ies", "es", "s", "ing", "ed" }) {
			if (value.endsWith(suffix) && value.length() - suffix.length() >= 3) {
				value = value.substring(0, value.length() - suffix.length());
				break;
			}
		}
		return value;
	}

	/** 统计短文里逐字复用单元原句的句数（一句原句最多算一次）。 */
	private int countVerbatimSentences(String passage, List<EnglishSentence> sentences) {
		String normalized = normalizeSentence(passage);
		int count = 0;
		for (EnglishSentence sentence : sentences) {
			String en = sentence.getEn();
			if (!StringUtils.hasText(en)) {
				continue;
			}
			String target = normalizeSentence(en);
			if (target.length() >= 12 && normalized.contains(target)) {
				count += 1;
			}
		}
		return count;
	}

	/** 归一英文句子：小写、去标点、压缩空白，用于逐字比对。 */
	private String normalizeSentence(String text) {
		if (text == null) {
			return "";
		}
		return text.toLowerCase().replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
	}

	private EnglishReadingView generateOnce(String grade, String unit, List<EnglishWord> words,
			List<EnglishSentence> sentences, List<String> avoid) {
		String content = postChat(buildUserPrompt(grade, unit, words, sentences, avoid), 0.6);
		return normalizeOrThrow(content);
	}

	/**
	 * 调一次大模型并原样返回文本内容。
	 */
	private String postChat(String userPrompt, double temperature) {
		SystemInfo.AiSetting aiSetting = systemService.getSystemAiSetting();
		if (aiSetting == null || aiSetting.getEnabled() == null || !aiSetting.getEnabled()) {
			throw new ValidationException("AI 服务未启用：请先在系统设置中开启 AI");
		}
		String token = aiSetting.getToken();
		if (!StringUtils.hasText(token)) {
			throw new ValidationException("AI Token 未配置：请先在系统设置中填写 AI 平台 Token");
		}
		String model = (aiSetting.getModels() != null && !aiSetting.getModels().isEmpty())
				? aiSetting.getModels().get(0) : DEFAULT_MODEL;
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.setBearerAuth(token.trim());
			List<Map<String, String>> messages = new ArrayList<>();
			messages.add(message("system", AiPromptKeys.resolve(aiSetting, AiPromptKeys.EN_READING, null)));
			messages.add(message("user", userPrompt));
			Map<String, Object> body = new HashMap<>();
			body.put("model", model);
			body.put("messages", messages);
			body.put("temperature", temperature);
			body.put("max_tokens", 8192);
			body.put("stream", false);
			JsonNode resp = objectMapper.readTree(restTemplate.postForObject(
					AiEndpointUtils.resolveChatUrl(aiSetting.getBaseUrl()),
					new HttpEntity<>(body, headers), String.class));
			JsonNode choices = resp.path("choices");
			if (choices.isArray() && choices.size() > 0) {
				String content = choices.get(0).path("message").path("content").asText("");
				if (StringUtils.hasText(content)) {
					return content;
				}
			}
			throw new ValidationException("AI 返回内容为空，请稍后重试");
		}
		catch (ValidationException e) {
			throw e;
		}
		catch (Exception e) {
			log.error("english reading generate failed: {}", e.getMessage(), e);
			throw new ValidationException("阅读理解生成失败：" + e.getMessage());
		}
	}

	private EnglishReadingView normalizeOrThrow(String raw) {
		try {
			return normalize(raw);
		}
		catch (ValidationException e) {
			throw e;
		}
		catch (Exception e) {
			throw new ValidationException("阅读理解解析失败：" + e.getMessage());
		}
	}

	private String buildUserPrompt(String grade, String unit, List<EnglishWord> words,
			List<EnglishSentence> sentences, List<String> avoid) {
		StringBuilder sb = new StringBuilder();
		sb.append("请为下面单元编写一篇英语短文和 4 道阅读理解选择题：\n");
		sb.append("年级：").append(grade).append("\n");
		sb.append("单元：").append(unit).append("\n");
		sb.append("核心单词：\n");
		for (EnglishWord word : words) {
			if (StringUtils.hasText(word.getSpell())) {
				sb.append("- ").append(word.getSpell());
				if (StringUtils.hasText(word.getMeaning())) {
					sb.append("（").append(cleanMeaning(word.getMeaning())).append("）");
				}
				sb.append("\n");
			}
		}
		sb.append("重点句式（仅作句式结构参考，内容必须按故事改写，不要整句照抄）：\n");
		for (EnglishSentence sentence : sentences) {
			if (StringUtils.hasText(sentence.getEn())) {
				sb.append("- ").append(sentence.getEn());
				if (StringUtils.hasText(sentence.getZh())) {
					sb.append("（").append(sentence.getZh()).append("）");
				}
				sb.append("\n");
			}
		}
		if (avoid != null && !avoid.isEmpty()) {
			sb.append("\n本单元已有以下短文，必须换一个主角姓名、场景与叙述角度，禁止复用这些标题或开头：\n");
			for (String item : avoid) {
				sb.append("- ").append(item).append("\n");
			}
			sb.append("尤其禁止再用 Good morning, everyone! 这类相同的开场句，"
					+ "每篇的第一句必须换成不同的叙述起点（如时间、天气、人物动作或心理描写）。\n");
		}
		sb.append("\n【写作硬性要求】\n");
		sb.append("1) 写成一篇有完整主线的记叙文或说明文：开头交代时间/地点/人物，中间按一条线发展，"
				+ "结尾收束；全篇只允许一个叙述者、一个主要场景，不得中途换主角或跳话题。\n");
		sb.append("2) 严禁把上面的单词和句子逐条拼成一段话：原句逐字照抄最多 1 句，"
				+ "其余句子必须按故事情节自己组织；单词要融进情节里自然出现，不要为了用词而硬塞。\n");
		sb.append("2.1) 先想清楚故事三要素再写：主角是谁、在哪、要完成什么事；"
				+ "与情节无关的事物（例如赛跑里出现冲浪板、水下憋气）一律不写。\n");
		sb.append("3) 不要写问答对白式的排比句，也不要堆「Both.../None.../Either...」这类语法操练句。\n");
		sb.append("4) 短文必须用到上面核心单词中至少 6 个，并至少体现 2 个重点句式的语法结构"
				+ "（改写主语、时态或内容都算，逐字照抄整句不超过 1 句）。\n");
		sb.append("\n【出题要求】\n");
		sb.append("1) 4 道题全部是细节理解或主旨题，正确答案必须在原文中有明确的一处依据；"
				+ "凡是问原因/感受的题，原文必须直接写出原因或感受，不得让读者猜。\n");
		sb.append("2) 每题 4 个选项、只有 1 个正确；干扰项必须是原文没提到或与原文相反的内容，"
				+ "不得出现「All of the above」「None of the above」或两个都说得通的选项。\n");
		sb.append("3) 选项文本不要带 A./B./C./D. 前缀，answerIndex 指向正确项下标，answer 字段必须与正确选项逐字相同；每题必须恰好 4 个互不相同的具体选项。\n");
		sb.append("4) 题干和选项只用该年级常见词汇，避免 narrator、highlight、tradition、unity、gymnasium、auditorium、prize-winning 这类抽象或超纲词，改用 student、main idea、the best title、activity 等常用表达。\n");
		sb.append("5) 问 favourite、most important、most exciting 这类题时，原文必须明确比较或直接点明；没有比较就改成细节题。\n");
		sb.append("6) 短文的时间顺序必须自洽：早上到校不能先吃午饭再上第一节课，日落时不能才开始第一节课；"
				+ "地点也要一致，不能在食堂场景里问教室位置。\n");
		sb.append("7) 出题前先定答案：正确答案对应的事实、感受或地点在原文只能出现一处且表述唯一；"
				+ "原文不得同时写出两种都能当答案的信息（例如既 excited 又 nervous、既去教室又去图书馆），"
				+ "否则换成细节题或改写原文。\n");
		sb.append("8) 英文正文和题干里禁止出现任何中文（中文只能写在 passageZh 与 explanation 中）。\n");
		sb.append("\n篇幅要求：全文英文单词数必须落在 ").append(passageWordMin(grade)).append("~")
				.append(passageWordMax(grade))
				.append(" 词之间，少于 ").append(passageWordMin(grade))
				.append(" 词视为不合格；不少于 6 句，可分 2~3 段，词汇与语法符合该年级水平。");
		return sb.toString();
	}

	/** 短文目标词数：七年级 120、八年级 145、九年级 180，小学 120。 */
	private int passageWordTarget(String grade) {
		String value = grade == null ? "" : grade.trim();
		if (value.isEmpty()) {
			return 120;
		}
		if (value.contains("九") || value.contains("Grade 9") || value.contains("grade 9")) {
			return 180;
		}
		if (value.contains("八") || value.contains("Grade 8") || value.contains("grade 8")) {
			return 145;
		}
		if (value.contains("初") || value.contains("高") || value.contains("七")
				|| value.contains("Grade 7") || value.contains("grade 7")) {
			return 120;
		}
		return 120;
	}

	/** 短文允许的最少词数（目标的 70%）。 */
	private int passageWordMin(String grade) {
		return (int) Math.round(passageWordTarget(grade) * 0.7);
	}

	/** 短文允许的最多词数（目标的 160%）。 */
	private int passageWordMax(String grade) {
		return (int) Math.round(passageWordTarget(grade) * 1.6);
	}


	private static final java.util.regex.Pattern MEANING_POS = java.util.regex.Pattern
			.compile("(?i)^\\s*(?:v|n|adj|adv|prep|pron|conj|num|art|phr|abbr|aux|interj)\\.\\s*");
	private static final java.util.regex.Pattern HAS_CJK =
			java.util.regex.Pattern.compile("[\\u4e00-\\u9fa5]");
	private static final java.util.regex.Pattern NARRATOR =
			java.util.regex.Pattern.compile("(?i)\\bnarrators?\\b");
	private static final java.util.regex.Pattern GYMNASIUM =
			java.util.regex.Pattern.compile("(?i)\\bgymnasium(s)?\\b");
	private static final java.util.regex.Pattern AUDITORIUM =
			java.util.regex.Pattern.compile("(?i)\\bauditorium(s)?\\b");

	/** 词库释义只取第一义项并去掉词性前缀，避免提示词里出现「v. 批评，抨击；袭击」。 */
	private String cleanMeaning(String meaning) {
		if (meaning == null) {
			return "";
		}
		String value = meaning.trim();
		int cut = value.indexOf('；');
		if (cut < 0) {
			cut = value.indexOf(';');
		}
		if (cut > 0) {
			value = value.substring(0, cut);
		}
		return MEANING_POS.matcher(value).replaceAll("").trim();
	}

	private EnglishReadingView normalize(String raw) throws Exception {
		String text = raw.trim();
		if (text.contains("```")) {
			int first = text.indexOf("```");
			int last = text.lastIndexOf("```");
			if (last > first) {
				text = text.substring(first + 3, last).trim();
			}
			int newline = text.indexOf('\n');
			if (newline > 0 && text.substring(0, newline).trim().toLowerCase().startsWith("json")) {
				text = text.substring(newline + 1).trim();
			}
		}
		int start = text.indexOf('{');
		int end = text.lastIndexOf('}');
		if (start >= 0 && end > start) {
			text = text.substring(start, end + 1);
		}
		JsonNode root = objectMapper.readTree(text);
		EnglishReadingView view = new EnglishReadingView();
		view.setTitle(text(root, "title"));
		view.setPassage(text(root, "passage", "text", "content"));
		view.setPassageZh(text(root, "passageZh", "translation", "zh"));
		if (!StringUtils.hasText(view.getPassage())) {
			throw new ValidationException("AI 未返回短文内容，请重试");
		}
		if (HAS_CJK.matcher(view.getPassage()).find()) {
			throw new ValidationException("AI 短文混入中文，请重试");
		}
		JsonNode questions = firstNode(root, "questions", "items", "exercises");
		if (questions != null && questions.isArray()) {
			for (JsonNode node : questions) {
				EnglishReadingView.Question question = new EnglishReadingView.Question();
				question.setStem(text(node, "stem", "question", "q", "title"));
				question.setExplanation(text(node, "explanation", "analysis"));
				JsonNode options = firstNode(node, "options", "choices");
				if (options != null && options.isArray()) {
					for (JsonNode option : options) {
						String value = option.isValueNode() ? option.asText("")
								: text(option, "text", "label", "value");
						value = stripOptionPrefix(value);
						if (StringUtils.hasText(value)) {
							question.getOptions().add(value.trim());
						}
					}
				}
				// 答案文本与选项文本逐字比对优先，比对不上才回退模型给的下标，避免下标错位
				String answer = text(node, "answer", "correct");
				int textIndex = indexOfAnswer(answer, question.getOptions());
				JsonNode answerIndex = node.get("answerIndex");
				if (textIndex >= 0) {
					question.setAnswerIndex(textIndex);
				}
				else if (answerIndex != null && answerIndex.isNumber()) {
					question.setAnswerIndex(answerIndex.asInt());
				}
				else {
					question.setAnswerIndex(-1);
				}
				boolean metaOption = question.getOptions().stream()
						.anyMatch(option -> option.matches("(?i)^(all|none) of (the )?above.*")
								|| option.matches("(?i)^both [a-d] and [a-d].*"));
				if (metaOption || hasDuplicateOption(question.getOptions())
						|| hasCjk(question.getStem())
						|| question.getOptions().stream().anyMatch(this::hasCjk)) {
					continue;
				}
				if (question.getOptions().size() == 4 && question.getAnswerIndex() != null
						&& question.getAnswerIndex() >= 0
						&& question.getAnswerIndex() < question.getOptions().size()
						&& StringUtils.hasText(question.getStem())) {
					view.getQuestions().add(question);
				}
			}
		}
		if (view.getQuestions().size() < 3) {
			throw new ValidationException("AI 返回的阅读题不足，请重试");
		}
		sanitizeWording(view);
		return view;
	}

	/**
	 * 把模型中反复出现的超纲表达替换成同义常用词，题干与原文同步，避免选项与正文对不上。
	 */
	private void sanitizeWording(EnglishReadingView view) {
		view.setTitle(replaceOutOfSyllabus(view.getTitle()));
		view.setPassage(replaceOutOfSyllabus(view.getPassage()));
		for (EnglishReadingView.Question question : view.getQuestions()) {
			question.setStem(replaceOutOfSyllabus(question.getStem()));
			question.setExplanation(replaceOutOfSyllabus(question.getExplanation()));
			List<String> options = new ArrayList<>();
			for (String option : question.getOptions()) {
				options.add(replaceOutOfSyllabus(option));
			}
			question.setOptions(options);
		}
	}

	private boolean hasCjk(String value) {
		return value != null && HAS_CJK.matcher(value).find();
	}

	private String replaceOutOfSyllabus(String value) {
		if (value == null || value.isEmpty()) {
			return value;
		}
		String result = NARRATOR.matcher(value).replaceAll("writer");
		result = GYMNASIUM.matcher(result).replaceAll("sports hall");
		return AUDITORIUM.matcher(result).replaceAll("school hall");
	}

	private boolean hasDuplicateOption(List<String> options) {
		Set<String> seen = new HashSet<>();
		for (String option : options) {
			String key = option.trim().toLowerCase();
			if (!seen.add(key)) {
				return true;
			}
		}
		return false;
	}

	/** 去掉选项开头的字母序号前缀（AI 常写成「A. xxx」「b) xxx」），避免学员端出现双重字母。 */
	private String stripOptionPrefix(String value) {
		if (value == null) {
			return "";
		}
		String text = value.trim();
		java.util.regex.Matcher matcher = java.util.regex.Pattern
				.compile("^[A-Da-d]\\s*[.、．:：)\\]]\\s*").matcher(text);
		return matcher.find() ? text.substring(matcher.end()).trim() : text;
	}

	private int indexOfAnswer(String answer, List<String> options) {
		if (!StringUtils.hasText(answer)) {
			return -1;
		}
		String value = answer.trim();
		String normalized = normalizeAnswerText(value);
		for (int i = 0; i < options.size(); i++) {
			if (options.get(i).trim().equalsIgnoreCase(value)) {
				return i;
			}
		}
		for (int i = 0; i < options.size(); i++) {
			if (normalizeAnswerText(options.get(i)).equals(normalized)) {
				return i;
			}
		}
		if (value.length() == 1) {
			char letter = Character.toUpperCase(value.charAt(0));
			if (letter >= 'A' && letter <= 'D') {
				return letter - 'A';
			}
		}
		return -1;
	}

	/** 去掉首尾标点并压空格，用于容忍模型抄写答案时的细微差异。 */
	private String normalizeAnswerText(String value) {
		return value == null ? "" : value.trim().toLowerCase()
				.replaceAll("[\\s]+", " ")
				.replaceAll("^[\"'“”‘’]+|[\"'“”‘’]+$", "")
				.replaceAll("[.。!！?？]+$", "")
				.trim();
	}

	private void saveCache(EnglishScenarioPack pack, String version, String grade, String term,
			String unit, EnglishReadingView view) {
		try {
			Map<String, Object> wrapper = new HashMap<>();
			wrapper.put("articles", java.util.Collections.singletonList(view));
			String json = objectMapper.writeValueAsString(wrapper);
			if (pack == null) {
				pack = new EnglishScenarioPack();
				pack.setVersion(version);
				pack.setGrade(grade);
				pack.setTerm(term);
				pack.setUnit(unit);
				pack.setReadingContent(json);
				pack.setWordCount(0);
				pack.setPatternCount(0);
				packMapper.insert(pack);
			}
			else {
				pack.setReadingContent(json);
				packMapper.updateById(pack);
			}
		}
		catch (Exception e) {
			log.warn("english reading cache save failed: {}", e.getMessage());
		}
	}

	private Map<String, String> message(String role, String content) {
		Map<String, String> message = new HashMap<>();
		message.put("role", role);
		message.put("content", content);
		return message;
	}

	private JsonNode firstNode(JsonNode root, String... keys) {
		for (String key : keys) {
			JsonNode node = root.get(key);
			if (node != null && !node.isNull()) {
				return node;
			}
		}
		return null;
	}

	private String text(JsonNode node, String... keys) {
		JsonNode value = firstNode(node, keys);
		return value == null ? null : value.asText("");
	}

	private void requireText(String value, String name) {
		if (!StringUtils.hasText(value)) {
			throw new ValidationException(name + "不能为空");
		}
	}

}

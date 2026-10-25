package cn.wisestar.server.impl;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.domain.dto.english.EnglishWordQuery;
import cn.wisestar.server.domain.dto.english.EnglishWordView;
import cn.wisestar.server.domain.model.EnglishLearningLog;
import cn.wisestar.server.domain.model.EnglishWord;
import cn.wisestar.server.domain.model.EnglishWordBook;
import cn.wisestar.server.mapper.EnglishLearningLogMapper;
import cn.wisestar.server.mapper.EnglishWordMapper;
import cn.wisestar.server.mapper.EnglishWordBookMapper;
import cn.wisestar.server.service.EnglishTtsService;
import cn.wisestar.server.service.EnglishWordService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 英语单词学习服务实现。
 *
 * @author wisestar
 * @date 2026/8/30
 */
@Service
@RequiredArgsConstructor
public class EnglishWordServiceImpl implements EnglishWordService {

	private final EnglishWordMapper englishWordMapper;
	private final EnglishWordBookMapper englishWordBookMapper;
	private final EnglishLearningLogMapper englishLearningLogMapper;
	private final EnglishPracticeEvaluator englishPracticeEvaluator;
	private final EnglishTtsService englishTtsService;

	/** 需加强阈值：累计「不认识」达到该次数的单词标记为需加强。 */
	private static final int WEAK_WRONG_TIMES = 2;

	/** 发音预热上限：返回词数超过该值（整册）时不做后台合成，避免拖慢双核机器。 */
	private static final int WARMUP_MAX_WORDS = 60;

	/** 巩固池上限：熟练度达到该值视为已熟悉，不再强制巩固。 */
	private static final int DRILL_FAMILIARITY_CEILING = 3;

	/** 上报来源：学习环节 */
	private static final String SOURCE_LEARN = "learn";

	/** 上报来源：单词练习 */
	private static final String SOURCE_PRACTICE = "practice";

	/** 上报来源：进入学习前的强制巩固 */
	private static final String SOURCE_DRILL = "drill";

	@Override
	public PaginationResponse<EnglishWordView> listWords(EnglishWordQuery query) {
		LambdaQueryWrapper<EnglishWord> wrapper = Wrappers.<EnglishWord>lambdaQuery()
				.eq(query.getVersion() != null, EnglishWord::getVersion, query.getVersion())
				.eq(query.getGrade() != null, EnglishWord::getGrade, query.getGrade())
				.eq(query.getTerm() != null, EnglishWord::getTerm, query.getTerm())
				.eq(query.getUnit() != null, EnglishWord::getUnit, query.getUnit())
				.eq(query.getSection() != null, EnglishWord::getSection, query.getSection())
				.eq(query.getLevel() != null, EnglishWord::getLevel, query.getLevel())
				.like(query.getSpell() != null, EnglishWord::getSpell, query.getSpell())
				.orderByAsc(EnglishWord::getGrade)
				.orderByAsc(EnglishWord::getTerm)
				.orderByAsc(EnglishWord::getUnit)
				.orderByAsc(EnglishWord::getSection)
				.orderByAsc(EnglishWord::getSpell);

		Page<EnglishWord> page = new Page<>(query.getCurrent(), query.getPageSize());
		Page<EnglishWord> result = englishWordMapper.selectPage(page, wrapper);

		List<EnglishWordView> views = result.getRecords().stream()
				.map(this::toView)
				.collect(Collectors.toList());

		return new PaginationResponse<>(result.getTotal(), views);
	}

	@Override
	public PaginationResponse<EnglishWordView> wordBook(String userId, EnglishWordQuery query) {
		LambdaQueryWrapper<EnglishWord> wrapper = Wrappers.<EnglishWord>lambdaQuery()
				.eq(query.getVersion() != null, EnglishWord::getVersion, query.getVersion())
				.eq(query.getGrade() != null, EnglishWord::getGrade, query.getGrade())
				.eq(query.getTerm() != null, EnglishWord::getTerm, query.getTerm())
				.eq(query.getUnit() != null, EnglishWord::getUnit, query.getUnit())
				.eq(query.getSection() != null, EnglishWord::getSection, query.getSection())
				.eq(query.getLevel() != null, EnglishWord::getLevel, query.getLevel())
				.like(query.getSpell() != null, EnglishWord::getSpell, query.getSpell())
				.orderByAsc(EnglishWord::getGrade)
				.orderByAsc(EnglishWord::getTerm)
				.orderByAsc(EnglishWord::getUnit)
				.orderByAsc(EnglishWord::getSection)
				.orderByAsc(EnglishWord::getSpell);

		Page<EnglishWord> page = new Page<>(query.getCurrent(), query.getPageSize());
		Page<EnglishWord> result = englishWordMapper.selectPage(page, wrapper);

		List<EnglishWordView> views = result.getRecords().stream()
				.map(this::toView)
				.collect(Collectors.toList());
		fillFamiliarity(userId, views);

		// 学员端翻到哪页就按该页语速预热哪页的单词发音：Piper 单句首次合成要 1~2 秒，预热后点击即响。
		// 整册（复习页取干扰项池，几百词）不预热，否则双核机器会被后台合成拖住。
		if (userId != null && !views.isEmpty() && views.size() <= WARMUP_MAX_WORDS) {
			englishTtsService.warmUp(views.stream().map(EnglishWordView::getSpell).collect(Collectors.toList()),
					query.getSpeed());
		}

		return new PaginationResponse<>(result.getTotal(), views);
	}

	/**
	 * 为单词视图回填当前学员在单词本中的熟练度与需加强标记（未学习默认 0）。
	 */
	private void fillFamiliarity(String userId, List<EnglishWordView> views) {
		if (userId == null || views.isEmpty()) {
			return;
		}
		List<String> wordIds = views.stream().map(EnglishWordView::getId).collect(Collectors.toList());
		java.util.Map<String, EnglishWordBook> bookByWord = englishWordBookMapper.selectList(
						Wrappers.<EnglishWordBook>lambdaQuery()
								.eq(EnglishWordBook::getUserId, userId)
								.in(EnglishWordBook::getWordId, wordIds))
				.stream()
				.collect(Collectors.toMap(EnglishWordBook::getWordId, b -> b, (a, b) -> b));
		views.forEach(v -> {
			EnglishWordBook book = bookByWord.get(v.getId());
			v.setFamiliarity(book == null || book.getFamiliarity() == null ? 0 : book.getFamiliarity());
			v.setCorrectCount(book == null || book.getCorrectCount() == null ? 0 : book.getCorrectCount());
			v.setWrongCount(book == null || book.getWrongCount() == null ? 0 : book.getWrongCount());
			v.setWeak(v.getWrongCount() >= WEAK_WRONG_TIMES);
			v.setNextReviewTime(book == null ? null : book.getNextReviewTime());
			v.setQuizTotal(EnglishMastery.intValue(book == null ? null : book.getQuizTotal()));
			v.setQuizFirstCorrect(EnglishMastery.intValue(book == null ? null : book.getQuizFirstCorrect()));
			v.setFirstCorrectRate(EnglishMastery.firstCorrectRate(book));
			int masteryLevel = EnglishMastery.level(book);
			v.setMasteryLevel(masteryLevel);
			v.setMasteryLabel(EnglishMastery.label(masteryLevel));
		});
	}

	@Override
	public List<EnglishWordView> getStudyWords(String userId, int limit) {
		// 查询用户单词本
		List<EnglishWordBook> wordBooks = englishWordBookMapper.selectList(
				Wrappers.<EnglishWordBook>lambdaQuery()
						.eq(EnglishWordBook::getUserId, userId)
						.le(EnglishWordBook::getNextReviewTime, new java.util.Date())
						.orderByAsc(EnglishWordBook::getNextReviewTime)
						.last("LIMIT " + limit));

		List<EnglishWordView> views;
		if (wordBooks.isEmpty()) {
			// 无待复习单词，返回最新入库的新单词
			views = englishWordMapper.selectList(
							Wrappers.<EnglishWord>lambdaQuery()
									.orderByDesc(EnglishWord::getCreateAt)
									.last("LIMIT " + limit))
					.stream().map(this::toView).collect(Collectors.toList());
		} else {
			List<String> wordIds = wordBooks.stream()
					.map(EnglishWordBook::getWordId)
					.collect(Collectors.toList());
			views = englishWordMapper.selectBatchIds(wordIds).stream()
					.map(this::toView)
					.collect(Collectors.toList());
		}
		fillFamiliarity(userId, views);
		return views;
	}

	@Override
	public List<EnglishWordView> getDrillWords(String userId, int limit) {
		if (userId == null || limit <= 0) {
			return new ArrayList<>();
		}
		// 最近答错（wrongCount>0）或复习到期（nextReviewTime<=now），且尚未熟悉的单词
		List<EnglishWordBook> books = englishWordBookMapper.selectList(
				Wrappers.<EnglishWordBook>lambdaQuery()
						.eq(EnglishWordBook::getUserId, userId)
						.lt(EnglishWordBook::getFamiliarity, DRILL_FAMILIARITY_CEILING)
						.and(w -> w.gt(EnglishWordBook::getWrongCount, 0)
								.or().le(EnglishWordBook::getNextReviewTime, new java.util.Date()))
						.orderByDesc(EnglishWordBook::getUpdateAt)
						.last("LIMIT " + limit));
		if (books.isEmpty()) {
			return new ArrayList<>();
		}
		List<String> wordIds = books.stream()
				.map(EnglishWordBook::getWordId)
				.collect(Collectors.toList());
		Map<String, EnglishWord> byId = new LinkedHashMap<>();
		englishWordMapper.selectBatchIds(wordIds).forEach(w -> byId.put(w.getId(), w));
		List<EnglishWordView> views = new ArrayList<>();
		for (EnglishWordBook book : books) {
			EnglishWord word = byId.get(book.getWordId());
			if (word != null) {
				views.add(toView(word));
			}
		}
		fillFamiliarity(userId, views);
		return views;
	}

	@Override
	public boolean hasDrillCompletedToday(String userId) {
		if (userId == null) {
			return false;
		}
		// 当天开始的时刻，用于判定「今天是否已完成巩固」（与全站 LocalDate.now() 口径一致）
		Date todayStart = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
		Long count = englishLearningLogMapper.selectCount(
				Wrappers.<EnglishLearningLog>lambdaQuery()
						.eq(EnglishLearningLog::getUserId, userId)
						.eq(EnglishLearningLog::getType, "drill")
						.ge(EnglishLearningLog::getCreatedAt, todayStart));
		return count != null && count > 0;
	}

	@Override
	public void recordLearning(String userId, String wordId, boolean correct) {
		recordLearning(userId, wordId, correct, SOURCE_LEARN, false);
	}

	@Override
	public void recordLearning(String userId, String wordId, boolean correct, String source, boolean firstAttempt) {
		// 「练习/巩固」的首次作答计入首答统计；学习环节的点击与重做不算
		boolean quizFirst = firstAttempt
				&& (SOURCE_PRACTICE.equals(source) || SOURCE_DRILL.equals(source));
		EnglishWordBook book = englishWordBookMapper.selectOne(
				Wrappers.<EnglishWordBook>lambdaQuery()
						.eq(EnglishWordBook::getUserId, userId)
						.eq(EnglishWordBook::getWordId, wordId));

		if (book == null) {
			// 首次学习
			book = new EnglishWordBook();
			book.setUserId(userId);
			book.setWordId(wordId);
			book.setFamiliarity(correct ? 1 : 0);
			book.setCorrectCount(correct ? 1 : 0);
			book.setWrongCount(correct ? 0 : 1);
			book.setQuizTotal(quizFirst ? 1 : 0);
			book.setQuizFirstCorrect(quizFirst && correct ? 1 : 0);
			book.setNextReviewTime(calculateNextReviewTime(correct ? 1 : 0));
			englishWordBookMapper.insert(book);
		} else {
			// 复习
			int familiarity = book.getFamiliarity() == null ? 0 : book.getFamiliarity();
			if (correct) {
				familiarity = Math.min(familiarity + 1, 4);
			} else {
				familiarity = Math.max(familiarity - 1, 0);
			}
			book.setFamiliarity(familiarity);
			book.setCorrectCount((book.getCorrectCount() == null ? 0 : book.getCorrectCount()) + (correct ? 1 : 0));
			book.setWrongCount((book.getWrongCount() == null ? 0 : book.getWrongCount()) + (correct ? 0 : 1));
			if (quizFirst) {
				book.setQuizTotal((book.getQuizTotal() == null ? 0 : book.getQuizTotal()) + 1);
				book.setQuizFirstCorrect((book.getQuizFirstCorrect() == null ? 0 : book.getQuizFirstCorrect())
						+ (correct ? 1 : 0));
			}
			book.setNextReviewTime(calculateNextReviewTime(familiarity));
			englishWordBookMapper.updateById(book);
		}
		// 逐词结果纳入「单元·单词」掌握度，供薄弱点检测使用
		EnglishWord word = englishWordMapper.selectById(wordId);
		if (word != null) {
			englishPracticeEvaluator.record(userId, word.getVersion(), word.getGrade(), word.getTerm(),
					word.getUnit(), EnglishPracticeEvaluator.MODULE_WORD, correct);
		}
	}

	/**
	 * 根据熟练度计算下次复习时间（艾宾浩斯遗忘曲线）。
	 */
	private java.util.Date calculateNextReviewTime(int familiarity) {
		Calendar cal = Calendar.getInstance();
		int minutes = 0;

		switch (familiarity) {
			case 0: minutes = 5; break;          // 首次复习：5 分钟
			case 1: minutes = 30; break;         // 第 2 次：30 分钟
			case 2: minutes = 720; break;        // 第 3 次：12 小时
			case 3: minutes = 1440; break;       // 第 4 次：1 天
			case 4: minutes = 2880; break;       // 第 5 次：2 天
			default: minutes = 5760; break;      // 第 6 次+：4-15 天
		}

		cal.add(Calendar.MINUTE, minutes);
		return cal.getTime();
	}

	/**
	 * 将英语单词实体转换为视图对象。
	 *
	 * @param word 单词实体
	 * @return 单词视图
	 */
	private EnglishWordView toView(EnglishWord word) {
		EnglishWordView view = new EnglishWordView();
		view.setId(word.getId());
		view.setSpell(word.getSpell());
		view.setPhonetic(word.getPhonetic());
		view.setMeaning(word.getMeaning());
		view.setImageUrl(word.getImageUrl());
		view.setAudioUrl(word.getAudioUrl());
		view.setExampleSentence(word.getExampleSentence());
		view.setVersion(word.getVersion());
		view.setGrade(word.getGrade());
		view.setTerm(word.getTerm());
		view.setUnit(word.getUnit());
		view.setSection(word.getSection());
		view.setLevel(word.getLevel());
		return view;
	}

}

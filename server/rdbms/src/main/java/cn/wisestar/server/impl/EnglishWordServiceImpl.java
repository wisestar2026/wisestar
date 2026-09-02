package cn.wisestar.server.impl;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.domain.dto.english.EnglishWordQuery;
import cn.wisestar.server.domain.dto.english.EnglishWordView;
import cn.wisestar.server.domain.model.EnglishWord;
import cn.wisestar.server.domain.model.EnglishWordBook;
import cn.wisestar.server.mapper.EnglishWordMapper;
import cn.wisestar.server.mapper.EnglishWordBookMapper;
import cn.wisestar.server.service.EnglishWordService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.List;
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

	@Override
	public PaginationResponse<EnglishWordView> listWords(EnglishWordQuery query) {
		LambdaQueryWrapper<EnglishWord> wrapper = Wrappers.<EnglishWord>lambdaQuery()
				.eq(query.getVersion() != null, EnglishWord::getVersion, query.getVersion())
				.eq(query.getGrade() != null, EnglishWord::getGrade, query.getGrade())
				.eq(query.getUnit() != null, EnglishWord::getUnit, query.getUnit())
				.like(query.getSpell() != null, EnglishWord::getSpell, query.getSpell())
				.orderByAsc(EnglishWord::getGrade)
				.orderByAsc(EnglishWord::getUnit)
				.orderByAsc(EnglishWord::getSpell);

		Page<EnglishWord> page = new Page<>(query.getCurrent(), query.getPageSize());
		Page<EnglishWord> result = englishWordMapper.selectPage(page, wrapper);

		List<EnglishWordView> views = result.getRecords().stream()
				.map(this::toView)
				.collect(Collectors.toList());

		return new PaginationResponse<>(result.getTotal(), views);
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

		if (wordBooks.isEmpty()) {
			// 无待复习单词，返回新单词
			List<EnglishWord> newWords = englishWordMapper.selectList(
					Wrappers.<EnglishWord>lambdaQuery()
							.orderByDesc(EnglishWord::getCreateAt)
							.last("LIMIT " + limit));
			return newWords.stream().map(this::toView).collect(Collectors.toList());
		}

		List<String> wordIds = wordBooks.stream()
				.map(EnglishWordBook::getWordId)
				.collect(Collectors.toList());

		return englishWordMapper.selectBatchIds(wordIds).stream()
				.map(this::toView)
				.collect(Collectors.toList());
	}

	@Override
	public void recordLearning(String userId, String wordId, boolean correct) {
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
			book.setNextReviewTime(calculateNextReviewTime(1));
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
			book.setNextReviewTime(calculateNextReviewTime(familiarity));
			englishWordBookMapper.updateById(book);
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
		view.setUnit(word.getUnit());
		return view;
	}

}

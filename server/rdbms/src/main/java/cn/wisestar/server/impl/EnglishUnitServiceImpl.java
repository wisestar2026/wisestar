package cn.wisestar.server.impl;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.domain.dto.english.EnglishUnitQuery;
import cn.wisestar.server.domain.dto.english.EnglishUnitView;
import cn.wisestar.server.domain.model.EnglishSentence;
import cn.wisestar.server.domain.model.EnglishUnit;
import cn.wisestar.server.domain.model.EnglishWord;
import cn.wisestar.server.mapper.EnglishSentenceMapper;
import cn.wisestar.server.mapper.EnglishUnitMapper;
import cn.wisestar.server.mapper.EnglishWordMapper;
import cn.wisestar.server.service.EnglishUnitService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 英语单元目录服务实现。
 *
 * <p>单元列表由「词库单元 ∪ 句库单元 ∪ 单元目录表」派生：
 * 词库/句库决定实际有哪些单元，单元目录表仅用于补充占位单元与自定义排序，
 * 因此无需依赖脆弱的跨方言种子 SQL。</p>
 *
 * @author wisestar
 * @date 2026/9/15
 */
@Service
@RequiredArgsConstructor
public class EnglishUnitServiceImpl implements EnglishUnitService {

	private static final int DEFAULT_SORT = 9999;

	private static final Pattern DIGIT_PATTERN = Pattern.compile("(\\d+)");

	private final EnglishUnitMapper englishUnitMapper;
	private final EnglishWordMapper englishWordMapper;
	private final EnglishSentenceMapper englishSentenceMapper;

	@Override
	public PaginationResponse<EnglishUnitView> list(EnglishUnitQuery query) {
		List<EnglishUnitView> units = buildUnits(query.getVersion(), query.getGrade(), query.getTerm());
		if (query.getUnit() != null && !query.getUnit().isEmpty()) {
			units = units.stream()
					.filter(u -> u.getUnit() != null && u.getUnit().contains(query.getUnit()))
					.collect(Collectors.toList());
		}

		long total = units.size();
		int from = Math.max((query.getCurrent() - 1) * query.getPageSize(), 0);
		List<EnglishUnitView> pageList = new ArrayList<>();
		if (from < units.size()) {
			int to = (int) Math.min((long) from + query.getPageSize(), units.size());
			pageList = new ArrayList<>(units.subList(from, to));
		}
		return new PaginationResponse<>(total, pageList);
	}

	@Override
	public List<EnglishUnitView> listByBook(String version, String grade, String term) {
		return buildUnits(version, grade, term);
	}

	@Override
	public void saveOrUpdate(EnglishUnitView view) {
		EnglishUnit entity = new EnglishUnit();
		entity.setId(view.getId());
		entity.setVersion(view.getVersion());
		entity.setGrade(view.getGrade());
		entity.setTerm(view.getTerm());
		entity.setUnit(view.getUnit());
		entity.setSort(view.getSort());
		if (view.getId() == null || view.getId().isEmpty()) {
			entity.setId(null);
			englishUnitMapper.insert(entity);
		} else {
			englishUnitMapper.updateById(entity);
		}
	}

	@Override
	public void delete(String id) {
		englishUnitMapper.deleteById(id);
	}

	/**
	 * 派生单元列表：合并词库单元、句库单元与单元目录表记录，并回填数量与排序。
	 */
	private List<EnglishUnitView> buildUnits(String version, String grade, String term) {
		Map<String, EnglishUnitView> map = new LinkedHashMap<>();

		List<EnglishWord> words = englishWordMapper.selectList(Wrappers.<EnglishWord>lambdaQuery()
				.eq(version != null, EnglishWord::getVersion, version)
				.eq(grade != null, EnglishWord::getGrade, grade)
				.eq(term != null, EnglishWord::getTerm, term));
		for (EnglishWord word : words) {
			if (isBlank(word.getUnit())) {
				continue;
			}
			EnglishUnitView view = map.computeIfAbsent(word.getUnit(), this::newUnitView);
			view.setWordCount(view.getWordCount() + 1);
			fillBook(view, word.getVersion(), word.getGrade(), word.getTerm());
		}

		List<EnglishSentence> sentences = englishSentenceMapper.selectList(Wrappers.<EnglishSentence>lambdaQuery()
				.eq(version != null, EnglishSentence::getVersion, version)
				.eq(grade != null, EnglishSentence::getGrade, grade)
				.eq(term != null, EnglishSentence::getTerm, term));
		for (EnglishSentence sentence : sentences) {
			if (isBlank(sentence.getUnit())) {
				continue;
			}
			EnglishUnitView view = map.computeIfAbsent(sentence.getUnit(), this::newUnitView);
			view.setSentenceCount(view.getSentenceCount() + 1);
			fillBook(view, sentence.getVersion(), sentence.getGrade(), sentence.getTerm());
		}

		List<EnglishUnit> rows = englishUnitMapper.selectList(Wrappers.<EnglishUnit>lambdaQuery()
				.eq(version != null, EnglishUnit::getVersion, version)
				.eq(grade != null, EnglishUnit::getGrade, grade)
				.eq(term != null, EnglishUnit::getTerm, term));
		for (EnglishUnit row : rows) {
			if (isBlank(row.getUnit())) {
				continue;
			}
			EnglishUnitView view = map.computeIfAbsent(row.getUnit(), this::newUnitView);
			view.setId(row.getId());
			view.setSort(row.getSort());
			fillBook(view, row.getVersion(), row.getGrade(), row.getTerm());
		}

		return map.values().stream()
				.peek(view -> {
					if (view.getSort() == null) {
						view.setSort(parseUnitOrder(view.getUnit()));
					}
				})
				.sorted(Comparator.comparingInt(EnglishUnitView::getSort)
						.thenComparing(view -> parseUnitOrder(view.getUnit()))
						.thenComparing(EnglishUnitView::getUnit, Comparator.nullsLast(Comparator.naturalOrder())))
				.collect(Collectors.toList());
	}

	private EnglishUnitView newUnitView(String unit) {
		EnglishUnitView view = new EnglishUnitView();
		view.setUnit(unit);
		view.setWordCount(0);
		view.setSentenceCount(0);
		return view;
	}

	private void fillBook(EnglishUnitView view, String version, String grade, String term) {
		if (view.getVersion() == null) {
			view.setVersion(version);
		}
		if (view.getGrade() == null) {
			view.setGrade(grade);
		}
		if (view.getTerm() == null) {
			view.setTerm(term);
		}
	}

	/**
	 * 从单元名中提取数字作为默认排序（如 Unit 3 → 3），无法提取时排在最后。
	 */
	private int parseUnitOrder(String unit) {
		if (unit == null) {
			return DEFAULT_SORT;
		}
		Matcher matcher = DIGIT_PATTERN.matcher(unit);
		if (matcher.find()) {
			try {
				return Integer.parseInt(matcher.group(1));
			} catch (NumberFormatException ignored) {
				return DEFAULT_SORT;
			}
		}
		return DEFAULT_SORT;
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

}

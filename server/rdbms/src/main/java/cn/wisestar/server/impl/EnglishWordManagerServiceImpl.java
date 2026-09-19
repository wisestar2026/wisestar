package cn.wisestar.server.impl;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.domain.dto.english.DictionaryEntryView;
import cn.wisestar.server.domain.dto.english.EnglishWordQuery;
import cn.wisestar.server.domain.dto.english.EnglishWordView;
import cn.wisestar.server.domain.dto.english.ImportResult;
import cn.wisestar.server.domain.model.EnglishWord;
import cn.wisestar.server.mapper.EnglishWordMapper;
import cn.wisestar.server.service.EnglishDictionaryService;
import cn.wisestar.server.service.EnglishWordManagerService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 英语单词管理服务实现。
 *
 * @author wisestar
 * @date 2026/8/30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnglishWordManagerServiceImpl implements EnglishWordManagerService {

	private final EnglishWordMapper englishWordMapper;

	private final EnglishDictionaryService dictionaryService;

	@Override
	public PaginationResponse<EnglishWordView> listWords(EnglishWordQuery query) {
		LambdaQueryWrapper<EnglishWord> wrapper = Wrappers.<EnglishWord>lambdaQuery()
				.eq(query.getVersion() != null, EnglishWord::getVersion, query.getVersion())
				.eq(query.getGrade() != null, EnglishWord::getGrade, query.getGrade())
				.eq(query.getTerm() != null, EnglishWord::getTerm, query.getTerm())
				.eq(query.getUnit() != null, EnglishWord::getUnit, query.getUnit())
				.eq(query.getSection() != null, EnglishWord::getSection, query.getSection())
				.like(query.getSpell() != null, EnglishWord::getSpell, query.getSpell())
				.orderByAsc(EnglishWord::getGrade)
				.orderByAsc(EnglishWord::getTerm)
				.orderByAsc(EnglishWord::getUnit)
				.orderByAsc(EnglishWord::getSection)
				.orderByAsc(EnglishWord::getSpell);

		// 按是否有图片过滤：false 仅无图，true 仅有图
		if (Boolean.FALSE.equals(query.getHasImage())) {
			wrapper.and(w -> w.isNull(EnglishWord::getImageUrl).or().eq(EnglishWord::getImageUrl, ""));
		} else if (Boolean.TRUE.equals(query.getHasImage())) {
			wrapper.isNotNull(EnglishWord::getImageUrl).ne(EnglishWord::getImageUrl, "");
		}

		Page<EnglishWord> page = new Page<>(query.getCurrent(), query.getPageSize());
		Page<EnglishWord> result = englishWordMapper.selectPage(page, wrapper);

		List<EnglishWordView> views = result.getRecords().stream()
				.map(this::toView)
				.collect(Collectors.toList());

		return new PaginationResponse<>(result.getTotal(), views);
	}

	@Override
	public EnglishWordView getDetail(String id) {
		EnglishWord word = englishWordMapper.selectById(id);
		return toView(word);
	}

	@Override
	public void createWord(EnglishWordView word) {
		EnglishWord entity = toEntity(word);
		entity.setId(UUID.randomUUID().toString().replace("-", ""));
		englishWordMapper.insert(entity);
	}

	@Override
	public void updateWord(EnglishWordView word) {
		EnglishWord entity = toEntity(word);
		englishWordMapper.updateById(entity);
	}

	@Override
	public void deleteWord(String id) {
		englishWordMapper.deleteById(id);
	}

	@Override
	public ImportResult importWords(MultipartFile file) {
		List<String> errors = new ArrayList<>();
		int success = 0;
		int failed = 0;

		try {
			Workbook workbook = WorkbookFactory.create(file.getInputStream());
			Sheet sheet = workbook.getSheetAt(0);

			for (int i = 1; i <= sheet.getLastRowNum(); i++) {
				Row row = sheet.getRow(i);
				if (row == null) continue;

				try {
					EnglishWord word = new EnglishWord();
					word.setId(UUID.randomUUID().toString().replace("-", ""));
					word.setSpell(getCellValue(row.getCell(0)));
					word.setPhonetic(getCellValue(row.getCell(1)));
					word.setMeaning(getCellValue(row.getCell(2)));
					word.setImageUrl(getCellValue(row.getCell(3)));
					word.setAudioUrl(getCellValue(row.getCell(4)));
					word.setExampleSentence(getCellValue(row.getCell(5)));
					word.setVersion(getCellValue(row.getCell(6)));
					word.setGrade(getCellValue(row.getCell(7)));
					word.setUnit(getCellValue(row.getCell(8)));
					word.setTerm(getCellValue(row.getCell(9)));
					word.setSection(getCellValue(row.getCell(10)));

					englishWordMapper.insert(word);
					success++;
				} catch (Exception e) {
					errors.add("第" + (i + 1) + "行导入失败：" + e.getMessage());
					failed++;
				}
			}

			workbook.close();
		} catch (IOException e) {
			throw new RuntimeException("Excel 文件解析失败：" + e.getMessage(), e);
		}

		return new ImportResult(success + failed, success, failed, errors);
	}

	/**
	 * 从免费词典接口补全单词的音标、释义、例句（只填充空字段）。
	 */
	@Override
	public ImportResult fillFromDictionary(List<String> wordIds) {
		List<String> errors = new ArrayList<>();
		int success = 0;
		int failed = 0;

		if (wordIds != null) {
			for (String wordId : wordIds) {
				if (wordId == null || wordId.trim().isEmpty()) {
					continue;
				}
				try {
					EnglishWord word = englishWordMapper.selectById(wordId);
					if (word == null) {
						errors.add("单词不存在：" + wordId);
						failed++;
						continue;
					}
					DictionaryEntryView entry = dictionaryService.lookup(word.getSpell());
					if (entry == null) {
						errors.add(word.getSpell() + "：词典未找到结果");
						failed++;
						continue;
					}
					boolean changed = false;
					if (isBlank(word.getPhonetic()) && !isBlank(entry.getPhonetic())) {
						word.setPhonetic(entry.getPhonetic());
						changed = true;
					}
					if (isBlank(word.getMeaning()) && !isBlank(entry.getMeaning())) {
						word.setMeaning(entry.getMeaning());
						changed = true;
					}
					if (isBlank(word.getExampleSentence()) && !isBlank(entry.getExampleSentence())) {
						word.setExampleSentence(entry.getExampleSentence());
						changed = true;
					}
					if (changed) {
						englishWordMapper.updateById(word);
					}
					success++;
				} catch (Exception e) {
					errors.add("单词 ID " + wordId + " 补全失败：" + e.getMessage());
					failed++;
					log.warn("词典补全失败 wordId={} err={}", wordId, e.getMessage());
				}
			}
		}

		return new ImportResult(success + failed, success, failed, errors);
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

	/**
	 * 获取单元格值。
	 */
	private String getCellValue(Cell cell) {
		if (cell == null) return null;
		switch (cell.getCellType()) {
			case STRING:
				return cell.getStringCellValue();
			case NUMERIC:
				return String.valueOf((long) cell.getNumericCellValue());
			default:
				return null;
		}
	}

	/**
	 * View 转 Entity。
	 */
	private EnglishWord toEntity(EnglishWordView view) {
		EnglishWord entity = new EnglishWord();
		entity.setId(view.getId());
		entity.setSpell(view.getSpell());
		entity.setPhonetic(view.getPhonetic());
		entity.setMeaning(view.getMeaning());
		entity.setImageUrl(view.getImageUrl());
		entity.setAudioUrl(view.getAudioUrl());
		entity.setExampleSentence(view.getExampleSentence());
		entity.setVersion(view.getVersion());
		entity.setGrade(view.getGrade());
		entity.setUnit(view.getUnit());
		entity.setTerm(view.getTerm());
		entity.setSection(view.getSection());
		return entity;
	}

	/**
	 * Entity 转 View。
	 */
	private EnglishWordView toView(EnglishWord entity) {
		EnglishWordView view = new EnglishWordView();
		view.setId(entity.getId());
		view.setSpell(entity.getSpell());
		view.setPhonetic(entity.getPhonetic());
		view.setMeaning(entity.getMeaning());
		view.setImageUrl(entity.getImageUrl());
		view.setAudioUrl(entity.getAudioUrl());
		view.setExampleSentence(entity.getExampleSentence());
		view.setVersion(entity.getVersion());
		view.setGrade(entity.getGrade());
		view.setUnit(entity.getUnit());
		view.setTerm(entity.getTerm());
		view.setSection(entity.getSection());
		return view;
	}

}

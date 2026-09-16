package cn.wisestar.server.impl;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.domain.dto.english.EnglishSentenceQuery;
import cn.wisestar.server.domain.dto.english.EnglishSentenceView;
import cn.wisestar.server.domain.dto.english.ImportResult;
import cn.wisestar.server.domain.model.EnglishSentence;
import cn.wisestar.server.mapper.EnglishSentenceMapper;
import cn.wisestar.server.service.EnglishSentenceService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 英语句库服务实现。
 *
 * @author wisestar
 * @date 2026/9/15
 */
@Service
@RequiredArgsConstructor
public class EnglishSentenceServiceImpl implements EnglishSentenceService {

	private final EnglishSentenceMapper englishSentenceMapper;

	@Override
	public PaginationResponse<EnglishSentenceView> list(EnglishSentenceQuery query) {
		LambdaQueryWrapper<EnglishSentence> wrapper = Wrappers.<EnglishSentence>lambdaQuery()
				.eq(query.getVersion() != null, EnglishSentence::getVersion, query.getVersion())
				.eq(query.getGrade() != null, EnglishSentence::getGrade, query.getGrade())
				.eq(query.getTerm() != null, EnglishSentence::getTerm, query.getTerm())
				.eq(query.getUnit() != null, EnglishSentence::getUnit, query.getUnit())
				.and(query.getKeyword() != null && !query.getKeyword().isEmpty(),
						w -> w.like(EnglishSentence::getEn, query.getKeyword())
								.or().like(EnglishSentence::getZh, query.getKeyword()))
				.orderByAsc(EnglishSentence::getGrade)
				.orderByAsc(EnglishSentence::getTerm)
				.orderByAsc(EnglishSentence::getUnit)
				.orderByAsc(EnglishSentence::getSort);

		Page<EnglishSentence> page = new Page<>(query.getCurrent(), query.getPageSize());
		Page<EnglishSentence> result = englishSentenceMapper.selectPage(page, wrapper);

		List<EnglishSentenceView> views = result.getRecords().stream()
				.map(this::toView)
				.collect(Collectors.toList());

		return new PaginationResponse<>(result.getTotal(), views);
	}

	@Override
	public void saveOrUpdate(EnglishSentenceView view) {
		EnglishSentence entity = toEntity(view);
		if (view.getId() == null || view.getId().isEmpty()) {
			entity.setId(null);
			englishSentenceMapper.insert(entity);
		} else {
			englishSentenceMapper.updateById(entity);
		}
	}

	@Override
	public void delete(String id) {
		englishSentenceMapper.deleteById(id);
	}

	@Override
	public ImportResult importSentences(MultipartFile file) {
		List<String> errors = new ArrayList<>();
		int success = 0;
		int failed = 0;

		try {
			Workbook workbook = WorkbookFactory.create(file.getInputStream());
			Sheet sheet = workbook.getSheetAt(0);

			for (int i = 1; i <= sheet.getLastRowNum(); i++) {
				Row row = sheet.getRow(i);
				if (row == null) {
					continue;
				}
				try {
					String version = getCellValue(row.getCell(0));
					String grade = getCellValue(row.getCell(1));
					String term = getCellValue(row.getCell(2));
					String unit = getCellValue(row.getCell(3));
					String en = getCellValue(row.getCell(4));
					String zh = getCellValue(row.getCell(5));
					String audioUrl = getCellValue(row.getCell(6));

					if (en == null || en.trim().isEmpty()) {
						throw new IllegalArgumentException("英文句子为空");
					}

					EnglishSentence existing = englishSentenceMapper.selectOne(
							Wrappers.<EnglishSentence>lambdaQuery()
									.eq(EnglishSentence::getVersion, version)
									.eq(EnglishSentence::getGrade, grade)
									.eq(EnglishSentence::getTerm, term)
									.eq(EnglishSentence::getUnit, unit)
									.eq(EnglishSentence::getEn, en)
									.last("LIMIT 1"));
					if (existing != null) {
						existing.setZh(zh);
						existing.setAudioUrl(audioUrl);
						englishSentenceMapper.updateById(existing);
					} else {
						EnglishSentence entity = new EnglishSentence();
						entity.setVersion(version);
						entity.setGrade(grade);
						entity.setTerm(term);
						entity.setUnit(unit);
						entity.setEn(en);
						entity.setZh(zh);
						entity.setAudioUrl(audioUrl);
						entity.setSort(0);
						englishSentenceMapper.insert(entity);
					}
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
	 * 获取单元格值。
	 */
	private String getCellValue(Cell cell) {
		if (cell == null) {
			return null;
		}
		switch (cell.getCellType()) {
			case STRING:
				return cell.getStringCellValue().trim();
			case NUMERIC:
				return String.valueOf((long) cell.getNumericCellValue());
			default:
				return null;
		}
	}

	private EnglishSentence toEntity(EnglishSentenceView view) {
		EnglishSentence entity = new EnglishSentence();
		entity.setId(view.getId());
		entity.setEn(view.getEn());
		entity.setZh(view.getZh());
		entity.setAudioUrl(view.getAudioUrl());
		entity.setVersion(view.getVersion());
		entity.setGrade(view.getGrade());
		entity.setTerm(view.getTerm());
		entity.setUnit(view.getUnit());
		entity.setSort(view.getSort() == null ? 0 : view.getSort());
		return entity;
	}

	private EnglishSentenceView toView(EnglishSentence entity) {
		EnglishSentenceView view = new EnglishSentenceView();
		view.setId(entity.getId());
		view.setEn(entity.getEn());
		view.setZh(entity.getZh());
		view.setAudioUrl(entity.getAudioUrl());
		view.setVersion(entity.getVersion());
		view.setGrade(entity.getGrade());
		view.setTerm(entity.getTerm());
		view.setUnit(entity.getUnit());
		view.setSort(entity.getSort());
		return view;
	}

}

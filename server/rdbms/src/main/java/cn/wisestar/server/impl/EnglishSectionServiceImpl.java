package cn.wisestar.server.impl;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.domain.dto.english.EnglishSectionQuery;
import cn.wisestar.server.domain.dto.english.EnglishSectionView;
import cn.wisestar.server.domain.model.EnglishSection;
import cn.wisestar.server.mapper.EnglishSectionMapper;
import cn.wisestar.server.service.EnglishSectionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.validation.ValidationException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 英语小节目录服务实现。
 *
 * @author wisestar
 * @date 2026/9/18
 */
@Service
@RequiredArgsConstructor
public class EnglishSectionServiceImpl implements EnglishSectionService {

	private final EnglishSectionMapper englishSectionMapper;

	@Override
	public PaginationResponse<EnglishSectionView> list(EnglishSectionQuery query) {
		LambdaQueryWrapper<EnglishSection> wrapper = Wrappers.<EnglishSection>lambdaQuery()
				.eq(query.getVersion() != null, EnglishSection::getVersion, query.getVersion())
				.eq(query.getGrade() != null, EnglishSection::getGrade, query.getGrade())
				.eq(query.getTerm() != null, EnglishSection::getTerm, query.getTerm())
				.eq(query.getUnit() != null, EnglishSection::getUnit, query.getUnit())
				.like(query.getSection() != null && !query.getSection().isEmpty(),
						EnglishSection::getSection, query.getSection())
				.orderByAsc(EnglishSection::getGrade)
				.orderByAsc(EnglishSection::getTerm)
				.orderByAsc(EnglishSection::getUnit)
				.orderByAsc(EnglishSection::getSort)
				.orderByAsc(EnglishSection::getSection);

		Page<EnglishSection> page = new Page<>(query.getCurrent(), query.getPageSize());
		Page<EnglishSection> result = englishSectionMapper.selectPage(page, wrapper);

		List<EnglishSectionView> views = result.getRecords().stream()
				.map(this::toView)
				.collect(Collectors.toList());

		return new PaginationResponse<>(result.getTotal(), views);
	}

	@Override
	public List<EnglishSectionView> listByUnit(String version, String grade, String term, String unit) {
		return englishSectionMapper.selectList(Wrappers.<EnglishSection>lambdaQuery()
						.eq(version != null, EnglishSection::getVersion, version)
						.eq(grade != null, EnglishSection::getGrade, grade)
						.eq(term != null, EnglishSection::getTerm, term)
						.eq(unit != null, EnglishSection::getUnit, unit)
						.orderByAsc(EnglishSection::getSort)
						.orderByAsc(EnglishSection::getSection))
				.stream()
				.map(this::toView)
				.collect(Collectors.toList());
	}

	@Override
	public void saveOrUpdate(EnglishSectionView view) {
		if (isBlank(view.getSection())) {
			throw new ValidationException("小节名称不能为空");
		}
		String section = view.getSection().trim();

		// 同一「版本 + 年级 + 学期 + 单元」下小节重名校验
		LambdaQueryWrapper<EnglishSection> duplicate = Wrappers.<EnglishSection>lambdaQuery()
				.eq(EnglishSection::getVersion, view.getVersion())
				.eq(EnglishSection::getGrade, view.getGrade())
				.eq(EnglishSection::getTerm, view.getTerm())
				.eq(EnglishSection::getUnit, view.getUnit())
				.eq(EnglishSection::getSection, section);
		if (view.getId() != null && !view.getId().isEmpty()) {
			duplicate.ne(EnglishSection::getId, view.getId());
		}
		if (englishSectionMapper.selectCount(duplicate) > 0) {
			throw new ValidationException("同一单元下已存在同名小节");
		}

		EnglishSection entity = new EnglishSection();
		entity.setId(view.getId());
		entity.setVersion(view.getVersion());
		entity.setGrade(view.getGrade());
		entity.setTerm(view.getTerm());
		entity.setUnit(view.getUnit());
		entity.setSection(section);
		entity.setSort(view.getSort());
		if (view.getId() == null || view.getId().isEmpty()) {
			entity.setId(null);
			englishSectionMapper.insert(entity);
		} else {
			englishSectionMapper.updateById(entity);
		}
	}

	@Override
	public void delete(String id) {
		englishSectionMapper.deleteById(id);
	}

	private EnglishSectionView toView(EnglishSection entity) {
		EnglishSectionView view = new EnglishSectionView();
		view.setId(entity.getId());
		view.setVersion(entity.getVersion());
		view.setGrade(entity.getGrade());
		view.setTerm(entity.getTerm());
		view.setUnit(entity.getUnit());
		view.setSection(entity.getSection());
		view.setSort(entity.getSort());
		return view;
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

}

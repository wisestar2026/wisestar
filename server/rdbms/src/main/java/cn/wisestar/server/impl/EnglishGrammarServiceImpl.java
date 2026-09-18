package cn.wisestar.server.impl;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.domain.dto.english.EnglishGrammarQuery;
import cn.wisestar.server.domain.dto.english.EnglishGrammarView;
import cn.wisestar.server.domain.model.EnglishGrammar;
import cn.wisestar.server.mapper.EnglishGrammarMapper;
import cn.wisestar.server.service.EnglishGrammarService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.validation.ValidationException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 英语语法服务实现。
 *
 * @author wisestar
 * @date 2026/9/18
 */
@Service
@RequiredArgsConstructor
public class EnglishGrammarServiceImpl implements EnglishGrammarService {

	private final EnglishGrammarMapper englishGrammarMapper;

	@Override
	public PaginationResponse<EnglishGrammarView> list(EnglishGrammarQuery query) {
		LambdaQueryWrapper<EnglishGrammar> wrapper = Wrappers.<EnglishGrammar>lambdaQuery()
				.eq(query.getVersion() != null, EnglishGrammar::getVersion, query.getVersion())
				.eq(query.getGrade() != null, EnglishGrammar::getGrade, query.getGrade())
				.eq(query.getTerm() != null, EnglishGrammar::getTerm, query.getTerm())
				.eq(query.getUnit() != null, EnglishGrammar::getUnit, query.getUnit())
				.eq(query.getSection() != null, EnglishGrammar::getSection, query.getSection())
				.like(query.getTitle() != null && !query.getTitle().isEmpty(),
						EnglishGrammar::getTitle, query.getTitle())
				.orderByAsc(EnglishGrammar::getGrade)
				.orderByAsc(EnglishGrammar::getTerm)
				.orderByAsc(EnglishGrammar::getUnit)
				.orderByAsc(EnglishGrammar::getSort)
				.orderByAsc(EnglishGrammar::getTitle);

		Page<EnglishGrammar> page = new Page<>(query.getCurrent(), query.getPageSize());
		Page<EnglishGrammar> result = englishGrammarMapper.selectPage(page, wrapper);

		List<EnglishGrammarView> views = result.getRecords().stream()
				.map(this::toView)
				.collect(Collectors.toList());

		return new PaginationResponse<>(result.getTotal(), views);
	}

	@Override
	public void saveOrUpdate(EnglishGrammarView view) {
		if (isBlank(view.getTitle())) {
			throw new ValidationException("语法标题不能为空");
		}
		EnglishGrammar entity = toEntity(view);
		if (view.getId() == null || view.getId().isEmpty()) {
			entity.setId(null);
			englishGrammarMapper.insert(entity);
		} else {
			englishGrammarMapper.updateById(entity);
		}
	}

	@Override
	public void delete(String id) {
		englishGrammarMapper.deleteById(id);
	}

	@Override
	public boolean upsertFromAi(EnglishGrammarView view) {
		if (view == null || isBlank(view.getTitle())) {
			return false;
		}
		String title = view.getTitle().trim();
		EnglishGrammar exist = englishGrammarMapper.selectOne(dedupWrapper(view, title).last("limit 1"));
		boolean isNew = exist == null;
		if (isNew) {
			exist = new EnglishGrammar();
		}
		exist.setTitle(title);
		exist.setContent(view.getContent());
		exist.setExamples(view.getExamples());
		exist.setExercises(view.getExercises());
		exist.setVersion(view.getVersion());
		exist.setGrade(view.getGrade());
		exist.setTerm(view.getTerm());
		exist.setUnit(view.getUnit());
		exist.setSection(view.getSection());
		exist.setSort(view.getSort() == null ? 0 : view.getSort());
		if (isNew) {
			englishGrammarMapper.insert(exist);
		} else {
			englishGrammarMapper.updateById(exist);
		}
		return true;
	}

	private LambdaQueryWrapper<EnglishGrammar> dedupWrapper(EnglishGrammarView view, String title) {
		return Wrappers.<EnglishGrammar>lambdaQuery()
				.eq(EnglishGrammar::getTitle, title)
				.eq(view.getVersion() != null, EnglishGrammar::getVersion, view.getVersion())
				.isNull(view.getVersion() == null, EnglishGrammar::getVersion)
				.eq(view.getGrade() != null, EnglishGrammar::getGrade, view.getGrade())
				.isNull(view.getGrade() == null, EnglishGrammar::getGrade)
				.eq(view.getTerm() != null, EnglishGrammar::getTerm, view.getTerm())
				.isNull(view.getTerm() == null, EnglishGrammar::getTerm)
				.eq(view.getUnit() != null, EnglishGrammar::getUnit, view.getUnit())
				.isNull(view.getUnit() == null, EnglishGrammar::getUnit)
				.eq(view.getSection() != null, EnglishGrammar::getSection, view.getSection())
				.isNull(view.getSection() == null, EnglishGrammar::getSection);
	}

	private EnglishGrammar toEntity(EnglishGrammarView view) {
		EnglishGrammar entity = new EnglishGrammar();
		entity.setId(view.getId());
		entity.setTitle(view.getTitle() == null ? null : view.getTitle().trim());
		entity.setContent(view.getContent());
		entity.setExamples(view.getExamples());
		entity.setExercises(view.getExercises());
		entity.setVersion(view.getVersion());
		entity.setGrade(view.getGrade());
		entity.setTerm(view.getTerm());
		entity.setUnit(view.getUnit());
		entity.setSection(view.getSection());
		entity.setSort(view.getSort() == null ? 0 : view.getSort());
		return entity;
	}

	private EnglishGrammarView toView(EnglishGrammar entity) {
		EnglishGrammarView view = new EnglishGrammarView();
		view.setId(entity.getId());
		view.setTitle(entity.getTitle());
		view.setContent(entity.getContent());
		view.setExamples(entity.getExamples());
		view.setExercises(entity.getExercises());
		view.setVersion(entity.getVersion());
		view.setGrade(entity.getGrade());
		view.setTerm(entity.getTerm());
		view.setUnit(entity.getUnit());
		view.setSection(entity.getSection());
		view.setSort(entity.getSort());
		view.setUpdateAt(entity.getUpdateAt());
		return view;
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

}

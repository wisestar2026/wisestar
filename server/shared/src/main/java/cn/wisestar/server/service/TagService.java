package cn.wisestar.server.service;

import cn.wisestar.server.core.constant.TagCategoryEnum;
import cn.wisestar.server.domain.dto.SelectTagRequest;

import java.util.Set;

/**
 * 标签服务接口（TagService）。
 *
 * <p><b>所属模块</b>：shared 模块服务接口包（cn.wisestar.server.service）。</p>
 * <p><b>类职责</b>：提供标签的通用维护能力：为业务实体（模板/题库/问卷/考试）
 * 批量添加标签、按实体删除标签、按条件查询标签。标签按
 * {@link TagCategoryEnum} 区分业务归属。实现类位于 rdbms 模块（TagServiceImpl）。</p>
 *
 * <p><b>调用方</b>：模板管理、题库管理等模块在创建/更新实体时调用
 * batchAddTag / deleteTagByEntryId 维护标签关系；标签筛选项查询调用 selectTag。</p>
 *
 * @author javahuang
 * @date 2022/4/28
 */
public interface TagService {

	/**
	 * 为指定实体批量添加标签（覆盖式：先删后加或合并，由实现决定）。
	 *
	 * @param entityId 业务实体 id（模板/题库/问卷/考试 id）
	 * @param category 标签分类（TagCategoryEnum：template/repo/survey/exam）
	 * @param tagArr   标签名数组
	 */
	void batchAddTag(String entityId, TagCategoryEnum category, String[] tagArr);

	/**
	 * 按业务实体删除其全部标签。
	 *
	 * @param entityId 业务实体 id
	 */
	void deleteTagByEntryId(String entityId);

	/**
	 * 按条件查询标签集合（去重）。
	 *
	 * @param request 查询条件（分类、实体范围、关键字等，见 {@link SelectTagRequest}）
	 * @return 匹配的标签名集合
	 */
	Set<String> selectTag(SelectTagRequest request);

}

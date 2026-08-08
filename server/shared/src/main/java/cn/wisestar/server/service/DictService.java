package cn.wisestar.server.service;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.domain.dto.*;

import java.util.List;

/**
 * 字典服务接口（DictService）。
 *
 * <p><b>所属模块</b>：shared 模块服务接口包（cn.wisestar.server.service）。</p>
 * <p><b>类职责</b>：提供通用字典（CommDict）与字典项（CommDictItem）的完整
 * 维护能力：字典分页、字典 CRUD、字典项分页/新增/更新/删除/导入，以及
 * 字典选择器数据源。字典用于问卷下拉选项、系统参数等可配置数据。
 * 实现类位于 rdbms 模块（DictServiceImpl）。</p>
 *
 * <p><b>调用方</b>：api 模块系统管理相关接口（SystemApi 中的字典管理端点）。</p>
 *
 * @author javahuang
 * @date 2022/7/19
 */
public interface DictService {

	/**
	 * 分页查询字典列表。
	 *
	 * @param query 分页 + 筛选条件（见 {@link CommDictQuery}）
	 * @return 字典分页列表（CommDictView）
	 */
	PaginationResponse<CommDictView> listDict(CommDictQuery query);

	/**
	 * 新增字典。
	 *
	 * @param request 字典创建请求（见 {@link CommDictRequest}）
	 */
	void addDict(CommDictRequest request);

	/**
	 * 更新字典。
	 *
	 * @param request 字典更新请求（含 id）
	 */
	void updateDict(CommDictRequest request);

	/**
	 * 删除字典（含其字典项）。
	 *
	 * @param id 字典 id
	 */
	void deleteDict(String id);

	/**
	 * 分页查询指定字典的字典项列表。
	 *
	 * @param query 分页 + 筛选条件（含 dictId 等，见 {@link CommDictItemQuery}）
	 * @return 字典项分页列表（CommDictItemView）
	 */
	PaginationResponse<CommDictItemView> listDictItem(CommDictItemQuery query);

	/**
	 * 新增或更新字典项（存在则更新，否则新增）。
	 *
	 * @param request 字典项请求（见 {@link CommDictItemRequest}）
	 */
	void saveOrUpdateDictItem(CommDictItemRequest request);

	/**
	 * 删除字典项。
	 *
	 * @param id 字典项 id
	 */
	void deleteDictItem(String id);

	/**
	 * 导入字典项（批量新增）。
	 *
	 * @param request 字典项导入请求（含批量数据）
	 */
	void importDictItem(CommDictItemRequest request);

	/**
	 * 字典选择器数据源（下拉选择用）。
	 *
	 * @return 全部字典视图列表
	 */
	List<CommDictView> selectDict();

}

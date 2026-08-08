package cn.wisestar.server.service;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.domain.dto.PositionQuery;
import cn.wisestar.server.domain.dto.PositionRequest;
import cn.wisestar.server.domain.dto.PositionView;
import cn.wisestar.server.domain.dto.SelectPositionRequest;

import java.util.List;

/**
 * 岗位服务接口（PositionService）。
 *
 * <p><b>所属模块</b>：shared 模块服务接口包（cn.wisestar.server.service）。</p>
 * <p><b>类职责</b>：提供岗位（Position）的管理能力：分页列表、新增、修改、
 * 删除，以及岗位选择器数据源。岗位用于定义用户的数据权限范围
 * （本人/本部门/全部等，见 {@link cn.wisestar.server.core.constant.AppConsts.DataPermissionTypeEnum}）。
 * 实现类位于 rdbms 模块（PositionServiceImpl）。</p>
 *
 * @author javahuang
 * @date 2021/11/2
 */
public interface PositionService {

	/**
	 * 分页查询岗位列表。
	 *
	 * @param query 分页 + 筛选条件（见 {@link PositionQuery}）
	 * @return 岗位分页列表
	 */
	PaginationResponse<PositionView> listPosition(PositionQuery query);

	/**
	 * 新增岗位。
	 *
	 * @param request 岗位创建请求（见 {@link PositionRequest}）
	 */
	void addPosition(PositionRequest request);

	/**
	 * 更新岗位。
	 *
	 * @param request 岗位更新请求（含 id）
	 */
	void updatePosition(PositionRequest request);

	/**
	 * 删除岗位。
	 *
	 * @param id 岗位 id
	 */
	void deletePosition(String id);

	/**
	 * 岗位选择器数据源。
	 *
	 * @param request 查询条件（见 {@link SelectPositionRequest}）
	 * @return 岗位视图列表
	 */
	List<PositionView> selectPositions(SelectPositionRequest request);

}

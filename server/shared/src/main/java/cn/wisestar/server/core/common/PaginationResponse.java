package cn.wisestar.server.core.common;

import lombok.Data;

import java.util.List;

/**
 * 分页响应对象（PaginationResponse）。
 *
 * <p><b>所属模块</b>：shared 模块核心通用类（cn.wisestar.server.core.common）。</p>
 * <p><b>类职责</b>：所有分页查询接口的统一返回结构，包含总数 total、
 * 当前页数据列表 list、当前页码 current 与每页大小 pageSize。</p>
 *
 * <p><b>数据流</b>：Service 层分页查询（MyBatis-Plus Page）→ 组装本对象 →
 * Controller 返回 → CustomResponseBodyAdvice 包装为 ApiResponse →
 * JSON 给前端（前端根据 total 渲染分页器）。</p>
 *
 * <p><b>使用示例</b>：{@link cn.wisestar.server.service.TemplateService#listTemplate}、
 * 用户/角色/部门等所有分页接口均返回本类型。</p>
 *
 * @param <T> 列表元素类型（如 TemplateView、UserView 等视图 DTO）
 * @author javahuang
 * @date 2021/10/6
 */
@Data
public class PaginationResponse<T> {

	/**
	 * 符合条件的总记录数（用于前端分页器计算总页数）。
	 */
	private Long total;

	/**
	 * 当前页的数据列表（每页最多 pageSize 条）。
	 */
	private List<T> list;

	/**
	 * 当前页码（从 1 开始；由查询条件传入，此处回显）。
	 */
	private Integer current;

	/**
	 * 每页条数（由查询条件传入，此处回显）。
	 */
	private Integer pageSize;

	/**
	 * 构造分页响应（total + list；current / pageSize 需由调用方另行 set，或保持为空）。
	 *
	 * @param total 总记录数
	 * @param list  当前页数据列表
	 */
	public PaginationResponse(Long total, List<T> list) {
		this.total = total;
		this.list = list;
	}

}

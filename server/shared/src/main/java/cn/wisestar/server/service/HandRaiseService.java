package cn.wisestar.server.service;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.domain.dto.handraise.HandRaiseHandleRequest;
import cn.wisestar.server.domain.dto.handraise.HandRaiseQuery;
import cn.wisestar.server.domain.dto.handraise.HandRaiseRequest;
import cn.wisestar.server.domain.dto.handraise.HandRaiseView;

import java.util.Map;

/**
 * 学员举手反馈服务接口。
 *
 * <p><b>所属模块</b>：shared 模块服务接口包，实现类位于 rdbms 模块（HandRaiseServiceImpl）。</p>
 * <p><b>类职责</b>：学员题目页「举手」提交（寻求讲解 / 题干有误 / 答案有误）、
 * 教师端提醒列表与处理、待处理数量统计。</p>
 *
 * @author wisestar
 * @date 2026/10/22
 */
public interface HandRaiseService {

	/**
	 * 学员提交举手反馈（服务端按题目ID回填题干/选项/标准答案/解析快照）。
	 *
	 * @param request 反馈内容（含题目ID、学员作答与补充说明）
	 */
	void createByStudent(HandRaiseRequest request);

	/**
	 * 举手反馈分页列表（按当前登录用户的校区数据权限过滤）。
	 *
	 * @param query 类型/状态/学科/关键词/学员筛选
	 * @return 分页反馈列表
	 */
	PaginationResponse<HandRaiseView> listHandRaises(HandRaiseQuery query);

	/**
	 * 处理反馈（标记已处理/已忽略）。
	 *
	 * @param request 反馈ID、目标状态与处理备注
	 */
	void handle(HandRaiseHandleRequest request);

	/**
	 * 待处理数量统计（教师端菜单角标）。
	 *
	 * @return explain 待讲解数 / error 待修题数 / total 合计
	 */
	Map<String, Long> pendingCount();

}

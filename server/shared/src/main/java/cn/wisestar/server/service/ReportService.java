package cn.wisestar.server.service;

import cn.wisestar.server.domain.dto.ReportData;

/**
 * 报表数据服务接口（ReportService）。
 *
 * <p><b>所属模块</b>：shared 模块服务接口包（cn.wisestar.server.service）。</p>
 * <p><b>类职责</b>：提供问卷项目的统计报表数据，供项目概要/数据看板页面
 * 展示回收统计、每日回收数量趋势等。实现类位于 rdbms 模块
 * （ReportServiceImpl）。</p>
 *
 * <p><b>调用方</b>：api 模块 ReportApi（GET /api/report/xxx 系列接口）。</p>
 *
 * @author javahuang
 * @date 2021/8/3
 */
public interface ReportService {

	/**
	 * 获取指定项目的报表数据。
	 *
	 * @param shortId 项目的 short id（对外短标识，如 RyP2rR）
	 * @return 报表数据（含回收量统计、每日数量趋势等，结构见 {@link ReportData}）
	 */
	ReportData getData(String shortId);

}

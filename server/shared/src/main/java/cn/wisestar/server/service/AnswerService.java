package cn.wisestar.server.service;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.domain.dto.ExerciseView;
import cn.wisestar.server.domain.dto.HistoryExerciseQuery;

/**
 * 答卷服务接口（AnswerService）。
 *
 * <p><b>所属模块</b>：shared 模块服务接口包（cn.wisestar.server.service）。</p>
 * <p><b>类职责</b>：练习历史查询。问卷/答卷管理链路（答卷分页、详情、保存、删除、
 * 回收站、附件与 Excel 导出等）已随对应前端模块与控制器下线一并移除，
 * 故接口仅保留练习历史能力。实现类位于 rdbms 模块（AnswerServiceImpl）。</p>
 *
 * @author javahuang
 * @date 2021/8/3
 */
public interface AnswerService {

	/**
	 * 分页查询历史练习记录（考试练习场景）。
	 *
	 * @param query 分页 + 筛选条件（见 {@link HistoryExerciseQuery}）
	 * @return 练习记录分页列表（ExerciseView）
	 */
	PaginationResponse<ExerciseView> historyExercise(HistoryExerciseQuery query);
}

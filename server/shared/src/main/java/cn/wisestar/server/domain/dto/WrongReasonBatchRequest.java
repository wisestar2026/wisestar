package cn.wisestar.server.domain.dto;

import lombok.Data;

import java.util.List;

/**
 * 错题错误归因批量保存请求（交卷后强制逐题归因一次提交）。
 *
 * <p>专项练习交卷 → 后端判分返回逐题明细 ID → 学员为每道错题选择错因 →
 * 全部选完后批量提交（同属本次练习的明细）。</p>
 *
 * @author wisestar
 * @date 2026/9/8
 */
@Data
public class WrongReasonBatchRequest {

	/**
	 * 批量归因条目。
	 */
	private List<WrongReasonRequest> items;

}

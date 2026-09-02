package cn.wisestar.server.domain.dto.english;

import lombok.Data;

/**
 * 单词导入结果 DTO。
 *
 * @author wisestar
 * @date 2026/8/30
 */
@Data
public class ImportResult {

	/** 导入总数 */
	private Integer total;

	/** 成功数 */
	private Integer success;

	/** 失败数 */
	private Integer failed;

	/** 失败原因列表 */
	private java.util.List<String> errors;

	public ImportResult() {
	}

	public ImportResult(Integer total, Integer success, Integer failed, java.util.List<String> errors) {
		this.total = total;
		this.success = success;
		this.failed = failed;
		this.errors = errors;
	}
}

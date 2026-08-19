package cn.wisestar.server.domain.dto.knowledge;

import lombok.Data;

/**
 * 批量导入结果视图。
 *
 * <p>imported：实际新增条数；skipped：跳过条数（归属名称未匹配或同归属下重名）。</p>
 *
 * @author wisestar
 * @date 2026/8/19
 */
@Data
public class ImportResultView {

	/** 新增条数 */
	private int imported;

	/** 跳过条数（归属未找到或重复） */
	private int skipped;

	public ImportResultView() {
	}

	public ImportResultView(int imported, int skipped) {
		this.imported = imported;
		this.skipped = skipped;
	}
}

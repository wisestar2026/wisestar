package cn.wisestar.server.domain.dto.knowledge;

import lombok.Data;

/**
 * 批量导入结果视图。
 *
 * <p>imported：实际新增条数；skipped：跳过总条数（= missingRequired + sectionNotFound + duplicate）。
 * 分类字段为可选统计：仅知识点导入填充，章节/小节导入不填（保持 null 即可）。</p>
 *
 * @author wisestar
 * @date 2026/8/19
 */
@Data
public class ImportResultView {

	/** 新增条数 */
	private int imported;

	/** 跳过总条数（归属未找到或重复） */
	private int skipped;

	/** 跳过-必填项缺失行数 */
	private Integer missingRequired;

	/** 跳过-归属（学科/章节/小节名称）未匹配行数 */
	private Integer sectionNotFound;

	/** 跳过-同小节下知识点重名行数 */
	private Integer duplicate;

	public ImportResultView() {
	}

	public ImportResultView(int imported, int skipped) {
		this.imported = imported;
		this.skipped = skipped;
	}
}

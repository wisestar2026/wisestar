package cn.wisestar.server.domain.dto.archive;

import lombok.Data;

/**
 * 学员档案概览视图（学员列表「档案」入口的角标数据）。
 *
 * @author wisestar
 * @date 2026/10/3
 */
@Data
public class StudentArchiveOverviewView {

	private String studentId;

	private String studentName;

	/** 是否已建立本学期档案 */
	private boolean hasArchive;

	/** 当前学期键 */
	private String semester;

	/** 学期名称 */
	private String termLabel;

	/** 当前薄弱知识点数 */
	private int weakCount;

	/** 上课记录数 */
	private int recordCount;

	/** 最近一次上课日期 */
	private String lastRecordDate;

	/** 学期报告状态 none/draft/final */
	private String reportStatus;

}

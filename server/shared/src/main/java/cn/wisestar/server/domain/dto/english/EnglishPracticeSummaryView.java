package cn.wisestar.server.domain.dto.english;

import lombok.Data;

import java.util.Date;

/**
 * 英语练习成绩概览（最高分/上次/累计错题）。
 *
 * @author wisestar
 * @date 2026/10/23
 */
@Data
public class EnglishPracticeSummaryView {

	/** 所属模块 */
	private String module;

	/** 所属单元 */
	private String unit;

	/** 本次记录 ID（未落库为空） */
	private String recordId;

	/** 本次得分（0-100） */
	private Integer score;

	/** 历史最高分（0-100） */
	private Integer bestScore;

	/** 最近一次得分（含本次） */
	private Integer lastScore;

	/** 上一次得分（不含本次；无历史为空） */
	private Integer previousScore;

	/** 累计作答次数 */
	private Integer attempts;

	/** 未消灭错题数 */
	private Integer wrongCount;

	/** 最近一次作答时间 */
	private Date lastTime;

}

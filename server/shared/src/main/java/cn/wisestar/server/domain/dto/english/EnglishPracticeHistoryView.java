package cn.wisestar.server.domain.dto.english;

import lombok.Data;

import java.util.Date;

/**
 * 英语练习历史记录条目（学习中心卡片「查看过往记录」列表用，不含逐题明细）。
 *
 * @author wisestar
 * @date 2026/10/23
 */
@Data
public class EnglishPracticeHistoryView {

	private String id;

	private String module;

	private String unit;

	private Integer score;

	private Integer correct;

	private Integer total;

	private Integer accuracy;

	private Integer durationSeconds;

	private Date createAt;

}

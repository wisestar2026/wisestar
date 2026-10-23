package cn.wisestar.server.domain.dto.english;

import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 英语练习作答记录（用于查看上一次答题情况）。
 *
 * @author wisestar
 * @date 2026/10/23
 */
@Data
public class EnglishPracticeRecordView {

	private String id;

	private String module;

	private String unit;

	private Integer score;

	private Integer correct;

	private Integer total;

	private Integer accuracy;

	private Integer durationSeconds;

	private Date createAt;

	private List<EnglishPracticeItemView> items = new ArrayList<>();

}

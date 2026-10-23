package cn.wisestar.server.domain.dto.english;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 英语练习交卷上报（阅读理解等前端判分页面使用）。
 *
 * @author wisestar
 * @date 2026/10/23
 */
@Data
public class EnglishPracticeRecordRequest {

	/** 练习模块：reading/unit/grammar/sentence/word */
	private String module;

	private String version;

	private String grade;

	private String term;

	private String unit;

	/** 用时（秒） */
	private Integer durationSeconds;

	/** 逐题作答 */
	private List<EnglishPracticeItemView> items = new ArrayList<>();

}

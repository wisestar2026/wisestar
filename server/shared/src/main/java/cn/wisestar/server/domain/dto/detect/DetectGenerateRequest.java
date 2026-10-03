package cn.wisestar.server.domain.dto.detect;

import lombok.Data;

import java.util.List;

/**
 * 知识点检测组卷请求。
 *
 * @author wisestar
 * @date 2026/10/3
 */
@Data
public class DetectGenerateRequest {

	/** 学科ID（t_subject.id） */
	private String subjectId;

	/** 年级（如 三年级） */
	private String grade;

	/** 册别（上/下 或 上册/下册，服务端归一化比较） */
	private String term;

	/** 勾选的章节（单元）ID 列表 */
	private List<String> chapterIds;

	/** 抽题数量（默认 10，上限 50） */
	private Integer questionCount;

	/** 难度过滤（easy/medium/hard，可空表示不限） */
	private String difficulty;

	/** 题型过滤（Radio/FillBlank 等，可空表示不限） */
	private List<String> types;

}

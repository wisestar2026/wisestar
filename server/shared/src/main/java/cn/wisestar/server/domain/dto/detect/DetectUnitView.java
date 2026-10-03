package cn.wisestar.server.domain.dto.detect;

import lombok.Data;

/**
 * 知识点检测可选单元视图。
 *
 * @author wisestar
 * @date 2026/10/3
 */
@Data
public class DetectUnitView {

	/** 章节（单元）ID */
	private String id;

	/** 单元名称（如 Welcome / Unit 1 / 100以内加减法） */
	private String name;

	/** 年级 */
	private String grade;

	/** 册别（上/下 或 上册/下册，原样返回） */
	private String term;

	/** 可用题目数量 */
	private Integer questionCount;

}

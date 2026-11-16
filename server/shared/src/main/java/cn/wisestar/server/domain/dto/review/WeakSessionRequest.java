package cn.wisestar.server.domain.dto.review;

import lombok.Data;

/**
 * 攻克训练组卷条件。
 *
 * <p>训练默认 8 项，范围 6~10；输出类作答方式占比不低于 40%。</p>
 *
 * @author wisestar
 * @date 2026/11/16
 */
@Data
public class WeakSessionRequest {

	/** 学科ID，为空时覆盖全部学科 */
	private String subjectId;

	/** 模块，为空时覆盖全部模块 */
	private String module;

	/** 版本，英语模块使用 */
	private String version;

	/** 年级 */
	private String grade;

	/** 册别 */
	private String term;

	/** 单元 */
	private String unit;

	/** 题量，默认 8，范围 6~10 */
	private Integer size;

}

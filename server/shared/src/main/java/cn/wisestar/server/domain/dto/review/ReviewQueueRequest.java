package cn.wisestar.server.domain.dto.review;

import lombok.Data;

import java.util.List;

/**
 * 复习队列查询条件。
 *
 * <p>{@code modes} 为空时按「到期 + 薄弱 + 新项」取数；{@code unit} 为空时覆盖本册全部单元。
 * 新项配额固定不超过 {@code limit} 的 30%。</p>
 *
 * @author wisestar
 * @date 2026/11/16
 */
@Data
public class ReviewQueueRequest {

	/** 模块，为空时覆盖全部模块 */
	private String module;

	/** 学科ID，知识点级必填 */
	private String subjectId;

	/** 版本，英语模块使用 */
	private String version;

	/** 年级 */
	private String grade;

	/** 册别 */
	private String term;

	/** 单元，为空时覆盖整册 */
	private String unit;

	/** 取数上限，默认 20，最大 100 */
	private Integer limit;

	/** 来源过滤：due/weak/new，空表示全部 */
	private List<String> modes;

}

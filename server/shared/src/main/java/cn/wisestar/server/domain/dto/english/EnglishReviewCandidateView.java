package cn.wisestar.server.domain.dto.english;

import lombok.Data;

import java.util.List;

/**
 * 英语复习候选视图：某单元内待复习知识点及其可用考察角度。
 *
 * <p>供单元复习入口与复习角标使用。返回的知识点全部属于请求单元，
 * 前端据此展示角标数字并按知识点取派生题，不复用全局错题本的原始题目。</p>
 *
 * @author wisestar
 * @date 2026/11/05
 */
@Data
public class EnglishReviewCandidateView {

	/** 模块：word / sentence / grammar */
	private String module;

	/** 知识点 ID */
	private String knowledgeId;

	/** 知识点展示名（单词拼写 / 句子英文 / 语法标题） */
	private String name;

	/** 所属单元 */
	private String unit;

	/** 所属小节 */
	private String section;

	/** 掌握状态：familiar / ordinary / unfamiliar */
	private String state;

	/** 掌握状态中文标签：熟练 / 一般 / 陌生 */
	private String stateLabel;

	/** 是否在薄弱池 */
	private Boolean weak;

	/** 薄弱原因编码 */
	private String weakReason;

	/** 待复习原因：weak（薄弱）/ wrong（本单元做错）/ due（间隔到期） */
	private List<String> reasons;

	/** 可用的考察角度（本次推荐的角度排在首位） */
	private List<String> angles;

	/** 推荐角度中文标签 */
	private String nextAngleLabel;
}

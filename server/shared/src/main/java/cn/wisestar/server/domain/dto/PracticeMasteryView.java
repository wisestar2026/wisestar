package cn.wisestar.server.domain.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 练习掌握度汇总视图（学员端学习页/交卷结果页数据源）。
 *
 * <p><b>范围锚定</b>：按小节（sectionId）或知识点（knowledgePointId）二选一查询，
 * 该范围内学员全部练习历史（practice record/detail）作为掌握度统计底座：
 * 掌握度 = 判分题次中答对比例（全部历史累计）；「上次结果」取该范围最近一次练习记录。</p>
 *
 * <p><b>知识点维度</b>：kps 聚合该范围内各知识点的历史作答与正确率
 * （明细题按 t_knowledge_point_question 绑定归属知识点），供「知识点掌握总结」展示。</p>
 *
 * @author wisestar
 * @date 2026/9/8
 */
@Data
public class PracticeMasteryView {

	/**
	 * 范围类型：section 小节 / knowledgePoint 知识点
	 */
	private String scopeType;

	/**
	 * 范围ID（t_section.id / t_knowledge_point.id）
	 */
	private String scopeId;

	/**
	 * 范围名称（小节名/知识点名）
	 */
	private String scopeName;

	/**
	 * 范围内练习次数（锚定记录数）
	 */
	private Long practiceCount;

	/**
	 * 判分题次合计（明细 is_correct ∈ {0,1}）
	 */
	private Long questionCount;

	/**
	 * 答对题次合计
	 */
	private Long rightCount;

	/**
	 * 掌握度（答对题次 ÷ 判分题次 × 100，取整）
	 */
	private Integer accuracy;

	/**
	 * 上次练习结果（最近一次锚定练习记录；无记录为 null）
	 */
	private LastRecord lastRecord;

	/**
	 * 知识点维度掌握总结（按答题次数倒序）
	 */
	private List<KpMastery> kps = new ArrayList<>();

	/**
	 * 上次练习结果摘要。
	 */
	@Data
	public static class LastRecord {

		/** 练习记录ID（t_practice_record.id） */
		private String recordId;

		/** 练习模式：special/exam/random */
		private String mode;

		/** 得分 */
		private Double score;

		/** 总分 */
		private Double totalScore;

		/** 正确率（0-100，总分>0 时按得分占比） */
		private Integer rate;

		/** 总题数 */
		private Integer totalQuestions;

		/** 答对题数 */
		private Integer correctCount;

		/** 练习用时（毫秒） */
		private Long durationMs;

		/** 提交时间 */
		private Date createAt;
	}

	/**
	 * 单知识点掌握总结。
	 */
	@Data
	public static class KpMastery {

		/** 知识点ID */
		private String knowledgePointId;

		/** 知识点名称 */
		private String knowledgePointName;

		/** 该知识点累计练习题次 */
		private Long attempts;

		/** 该知识点累计答对题次 */
		private Long right;

		/** 该知识点掌握度（答对 ÷ 练习 × 100，取整） */
		private Integer accuracy;

		/** 最近一次练习时间 */
		private Date lastPracticedAt;
	}
}

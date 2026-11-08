package cn.wisestar.server.domain.dto.student;

import lombok.Data;

/**
 * 学习总结视图（学员本人 / 教师查看）。
 *
 * @author wisestar
 * @date 2026/9/11
 */
@Data
public class StudySummaryView {

	/** 总结ID */
	private String id;

	/** 学员ID */
	private String studentId;

	/** 学员姓名（教师查看时回填） */
	private String studentName;

	/** 总结日期 yyyy-MM-dd */
	private String summaryDate;

	/** 触发总结的会话ID */
	private String sessionId;

	/** 总结正文 */
	private String content;

	/** 生成模型，规则模板为 rule */
	private String model;

	/** 状态：success/failed */
	private String status;

	/** 当日累计学习时长（毫秒） */
	private Long durationMs;

	/** 当日生成时间 yyyy-MM-dd HH:mm */
	private String createTime;

	/** 当日练习次数 */
	private int practiceCount;

	/** 当日答题数 */
	private int questionCount;

	/** 当日答对数 */
	private int correctCount;

	/** 当日正确率 0-100 */
	private int accuracy;

	/** 当日错题数 */
	private int wrongCount;

	/** 当日获得学海积分 */
	private int points;

	/** 当日获得学习币 */
	private int coins;

	/** 当日覆盖知识点数 */
	private int knowledgeCount;

	/** 当日平均掌握度 0-100 */
	private int avgMastery;

	/** 当前薄弱知识点名称 */
	private java.util.List<String> weakNames;

	/** 当日强化的知识点名称（当日有练习进度的知识点） */
	private java.util.List<String> strengthenedNames;

	/** 上一次有练习的日期（yyyy-MM-dd，无历史时为 null） */
	private String lastStudyDate;

	/** 上一次练习的正确率 0-100 */
	private int lastAccuracy;

	/** 与前一次相比的正确率变化（百分点，正数为进步；无对比时为 0） */
	private int accuracyDelta;

	/** 是否具备前后对比数据 */
	private boolean hasComparison;

	/** 近 7 天练习次数 */
	private int weekPracticeCount;

	/** 近 7 天答题数 */
	private int weekQuestionCount;

	/** 近 7 天正确率 0-100 */
	private int weekAccuracy;

	/** 已攻克薄弱知识点数量（历史累计） */
	private int conqueredCount;

	/** 已攻克薄弱知识点名称（最多 3 个，供家长版展示） */
	private java.util.List<String> conqueredNames;

	/** 连续学习天数 */
	private int streakDays;

	/** 本次学习集中涉及的单元/小节（用于家长版叙述） */
	private String topicLabel;

	/** 家长版叙述（三段式：学了什么 / 进步在哪 / 下一步），供档案与首页直接展示 */
	private String parentContent;

}

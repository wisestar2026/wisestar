package cn.wisestar.server.domain.dto.archive;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 上课记录自动草稿视图。
 *
 * <p>老师新增某日上课记录时，系统自动拉取当日学习数据（时长/题量/正确率/积分学币/
 * 学习总结）与当前薄弱、当日强化知识点，作为草稿供老师编辑定稿。</p>
 *
 * @author wisestar
 * @date 2026/10/3
 */
@Data
public class ArchiveRecordDraftView {

	/** 上课日期 yyyy-MM-dd */
	private String date;

	/** 建议主题 */
	private String title;

	/** 当日学习情况文本（优先 AI 总结，缺失时规则模板；可空） */
	private String studySummary;

	/** 当日在线学习时长（分钟） */
	private long durationMinutes;

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

	/** 当日平均掌握度 */
	private int avgMastery;

	/** 当日强化的知识点名称（建议填入「强化的知识点」） */
	private List<String> strengthenedKps = new ArrayList<>();

	/** 当前薄弱知识点名称（建议填入「暴露的弱点」） */
	private List<String> weakNames = new ArrayList<>();

}

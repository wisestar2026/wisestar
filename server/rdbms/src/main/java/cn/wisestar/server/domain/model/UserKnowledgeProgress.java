package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.model.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 知识点掌握度实体（对应数据库表 t_user_knowledge_progress，按学科+版本独立）。
 *
 * @author wisestar
 * @date 2026/9/10
 */
@Data
@TableName("t_user_knowledge_progress")
@EqualsAndHashCode(callSuper = false)
public class UserKnowledgeProgress extends BaseModel {

	/** 学员ID */
	private String userId;

	/** 学科ID */
	private String subjectId;

	/** 教材版本ID */
	private String versionId;

	/** 章节ID */
	private String chapterId;

	/** 知识点ID */
	private String knowledgePointId;

	/** 掌握度 0-100 */
	private Integer mastery;

	/** 评级标签 */
	private String level;

	/** 练习会话次数 */
	private Integer times;

	/** 累计题数 */
	private Integer totalCount;

	/** 累计正确题数 */
	private Integer correctCount;

	/** 最近一次正确率 0-100 */
	private Integer lastCorrectRate;

	/** 最近 5 次练习正确率（逗号分隔，新到旧，用于加权掌握度） */
	private String recentRates;

	/** 最近练习时间 */
	private Date lastPracticeAt;

}

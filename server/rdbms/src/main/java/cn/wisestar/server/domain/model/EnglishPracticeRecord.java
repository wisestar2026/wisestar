package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.model.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 英语练习作答记录实体（对应数据库表 t_english_practice_record）。
 *
 * @author wisestar
 * @date 2026/10/23
 */
@Data
@TableName("t_english_practice_record")
@EqualsAndHashCode(callSuper = false)
public class EnglishPracticeRecord extends BaseModel {

	/** 学员账号 ID */
	private String userId;

	/** 练习模块 reading/unit/grammar/sentence/word */
	private String module;

	/** 教材版本 */
	private String version;

	/** 年级 */
	private String grade;

	/** 册别 */
	private String term;

	/** 单元 */
	private String unit;

	/** 本次百分制得分 */
	private Integer score;

	/** 答对题数 */
	private Integer correctCount;

	/** 总题数 */
	private Integer totalCount;

	/** 正确率（0-100） */
	private Integer accuracy;

	/** 用时（秒） */
	private Integer durationSeconds;

	/** 逐题作答快照 JSON */
	private String details;

}

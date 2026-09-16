package cn.wisestar.server.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 英语学习日志实体（对应数据库表 t_english_learning_log）。
 *
 * <p>该表沿用早期字段命名（duration / correct_count / created_at），
 * 不继承 BaseModel。</p>
 *
 * @author wisestar
 * @date 2026/9/15
 */
@Data
@TableName("t_english_learning_log")
public class EnglishLearningLog {

	/** 主键 */
	@TableId(type = IdType.ASSIGN_ID)
	private String id;

	/** 用户 ID */
	private String userId;

	/** 类型 word / sentence */
	private String type;

	/** 内容 ID（单词或句子 ID） */
	private String contentId;

	/** 学习时长（秒） */
	private Integer duration;

	/** 正确数 */
	private Integer correctCount;

	/** 创建时间 */
	private Date createdAt;

}

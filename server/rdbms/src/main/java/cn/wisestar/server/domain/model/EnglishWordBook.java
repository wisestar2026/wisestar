package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.model.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户单词本实体（对应数据库表 t_english_word_book）。
 *
 * @author wisestar
 * @date 2026/8/30
 */
@Data
@TableName("t_english_word_book")
@EqualsAndHashCode(callSuper = false)
public class EnglishWordBook extends BaseModel {

	/** 用户 ID */
	private String userId;

	/** 单词 ID */
	private String wordId;

	/** 熟练度（0-未学习 1-生疏 2-熟悉 3-熟练 4-精通） */
	private Integer familiarity;

	/** 下次复习时间 */
	private java.util.Date nextReviewTime;

}

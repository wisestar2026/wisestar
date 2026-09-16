package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.model.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户句子本实体（对应数据库表 t_english_sentence_book）。
 *
 * <p>记录学员对句子的熟练度与复习调度信息。</p>
 *
 * @author wisestar
 * @date 2026/9/15
 */
@Data
@TableName("t_english_sentence_book")
@EqualsAndHashCode(callSuper = false)
public class EnglishSentenceBook extends BaseModel {

	/** 用户 ID */
	private String userId;

	/** 句子 ID */
	private String sentenceId;

	/** 熟练度（0-未学习 1-生疏 2-熟悉 3-熟练 4-精通） */
	private Integer familiarity;

	/** 累计答对次数 */
	private Integer correctCount;

	/** 累计答错次数 */
	private Integer wrongCount;

	/** 最近复习时间 */
	private java.util.Date lastReviewTime;

	/** 下次复习时间 */
	private java.util.Date nextReviewTime;

}

package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.model.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 英语知识点学习状态实体（对应数据库表 t_english_learning_state）。
 *
 * 【类职责】
 * 记录学员在某知识点（单词 / 句子 / 语法点）上的三态掌握状态、连续答对计数、
 * 输出类题型答对次数与薄弱标记，供通关判定与薄弱点攻克使用。
 *
 * 【与既有表的关系】
 * t_english_word_book / t_english_sentence_book / t_english_grammar_book 继续保存
 * 熟练度与下次复习时间；本表只回答「这个知识点现在是熟练、一般还是陌生，是否在薄弱池」。
 * 两套字段各自演进，互不覆盖。
 *
 * 【被谁调用】
 * EnglishLearningRuleService、EnglishStudentServiceImpl、EnglishWordServiceImpl。
 *
 * @author wisestar
 * @date 2026/11/05
 */
@Data
@TableName("t_english_learning_state")
@EqualsAndHashCode(callSuper = false)
public class EnglishLearningState extends BaseModel {

	/** 用户 ID */
	private String userId;

	/** 模块：word / sentence / grammar */
	private String module;

	/** 知识点 ID（单词 ID / 句子 ID / 语法点 ID） */
	private String knowledgeId;

	/** 掌握状态：familiar / ordinary / unfamiliar */
	private String state;

	/** 连续答对计数（攻克判定用，答错清零） */
	private Integer consecutiveCorrect;

	/** 连续答错计数（薄弱判定用，答对清零） */
	private Integer consecutiveWrong;

	/** 输出类题型答对次数（默写 / 填空 / 组句 / 改写） */
	private Integer outputCorrectCount;

	/** 最近一次作答的考察角度 */
	private String lastAngle;

	/** 记忆阶段答题总数 */
	private Integer memoryTotal;

	/** 记忆阶段答对总数 */
	private Integer memoryCorrect;

	/** 练习阶段答题总数 */
	private Integer practiceTotal;

	/** 练习阶段答对总数 */
	private Integer practiceCorrect;

	/** 运用阶段答题总数 */
	private Integer useTotal;

	/** 运用阶段答对总数 */
	private Integer useCorrect;

	/** 是否在薄弱池 */
	private Integer weakFlag;

	/** 薄弱原因：consecutive_wrong / accuracy / output_error */
	private String weakReason;
}

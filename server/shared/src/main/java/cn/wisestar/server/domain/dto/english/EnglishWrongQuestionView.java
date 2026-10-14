package cn.wisestar.server.domain.dto.english;

import cn.wisestar.server.domain.dto.student.StudentQuestionView;
import lombok.Data;

import java.util.Date;

/**
 * 英语复习错题视图（来自学员检测记录 t_detect_record 聚合）。
 *
 * <p>仅用于「英语智能复习」错题回顾：题目快照已剥离标准答案与解析，
 * 供学员对照题干复习，不展示答案解析（避免直接记答案）。</p>
 *
 * @author wisestar
 * @date 2026/10/14
 */
@Data
public class EnglishWrongQuestionView {

	/** 题目 ID */
	private String questionId;

	/** 题型 */
	private String questionType;

	/** 所属单元（检测明细快照） */
	private String chapter;

	/** 知识点标签（检测明细快照） */
	private String knowledgePoint;

	/** 累计做错次数（跨检测记录聚合） */
	private Integer wrongCount;

	/** 最近一次做错时间 */
	private Date lastWrongTime;

	/** 原题（题干 + 选项，已剥离标准答案与解析） */
	private StudentQuestionView question;

}

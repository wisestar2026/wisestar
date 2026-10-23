package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.model.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 学员举手反馈实体（对应数据库表 t_hand_raise）。
 *
 * <p><b>用途</b>：学员在题目页点击「举手」提交反馈，按类型分流到两条处理链路：</p>
 * <ul>
 *   <li>{@code explain} 寻求老师讲解 → 学管师/教师端「举手提醒」，查看学员当时的作答（多选界面）；</li>
 *   <li>{@code question_error} 题干有误 / {@code answer_error} 答案有误 → 题目管理端（教研）待处理任务，
 *       便于筛查并修改题目。</li>
 * </ul>
 *
 * <p>提交时服务端按 questionId 回填题干、选项、标准答案与解析快照，学员作答与选项 ID 由学员端上报，
 * 二者一并落库，保证题目后续被修改后仍能还原学员当时的作答现场。</p>
 *
 * @author wisestar
 * @date 2026/10/22
 */
@Data
@TableName("t_hand_raise")
@EqualsAndHashCode(callSuper = false)
public class HandRaise extends BaseModel {

	/** 反馈类型：explain 寻求老师讲解 */
	public static final String TYPE_EXPLAIN = "explain";

	/** 反馈类型：question_error 题干有误 */
	public static final String TYPE_QUESTION_ERROR = "question_error";

	/** 反馈类型：answer_error 答案有误 */
	public static final String TYPE_ANSWER_ERROR = "answer_error";

	/** 状态：待处理 */
	public static final int STATUS_PENDING = 0;

	/** 状态：已处理 */
	public static final int STATUS_HANDLED = 2;

	/** 状态：已忽略 */
	public static final int STATUS_IGNORED = 3;

	/** 学员ID（t_student.id） */
	private String studentId;

	/** 反馈类型 explain / question_error / answer_error */
	private String raiseType;

	/** 处理状态 0待处理 2已处理 3已忽略 */
	private Integer status;

	/** 题目ID（t_template.id） */
	private String questionId;

	/** 题型（Radio/Checkbox/Judge/FillBlank/MultipleBlank/Text） */
	private String questionType;

	/** 学科 */
	private String subject;

	/** 章节 */
	private String chapter;

	/** 知识点（多个用顿号连接） */
	private String knowledgePoint;

	/** 难度 */
	private String difficulty;

	/** 题干快照 */
	private String questionContent;

	/** 选项快照 JSON：[{"id":"...","title":"..."}] */
	private String optionsJson;

	/** 学员作答展示文本 */
	private String studentAnswer;

	/** 学员选中选项ID（逗号分隔，选项题才有值） */
	private String studentOptionIds;

	/** 标准答案展示文本快照 */
	private String correctAnswer;

	/** 正确选项ID（逗号分隔，选项题才有值） */
	private String correctOptionIds;

	/** 解析快照 */
	private String answerAnalysis;

	/** 提交时所在页面标识 */
	private String page;

	/** 小节ID（练习上下文） */
	private String sectionId;

	/** 学员补充说明 */
	private String remark;

	/** 处理人ID */
	private String handlerId;

	/** 处理人姓名 */
	private String handlerName;

	/** 处理时间 */
	private Date handleAt;

	/** 处理备注 */
	private String handleNote;

}

package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.model.BaseModel;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 学习会话（t_study_session）。
 *
 * <p>学员端周期性心跳续会话：距上次心跳不超过 30 分钟视为同一次会话，
 * 否则开启新会话；{@code durationMs} 累计会话时长，达到 60 分钟触发当日学习总结。</p>
 *
 * @author wisestar
 * @date 2026/9/11
 */
@Data
@TableName("t_study_session")
@EqualsAndHashCode(callSuper = false)
public class StudySession extends BaseModel {

	/** 学员ID */
	private String studentId;

	/** 学科ID（可为空） */
	private String subjectId;

	/** 会话日期 yyyy-MM-dd */
	private String sessionDate;

	/** 会话开始时间 */
	@TableField("start_at")
	private Date startAt;

	/** 最近心跳时间 */
	private Date lastHeartbeatAt;

	/** 会话结束时间 */
	private Date endAt;

	/** 累计时长（毫秒） */
	private Long durationMs;

	/** 状态：active/ended */
	private String status;

}

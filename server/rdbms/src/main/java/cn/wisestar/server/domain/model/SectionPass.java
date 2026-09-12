package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.model.BaseModel;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 小节通关记录（t_section_pass）。
 *
 * <p>每学员每小节唯一一条：累加交卷次数，保留历史最佳正确率/星级，
 * 首次通关时记录 firstPassAt 且 passed 保持 true（不回退）。</p>
 *
 * @author wisestar
 * @date 2026/9/11
 */
@Data
@TableName("t_section_pass")
@EqualsAndHashCode(callSuper = false)
public class SectionPass extends BaseModel {

	/** 学员ID */
	@TableField("user_id")
	private String userId;

	/** 小节ID */
	@TableField("section_id")
	private String sectionId;

	/** 历史最佳正确率（百分比） */
	private Integer bestRate;

	/** 历史最佳得分 */
	private Double bestScore;

	/** 历史最佳星级 0-5 */
	private Integer stars;

	/** 是否已通关 */
	private Boolean passed;

	/** 交卷次数 */
	private Integer attemptCount;

	/** 首次通关时间 */
	private Date firstPassAt;

	/** 最近交卷时间 */
	private Date lastAttemptAt;

}

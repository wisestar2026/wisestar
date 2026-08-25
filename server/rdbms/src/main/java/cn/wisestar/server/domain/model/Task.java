package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.model.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 今日任务实体（对应数据库表 t_task，老师后台布置、学员端呈现）。
 *
 * @author wisestar
 * @date 2026/8/23
 */
@Data
@TableName("t_task")
@EqualsAndHashCode(callSuper = false)
public class Task extends BaseModel {

	/** 任务名称 */
	private String name;

	/** 任务描述 */
	private String description;

	/** 任务日期（YYYY-MM-DD） */
	private String taskDate;

	/** 内容类型 practice练习 / knowledge_point知识点 */
	private String contentType;

	/** 关联内容ID（练习=t_repo.id / 知识点=t_knowledge_point.id） */
	private String contentId;

	/** 状态 1发布 0停用 */
	private Integer status;

	/** 排序 */
	private Integer sort;

}

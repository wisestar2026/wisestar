package cn.wisestar.server.core.model;

import cn.wisestar.server.core.constant.AppConsts;
import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.Date;

/**
 * 所有业务实体（model）的公共基础父类。
 *
 * 【类职责】
 * 统一提供数据库表公共字段：主键 id、创建/更新时间、创建/更新人、逻辑删除标记。
 * rdbms 模块下 domain.model 中所有实体均继承本类（Answer 覆写了 id 生成策略）。
 *
 * 【依赖什么】
 * - MyBatis-Plus 注解体系：@TableId(ASSIGN_ID 雪花ID) / @TableField(fill 自动填充) /
 *   @TableLogic 逻辑删除
 * - AppConsts.COLUMN_IS_DELETED：逻辑删除列名常量
 * - MyMetaObjectHandler：实现 FieldFill.INSERT / UPDATE 时 createAt/createBy/updateAt/updateBy
 *   的自动填充逻辑
 *
 * 【核心数据流】
 * 实体入库时 MyBatis-Plus 通过 MyMetaObjectHandler 自动填充审计字段；
 * 查询时 @TableLogic 自动追加 is_deleted = 0 条件；@TableField(select=false) 使 deleted 列
 * 默认不参与查询回填（避免对象里出现冗余字段），逻辑删除的恢复/查询走 Mapper 自定义 SQL。
 */
@Data
public class BaseModel {

	/**
	 * 主键（对应各表 id 列，ASSIGN_ID 雪花算法生成，全局唯一、趋势递增）。
	 * 注意：Answer 实体覆写为 ASSIGN_UUID + UUID 字符串；Project 实体覆写为 NanoId 短码。
	 */
	@TableId(type = IdType.ASSIGN_ID)
	private String id;

	/**
	 * 创建时间（对应列 create_at，FieldFill.INSERT 插入时自动填充当前时间）。
	 * 答卷列表默认按此字段倒序排列。
	 */
	@TableField(fill = FieldFill.INSERT)
	private Date createAt;

	/**
	 * 创建人（对应列 create_by，FieldFill.INSERT 插入时自动填充当前登录用户ID）。
	 * 很多"只查自己的数据"的过滤条件（如 AnswerDetail 按学生归属）都基于该字段。
	 */
	@TableField(fill = FieldFill.INSERT)
	private String createBy;

	/**
	 * 更新时间（对应列 update_at，FieldFill.UPDATE 更新时自动填充当前时间）。
	 */
	@TableField(fill = FieldFill.UPDATE)
	private Date updateAt;

	/**
	 * 更新人（对应列 update_by，FieldFill.UPDATE 更新时自动填充当前登录用户ID）。
	 */
	@TableField(fill = FieldFill.UPDATE)
	private String updateBy;

	/**
	 * 默认逻辑删除标记（对应列 is_deleted）：false(0) 有效、true(1) 已删除。
	 * @TableLogic：普通查询/更新自动追加 is_deleted = 0 条件；
	 * @JsonIgnore：序列化到前端时隐藏该字段；
	 * select=false：查询结果不回填该列。
	 * 回收站恢复/物理销毁走各 Mapper 自定义 SQL（如 AnswerMapper.restoreAnswer/batchDestroy）。
	 */
	@TableLogic
	@JsonIgnore
	@TableField(value = AppConsts.COLUMN_IS_DELETED, select = false)
	private Boolean deleted = false;

}

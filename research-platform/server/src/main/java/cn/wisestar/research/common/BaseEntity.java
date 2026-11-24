package cn.wisestar.research.common;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;

import lombok.Data;

@Data
public abstract class BaseEntity {

	@TableId(type = IdType.ASSIGN_ID)
	private String id;

	@TableField(fill = FieldFill.INSERT)
	private LocalDateTime createAt;

	@TableField(fill = FieldFill.INSERT)
	private String createBy;

	@TableField(fill = FieldFill.INSERT_UPDATE)
	private LocalDateTime updateAt;

	@TableField(fill = FieldFill.INSERT_UPDATE)
	private String updateBy;

	@TableLogic
	private Integer isDeleted;

}

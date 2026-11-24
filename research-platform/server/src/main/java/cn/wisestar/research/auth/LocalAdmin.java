package cn.wisestar.research.auth;

import com.baomidou.mybatisplus.annotation.TableName;

import cn.wisestar.research.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("t_research_admin")
@EqualsAndHashCode(callSuper = false)
public class LocalAdmin extends BaseEntity {

	private String username;

	private String password;

	private String name;

	private String role;

	private Integer status;

}

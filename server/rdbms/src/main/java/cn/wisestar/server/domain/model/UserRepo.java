package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.model.BaseModel;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 学员-题库分配（练习题库分配记录）
 *
 * <p><b>所属模块</b>：rdbms 模块数据模型包（cn.wisestar.server.domain.model）。</p>
 * <p><b>表</b>：t_user_repo。记录老师为学员手动分配的可练习题库，
 * 学员端「我的题库」= 手动分配记录 ∪ 按标签自动匹配的题库。</p>
 *
 * <p>分配方式（assignType）：manual=老师手动分配；auto=系统按标签自动分配
 * （auto 类型为查询时动态计算，不落库，本表仅存 manual）。</p>
 *
 * @author zhanghaiyang
 */
@Data
@TableName(value = "t_user_repo", autoResultMap = true)
@EqualsAndHashCode(callSuper = false)
public class UserRepo extends BaseModel {

	/**
	 * 学员用户 ID（t_user.id）
	 */
	@TableField("user_id")
	private String userId;

	/**
	 * 题库 ID（t_repo.id）
	 */
	@TableField("repo_id")
	private String repoId;

	/**
	 * 分配方式：manual 手动 / auto 标签自动
	 */
	@TableField("assign_type")
	private String assignType;

}

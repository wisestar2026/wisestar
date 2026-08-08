package cn.wisestar.server.domain.dto;

import lombok.Data;

import java.util.Date;

/**
 * 学员-题库分配记录视图（管理端查询分配列表用）
 *
 * <p><b>所属模块</b>：shared 模块 DTO 包（cn.wisestar.server.domain.dto）。</p>
 * <p><b>功能</b>：管理端「题库分配」页面展示某学员已分配的题库列表，
 * 含学员信息与题库信息，便于增删分配。</p>
 *
 * @author zhanghaiyang
 */
@Data
public class RepoAssignView {

	/**
	 * 分配记录 ID（删除分配时用）
	 */
	private String id;

	/**
	 * 学员用户 ID
	 */
	private String userId;

	/**
	 * 学员姓名
	 */
	private String userName;

	/**
	 * 学员登录账号
	 */
	private String username;

	/**
	 * 题库 ID
	 */
	private String repoId;

	/**
	 * 题库名称
	 */
	private String repoName;

	/**
	 * 分配方式：manual 手动 / auto 标签自动
	 */
	private String assignType;

	/**
	 * 分配时间
	 */
	private Date createAt;

}

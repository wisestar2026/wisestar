package cn.wisestar.server.domain.dto;

import lombok.Data;

import java.util.List;

/**
 * 学员-题库分配请求（分配/删除/学员标签）
 *
 * <p><b>所属模块</b>：shared 模块 DTO 包（cn.wisestar.server.domain.dto）。</p>
 * <p><b>功能</b>：管理端「题库分配」页面请求体——
 * assign：userId + repoIds 批量手动分配；delete：ids 删除分配记录；
 * userTags：userId + tags 保存学员标签（按标签自动分配用）。</p>
 *
 * @author zhanghaiyang
 */
@Data
public class RepoAssignRequest {

	/**
	 * 学员用户 ID（分配 / 保存标签用）
	 */
	private String userId;

	/**
	 * 题库 ID 列表（批量分配用）
	 */
	private List<String> repoIds;

	/**
	 * 分配记录 ID 列表（删除用）
	 */
	private List<String> ids;

	/**
	 * 学员标签数组（保存标签用，为空则清除）
	 */
	private String[] tags;

}

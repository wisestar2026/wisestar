package cn.wisestar.server.domain.dto.student;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 学员档案视图（个人中心用）。
 *
 * @author wisestar
 * @date 2026/9/10
 */
@Data
public class StudentProfileView {

	/** 姓名 */
	private String name;

	/** 头像 */
	private String avatar;

	/** 学号 */
	private String studentNo;

	/** 头衔等级 1-5 */
	private int titleLevel = 1;

	/** 头衔名称 */
	private String titleName = "初探者";

	/** 总学海积分 */
	private int points;

	/** 已解锁证书数（证书体系另行实现，暂 0） */
	private int certCount;

	/** 证书总数（证书体系另行实现，暂 0） */
	private int certTotal;

	/** 累计知识点数 */
	private int kps;

	/** 达标章节数 */
	private int chapters;

	/** 薄弱知识点数 */
	private int weak;

}

package cn.wisestar.server.domain.dto;

import lombok.Data;

/**
 * @author javahuang
 * @date 2021/8/31
 */
@Data
public class PageQuery {

	/** 单页最大条数上限，防止一次拉取过多数据拖垮数据库 */
	public static final int MAX_PAGE_SIZE = 1000;

	/** 默认每页条数 */
	public static final int DEFAULT_PAGE_SIZE = 20;

	private int current = 1;

	private int pageSize = DEFAULT_PAGE_SIZE;

	public int getPageSize() {
		// -1 表示不分页、拉取全量（前端下拉/词库等既有契约，仅内部与受信调用使用）
		if (this.pageSize == -1) {
			return Integer.MAX_VALUE;
		}
		if (this.pageSize < 1) {
			return DEFAULT_PAGE_SIZE;
		}
		return Math.min(this.pageSize, MAX_PAGE_SIZE);
	}

}

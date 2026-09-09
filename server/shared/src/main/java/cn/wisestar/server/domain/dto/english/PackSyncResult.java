package cn.wisestar.server.domain.dto.english;

import lombok.Data;

/**
 * 内容包同步到词库/语法库的结果统计。
 *
 * @author wisestar
 * @date 2026/9/9
 */
@Data
public class PackSyncResult {

	/** 新增单词数 */
	private int wordsAdded;

	/** 更新单词数（同单元同拼写已存在，刷新释义/音标/例句） */
	private int wordsUpdated;

	/** 同步语法条目数（同年级同标题覆盖更新） */
	private int grammarSynced;

	public PackSyncResult(int wordsAdded, int wordsUpdated, int grammarSynced) {
		this.wordsAdded = wordsAdded;
		this.wordsUpdated = wordsUpdated;
		this.grammarSynced = grammarSynced;
	}

}

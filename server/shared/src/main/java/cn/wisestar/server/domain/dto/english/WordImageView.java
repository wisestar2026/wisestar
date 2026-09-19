package cn.wisestar.server.domain.dto.english;

import lombok.Data;

/**
 * 单词配图结果视图 DTO（确认入库后返回）。
 *
 * @author wisestar
 * @date 2026/9/18
 */
@Data
public class WordImageView {

	/** 单词 ID */
	private String wordId;

	/** 单词拼写 */
	private String spell;

	/** 入库后的图片地址（/api/file?id=...） */
	private String imageUrl;

}

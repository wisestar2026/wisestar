package cn.wisestar.server.domain.dto.english;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 单词配图候选视图 DTO（仅用于审核选择，不落库）。
 *
 * @author wisestar
 * @date 2026/9/18
 */
@Data
public class WordImageCandidateView {

	/** 单词 ID */
	private String wordId;

	/** 单词拼写 */
	private String spell;

	/** 释义 */
	private String meaning;

	/** 候选图片地址列表 */
	private List<String> candidates = new ArrayList<>();

}

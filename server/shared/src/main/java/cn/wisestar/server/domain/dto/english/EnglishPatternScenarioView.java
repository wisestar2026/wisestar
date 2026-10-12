package cn.wisestar.server.domain.dto.english;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 英语重点句式情景练习视图 DTO（学员端「句式情景」单个情景）。
 *
 * <p>由 AI 依据指定单元的重点句式（{@code t_english_sentence}）实时生成，不落库；每个情景
 * 围绕一个生活场景，两名角色交替对话，其中若干轮次给出中文情景提示，学员需从候选项中
 * 选出符合该情景的重点句式完成交际。</p>
 *
 * @author wisestar
 * @date 2026/10/12
 */
@Data
public class EnglishPatternScenarioView {

	/** 情景标题（中文） */
	private String title;

	/** 情景说明（中文，1-2 句背景） */
	private String context;

	/** 本情景考查的重点句式（取自本单元句子） */
	private List<String> patterns = new ArrayList<>();

	/** 情景对话内容（按顺序的轮次） */
	private List<EnglishPatternTurn> turns = new ArrayList<>();

}

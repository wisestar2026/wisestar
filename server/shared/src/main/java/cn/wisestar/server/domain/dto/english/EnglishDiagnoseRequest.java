package cn.wisestar.server.domain.dto.english;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 英语学习诊断组卷请求（家长向诊断卷）。
 *
 * <p>按 D1→D2→D3 三档难度各取若干题，跨单元抽取知识点，
 * 用于生成可打印的学习诊断报告，帮助家长直观了解当前英语水平。</p>
 *
 * @author wisestar
 * @date 2026/10/17
 */
@Data
public class EnglishDiagnoseRequest {

	/** 教材版本 */
	private String version;

	/** 年级 */
	private String grade;

	/** 学期/册别 */
	private String term;

	/** 指定单元（为空时取当前年级/册别全部单元） */
	private List<String> units = new ArrayList<>();

}

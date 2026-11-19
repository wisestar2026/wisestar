package cn.wisestar.server.domain.dto.english;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 英语课程音频批量预生成统计。
 *
 * <p>按「来源 + 语速」拆分缓存命中、新增合成与失败数量，供命令行任务结束时输出汇总。</p>
 *
 * @author wisestar
 * @date 2026/11/19
 */
@Data
public class EnglishTtsPregenerateSummary {

	/** 去重后的音频总数（同一文本在不同语速下分别计数） */
	private int total;

	/** 本次新合成的音频数 */
	private int generated;

	/** 已命中缓存、本次跳过的音频数 */
	private int cached;

	/** 合成失败数 */
	private int failed;

	/** 总耗时（毫秒） */
	private long elapsedMillis;

	/** 分来源统计，key 形如「句子 @ 109」 */
	private Map<String, Source> sources = new LinkedHashMap<>();

	/**
	 * 取得指定来源的统计对象，不存在时创建。
	 *
	 * @param name 来源名称（含语速）
	 * @return 统计对象
	 */
	public Source source(String name) {
		return sources.computeIfAbsent(name, key -> {
			Source source = new Source();
			source.setName(key);
			return source;
		});
	}

	/**
	 * 单个来源/语速的统计。
	 */
	@Data
	public static class Source {

		/** 来源名称（含语速） */
		private String name;

		/** 去重后的文本数 */
		private int total;

		/** 新合成数 */
		private int generated;

		/** 缓存命中数 */
		private int cached;

		/** 失败数 */
		private int failed;

	}

}

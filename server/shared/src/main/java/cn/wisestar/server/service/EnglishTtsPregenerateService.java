package cn.wisestar.server.service;

import cn.wisestar.server.domain.dto.english.EnglishTtsPregenerateSummary;

/**
 * 英语课程音频批量预生成服务。
 *
 * <p>把课程里的句子、单词、课文跟读行与语法例句按学员端实际播放语速提前合成到本地缓存目录，
 * 发布时随部署包分发，线上首次点击即可直接命中缓存。</p>
 *
 * @author wisestar
 * @date 2026/11/19
 */
public interface EnglishTtsPregenerateService {

	/**
	 * 扫描当前内容库并补齐缺失的音频缓存。
	 *
	 * <p>已存在的缓存会跳过；相同文本在同一语速下只合成一次。调用前需确保服务端 TTS 引擎可用。</p>
	 *
	 * @return 预生成统计
	 */
	EnglishTtsPregenerateSummary pregenerate();

}

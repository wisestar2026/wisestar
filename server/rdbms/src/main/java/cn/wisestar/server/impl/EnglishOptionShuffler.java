package cn.wisestar.server.impl;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 英语练习选项打乱工具。
 *
 * <p>AI 生成与内容包固化时普遍把正确答案放在首位，直接展示会让学员形成「选 A」的固定套路。
 * 这里以题目内容为随机种子做确定性打乱：同一道题每次请求顺序一致，不同题目之间的正确项位置随机分布。</p>
 *
 * @author wisestar
 * @date 2026/10/23
 */
final class EnglishOptionShuffler {

	private EnglishOptionShuffler() {
	}

	/**
	 * 按内容种子打乱选项，并返回正确答案在打乱后的新下标。
	 *
	 * @param options 选项列表（原地打乱）
	 * @param answerIndex 打乱前的正确下标，非法值（负数或越界）时不做打乱
	 * @param key 题目内容种子，相同内容始终得到相同顺序
	 * @return 打乱后的正确下标；未打乱时原样返回
	 */
	static int shuffle(List<String> options, Integer answerIndex, String key) {
		if (options == null || options.size() < 2 || answerIndex == null
				|| answerIndex < 0 || answerIndex >= options.size()) {
			return answerIndex == null ? -1 : answerIndex;
		}
		String correct = options.get(answerIndex);
		Collections.shuffle(options, new Random(key == null ? 0L : key.hashCode()));
		return options.indexOf(correct);
	}

}

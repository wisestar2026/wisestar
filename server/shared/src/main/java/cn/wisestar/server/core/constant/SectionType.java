package cn.wisestar.server.core.constant;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 小节类型常量（t_section.type）。
 *
 * <p>普通小节（{@link #NORMAL}）承载学习内容与练习；章节测评
 * （{@link #EXAM}）是以小节形式挂在章节下的「测试节点」，本身无知识点，
 * 学员端进入后出题范围覆盖整章知识点。历史数据缺省按普通小节处理。</p>
 *
 * @author wisestar
 * @date 2026/10/08
 */
public final class SectionType {

	/** 普通小节（默认） */
	public static final String NORMAL = "normal";

	/** 章节测评（测试节点） */
	public static final String EXAM = "exam";

	private static final Set<String> VALID = new HashSet<>(Arrays.asList(NORMAL, EXAM));

	private SectionType() {
	}

	/**
	 * 归一化类型：空值或非法值统一回退为普通小节。
	 *
	 * @param type 原始类型
	 * @return 合法类型（normal/exam）
	 */
	public static String normalize(String type) {
		if (type == null) {
			return NORMAL;
		}
		String trimmed = type.trim().toLowerCase();
		return VALID.contains(trimmed) ? trimmed : NORMAL;
	}

	/**
	 * 判断是否为章节测评（测试节点）。
	 *
	 * @param type 原始类型
	 * @return 是章节测评返回 true
	 */
	public static boolean isExam(String type) {
		return EXAM.equals(normalize(type));
	}

}

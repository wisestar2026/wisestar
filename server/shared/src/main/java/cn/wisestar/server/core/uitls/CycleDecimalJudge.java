package cn.wisestar.server.core.uitls;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 循环小数数学等价判定（后端移植版）。
 *
 * <p><b>定位</b>：与前端 <code>wisestar-client/src/utils/cycleDecimal.js</code> 语义保持一致，
 * 供 {@link AnswerJudgeUtil} 填空判分调用，修复「前端判对、后端判错」的循环小数判分不一致问题。</p>
 *
 * <p><b>四种形态</b>：</p>
 * <ul>
 *   <li>STD 标准：循环节半角括号，如 {@code 0.(6)}、{@code 3.4(897)}；</li>
 *   <li>DISPLAY 显示：教材循环点记法（组合上点 U+0307 / 间隔上点 U+02D9）；</li>
 *   <li>INPUT 输入：括号 / 循环点 / 省略号 / 有限小数；</li>
 *   <li>OBJ 判题对象：结构化 {@code {kind, int, nonrep, rep}} 或 {@code {kind:'finite', int, frac}}。</li>
 * </ul>
 *
 * <p><b>设计原则</b>：判题只做数学等价判定，不依赖字符串相等；有限小数与循环小数类型不同即判错
 * （{@code 0.666} ≠ {@code 0.(6)}）；同一数值的不同循环节写法归一为最短循环节。</p>
 *
 * @author zhanghaiyang
 */
public final class CycleDecimalJudge {

	private CycleDecimalJudge() {
	}

	/** 组合上点（循环点） */
	private static final char COMBINING_DOT = '\u0307';

	/** 间隔上点 */
	private static final char SPACING_DOT = '\u02D9';

	/** STD：整数部分.非循环部分(循环节)，循环节半角括号紧跟且至少 1 位 */
	private static final Pattern STD_CYCLE_RE = Pattern.compile("^(\\d+)?\\.(\\d*)\\((\\d+)\\)$");

	/** 有限小数：整数部分.小数部分，小数部分至少 1 位 */
	private static final Pattern FINITE_RE = Pattern.compile("^(\\d+)?\\.(\\d+)$");

	/** 省略号写法：如 0.666… / 3.4897897...（兼容三个及以上半角点） */
	private static final Pattern ELLIPSIS_RE = Pattern.compile("^(\\d+)?\\.(\\d+)(?:…|\\.{3,})$");

	/**
	 * 循环小数等价判定。
	 *
	 * <p>仅当至少一方为循环小数时返回 {@code TRUE}/{@code FALSE}；两边都不是循环小数时返回
	 * {@code null}（调用方回退到既有文本等值比较，避免影响普通数值/文本）。</p>
	 *
	 * @param stdText   一侧答案（通常为标准答案 STD 写法）
	 * @param inputText 另一侧答案（通常为学员输入）
	 * @return 等价结果；不适用循环小数判定返回 null
	 */
	public static Boolean cycleDecimalEquals(String stdText, String inputText) {
		// 快速路径：两侧均不含任何循环标记时，必然不涉及循环小数，直接回退
		if (!hasCycleIndicator(stdText) && !hasCycleIndicator(inputText)) {
			return null;
		}
		CycleObj a = parseDecimalLoose(stdText);
		CycleObj b = parseDecimalLoose(inputText);
		boolean aCycle = a != null && "cycle".equals(a.kind);
		boolean bCycle = b != null && "cycle".equals(b.kind);
		if (!aCycle && !bCycle) {
			return null;
		}
		return cycleEquals(a, b);
	}

	/** 是否含有循环小数的可能标记（括号 / 省略号 / 循环点）。 */
	private static boolean hasCycleIndicator(String s) {
		if (s == null) {
			return false;
		}
		return s.indexOf('(') >= 0 || s.indexOf('…') >= 0 || s.indexOf(COMBINING_DOT) >= 0
				|| s.indexOf(SPACING_DOT) >= 0 || s.contains("..");
	}

	/**
	 * 解析任意文本为判题对象：优先按 STD，其次按 INPUT。
	 */
	private static CycleObj parseDecimalLoose(String text) {
		String s = text == null ? "" : text.trim();
		if (s.isEmpty()) {
			return null;
		}
		if (STD_CYCLE_RE.matcher(s).matches()) {
			return parseStd(s);
		}
		return parseInput(s);
	}

	/** 解析 STD 文本；无括号但为合法十进制时返回有限小数对象；无法解析返回 null。 */
	private static CycleObj parseStd(String text) {
		String s = text == null ? "" : text.trim();
		Matcher m = STD_CYCLE_RE.matcher(s);
		if (m.matches()) {
			return canonicalizeCycle(orZero(m.group(1)), m.group(2) == null ? "" : m.group(2), m.group(3));
		}
		return parseFiniteText(s);
	}

	/**
	 * 解析任意 INPUT 文本：括号 → 循环点 → 省略号 → 有限小数；无法识别返回 null。
	 */
	private static CycleObj parseInput(String text) {
		String s = text == null ? "" : text.replaceAll("\\s+", "").trim();
		if (s.isEmpty()) {
			return null;
		}
		if (STD_CYCLE_RE.matcher(s).matches()) {
			return parseStd(s);
		}
		CycleObj dotted = parseDotNotation(s);
		if (dotted != null) {
			return dotted;
		}
		CycleObj ellipsis = parseEllipsis(s);
		if (ellipsis != null) {
			return ellipsis;
		}
		return parseFiniteText(s);
	}

	/** 解析纯十进制（含整数）文本为有限小数对象；失败返回 null。 */
	private static CycleObj parseFiniteText(String text) {
		String s = text == null ? "" : text.trim();
		Matcher f = FINITE_RE.matcher(s);
		if (f.matches()) {
			return CycleObj.finite(orZero(f.group(1)), f.group(2));
		}
		if (s.matches("\\d+")) {
			return CycleObj.finite(s, "");
		}
		return null;
	}

	/**
	 * 解析循环点记法（如 {@code 0.6̇}、{@code 3.48̇9̇7̇}）：
	 * 点只能落在小数位，且必须构成连续尾段（非循环 + 循环两段）。合法返回 cycle 对象；否则返回 null。
	 */
	private static CycleObj parseDotNotation(String text) {
		String s = text == null ? "" : text;
		if (s.indexOf(COMBINING_DOT) < 0 && s.indexOf(SPACING_DOT) < 0) {
			return null;
		}
		int dotIdx = s.indexOf('.');
		if (dotIdx < 0) {
			return null;
		}
		String intRaw = s.substring(0, dotIdx);
		String fracRaw = s.substring(dotIdx + 1);
		// 整数位不允许加点
		for (int i = 0; i < intRaw.length(); i++) {
			if (isDotChar(intRaw.charAt(i))) {
				return null;
			}
		}
		List<Character> digits = new ArrayList<>();
		List<Boolean> dotted = new ArrayList<>();
		for (int i = 0; i < fracRaw.length(); i++) {
			char ch = fracRaw.charAt(i);
			if (isDotChar(ch)) {
				if (digits.isEmpty()) {
					return null; // 点前无数字
				}
				dotted.set(dotted.size() - 1, Boolean.TRUE);
			}
			else if (ch >= '0' && ch <= '9') {
				digits.add(ch);
				dotted.add(Boolean.FALSE);
			}
			else {
				return null; // 出现非法字符
			}
		}
		if (digits.isEmpty()) {
			return null;
		}
		int first = -1;
		for (int i = 0; i < dotted.size(); i++) {
			if (Boolean.TRUE.equals(dotted.get(i))) {
				first = i;
				break;
			}
		}
		if (first < 0) {
			return null;
		}
		// 必须为连续尾段
		for (int i = first; i < dotted.size(); i++) {
			if (!Boolean.TRUE.equals(dotted.get(i))) {
				return null;
			}
		}
		StringBuilder nonrep = new StringBuilder();
		for (int i = 0; i < first; i++) {
			nonrep.append(digits.get(i));
		}
		StringBuilder rep = new StringBuilder();
		for (int i = first; i < digits.size(); i++) {
			rep.append(digits.get(i));
		}
		return canonicalizeCycle(orZero(intRaw), nonrep.toString(), rep.toString());
	}

	/** 解析省略号写法（{@code 0.666…}、{@code 3.4897897…}，兼容 {@code ...}）；不合法返回 null。 */
	private static CycleObj parseEllipsis(String text) {
		String s = text == null ? "" : text.trim();
		Matcher m = ELLIPSIS_RE.matcher(s);
		if (!m.matches()) {
			return null;
		}
		return deriveCycleFromFraction(m.group(1), m.group(2));
	}

	/**
	 * 从「省略号写法」的小数位推导循环节：从小数末尾往前取最短重复段，需出现 ≥2 次；无法判定返回 null。
	 */
	private static CycleObj deriveCycleFromFraction(String intPart, String fracDigits) {
		String frac = fracDigits == null ? "" : fracDigits;
		for (int len = 1; len <= frac.length() / 2; len++) {
			String cand = frac.substring(frac.length() - len);
			if (frac.substring(frac.length() - 2 * len).equals(cand + cand)) {
				return canonicalizeCycle(intPart, frac.substring(0, frac.length() - 2 * len), cand);
			}
		}
		return null;
	}

	/**
	 * 归一化循环小数：压缩循环节为最短，并把非循环部分中属于循环的前缀左移。
	 * 例：{@code 6(6)} → {@code (6)}；{@code 4(897)} 保持；{@code 1233(3)} → {@code 123(3)}。
	 */
	private static CycleObj canonicalizeCycle(String intPart, String nonrep, String rep) {
		String r = shortestPeriod(rep);
		int p = r.length();
		String baseInt = orZero(intPart);
		if (p == 0) {
			return CycleObj.finite(baseInt, nonrep == null ? "" : nonrep);
		}
		String d = nonrep == null ? "" : nonrep;
		// 展开足够长度后寻找最小的「纯循环起点」q
		String seq = d + r + r + r;
		for (int q = 0; q <= d.length(); q++) {
			boolean periodic = true;
			for (int i = q; i + p < seq.length(); i++) {
				if (seq.charAt(i) != seq.charAt(i + p)) {
					periodic = false;
					break;
				}
			}
			if (periodic) {
				return CycleObj.cycle(baseInt, seq.substring(0, q), shortestPeriod(seq.substring(q, q + p)));
			}
		}
		return CycleObj.cycle(baseInt, d, r);
	}

	/** 最小循环节：{@code 66}→{@code 6}、{@code 1212}→{@code 12}、{@code 897897}→{@code 897}。 */
	private static String shortestPeriod(String rep) {
		String s = rep == null ? "" : rep;
		if (s.isEmpty()) {
			return s;
		}
		for (int len = 1; len < s.length(); len++) {
			if (s.length() % len != 0) {
				continue;
			}
			String unit = s.substring(0, len);
			StringBuilder sb = new StringBuilder(s.length());
			for (int i = 0; i < s.length() / len; i++) {
				sb.append(unit);
			}
			if (sb.toString().equals(s)) {
				return unit;
			}
		}
		return s;
	}

	/**
	 * 数学等价判定：类型不同（有限 vs 循环）直接 false；有限小数按去尾随 0 比较；
	 * 循环小数展开前 L 位逐位比较，L = len(nonrep) + len(rep) * 2 + 4。
	 */
	private static boolean cycleEquals(CycleObj a, CycleObj b) {
		if (a == null || b == null) {
			return false;
		}
		if (!a.kind.equals(b.kind)) {
			return false;
		}
		if ("finite".equals(a.kind)) {
			return finiteEquals(a, b);
		}
		if (!intEquals(a.intPart, b.intPart)) {
			return false;
		}
		int l = Math.max(a.nonrep.length() + a.rep.length() * 2 + 4,
				b.nonrep.length() + b.rep.length() * 2 + 4);
		return expandCycleFrac(a, l).equals(expandCycleFrac(b, l));
	}

	/** 整数部分比较（忽略前导 0）。 */
	private static boolean intEquals(String a, String b) {
		return normalizeInt(a).equals(normalizeInt(b));
	}

	private static String normalizeInt(String s) {
		String x = orZero(s);
		String r = x.replaceFirst("^0+(?=\\d)", "");
		return r.isEmpty() ? "0" : r;
	}

	/** 有限小数比较（去尾随 0）。 */
	private static boolean finiteEquals(CycleObj a, CycleObj b) {
		if (!intEquals(a.intPart, b.intPart)) {
			return false;
		}
		return stripTrailingZeros(a.frac).equals(stripTrailingZeros(b.frac));
	}

	private static String stripTrailingZeros(String s) {
		String x = s == null ? "" : s;
		int end = x.length();
		while (end > 0 && x.charAt(end - 1) == '0') {
			end--;
		}
		return x.substring(0, end);
	}

	/** 展开循环小数的前 len 位小数。 */
	private static String expandCycleFrac(CycleObj obj, int len) {
		String rep = obj.rep == null ? "" : obj.rep;
		String nonrep = obj.nonrep == null ? "" : obj.nonrep;
		if (rep.isEmpty()) {
			return nonrep;
		}
		int need = Math.max(0, len - nonrep.length());
		int times = (int) Math.ceil(need / (double) rep.length()) + 1;
		StringBuilder sb = new StringBuilder(nonrep);
		for (int i = 0; i < times; i++) {
			sb.append(rep);
		}
		String s = sb.toString();
		return s.length() <= len ? s : s.substring(0, len);
	}

	private static boolean isDotChar(char c) {
		return c == COMBINING_DOT || c == SPACING_DOT;
	}

	private static String orZero(String s) {
		return s == null || s.isEmpty() ? "0" : s;
	}

	/** 判题对象：循环 {@code {kind:'cycle', int, nonrep, rep}} / 有限 {@code {kind:'finite', int, frac}}。 */
	private static final class CycleObj {

		final String kind;

		final String intPart;

		final String nonrep;

		final String rep;

		final String frac;

		private CycleObj(String kind, String intPart, String nonrep, String rep, String frac) {
			this.kind = kind;
			this.intPart = intPart;
			this.nonrep = nonrep;
			this.rep = rep;
			this.frac = frac;
		}

		static CycleObj cycle(String intPart, String nonrep, String rep) {
			return new CycleObj("cycle", intPart, nonrep, rep, "");
		}

		static CycleObj finite(String intPart, String frac) {
			return new CycleObj("finite", intPart, "", "", frac == null ? "" : frac);
		}

	}

}

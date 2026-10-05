package cn.wisestar.server.impl;

import cn.wisestar.server.domain.model.Template;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 薄弱点专攻组卷加权单元测试（任务 5.4）。
 *
 * <p>{@code pickByCoverage} 为纯函数：先保底覆盖每个知识点，再按权重轮询补足。
 * 薄弱知识点权重为 2、普通为 1，因此目标题量下薄弱知识点的配额应高于普通知识点。</p>
 *
 * @author wisestar
 * @date 2026/10/5
 */
class StudentServiceImplTest {

	@Test
	void pickByCoverage_biasesTowardWeakKnowledgePoints() throws Exception {
		Map<String, String> assigned = new HashMap<>();
		List<Template> picked = invoke(candidates(20), pools(), 6, null, false, false, assigned,
				new HashSet<>(Collections.singletonList("kp1")));

		assertThat(picked).hasSize(6);
		assertThat(countAssigned(assigned, "kp1")).isEqualTo(4);
		assertThat(countAssigned(assigned, "kp2")).isEqualTo(2);
	}

	@Test
	void pickByCoverage_distributesEvenlyWithoutWeakPoints() throws Exception {
		Map<String, String> assigned = new HashMap<>();
		List<Template> picked = invoke(candidates(20), pools(), 6, null, false, false, assigned,
				Collections.emptySet());

		assertThat(picked).hasSize(6);
		assertThat(countAssigned(assigned, "kp1")).isEqualTo(3);
		assertThat(countAssigned(assigned, "kp2")).isEqualTo(3);
	}

	@Test
	void pickByCoverage_guaranteesOnePerKnowledgePointWithPerKpLimit() throws Exception {
		Map<String, String> assigned = new HashMap<>();
		List<Template> picked = invoke(candidates(20), pools(), 2, 1, true, false, assigned,
				Collections.emptySet());

		// 目标 2 题、每知识点上限 1 题：保底覆盖两个知识点各 1 题，且都被回填归属
		assertThat(picked).hasSize(2);
		assertThat(assigned).hasSize(2);
		assertThat(new HashSet<>(assigned.values())).containsExactlyInAnyOrder("kp1", "kp2");
	}

	// ------------------------------------------------------------------ 辅助

	@SuppressWarnings("unchecked")
	private List<Template> invoke(List<Template> candidates, Map<String, Set<String>> questionsByKp, int limit,
			Integer perKp, boolean groupByKp, Boolean random, Map<String, String> assignedKp, Set<String> weakKpIds)
			throws Exception {
		StudentServiceImpl service = Mockito.mock(StudentServiceImpl.class, Mockito.CALLS_REAL_METHODS);
		Method method = StudentServiceImpl.class.getDeclaredMethod("pickByCoverage", List.class, Map.class, int.class,
				Integer.class, boolean.class, Boolean.class, Map.class, Set.class);
		method.setAccessible(true);
		return (List<Template>) method.invoke(service, candidates, questionsByKp, limit, perKp, groupByKp, random,
				assignedKp, weakKpIds);
	}

	private long countAssigned(Map<String, String> assigned, String kpId) {
		return assigned.values().stream().filter(kpId::equals).count();
	}

	private List<Template> candidates(int count) {
		List<Template> list = new ArrayList<>();
		for (int i = 1; i <= count; i++) {
			list.add(template("q" + i));
		}
		return list;
	}

	private Template template(String id) {
		Template t = new Template();
		t.setId(id);
		return t;
	}

	private Map<String, Set<String>> pools() {
		Map<String, Set<String>> byKp = new LinkedHashMap<>();
		byKp.put("kp1", ids("q1", "q2", "q3", "q4", "q5", "q6", "q7", "q8", "q9", "q10"));
		byKp.put("kp2", ids("q11", "q12", "q13", "q14", "q15", "q16", "q17", "q18", "q19", "q20"));
		return byKp;
	}

	private Set<String> ids(String... values) {
		return new LinkedHashSet<>(Arrays.asList(values));
	}

}

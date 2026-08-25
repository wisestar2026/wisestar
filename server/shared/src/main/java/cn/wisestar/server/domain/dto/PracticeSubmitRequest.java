package cn.wisestar.server.domain.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 练习交卷提交请求（学员端练习完成后落库）。
 *
 * <p><b>数据流</b>：前端交卷 → PracticeApi.submitPractice → PracticeServiceImpl：
 * 后端按 questionId 回源题目并复核判分，写入 t_practice_record + t_practice_detail，
 * 错题（is_correct=0）后续供错题本查询。</p>
 *
 * <p><b>学生答案格式</b>：items[i].answer 为前端答案结构
 * <code>{type: 'option', optionId} / {type: 'options', optionIds: []} / {type: 'text', text}</code>。</p>
 */
@Data
public class PracticeSubmitRequest {

	/**
	 * 练习模式：special 专项刷题 / exam 套卷模拟 / random 随机练习
	 */
	private String mode;

	/**
	 * 来源题库 ID（可空）
	 */
	private String repoId;

	/** 知识点ID（知识点练习提交时记录，供知识点型任务完成判定） */
	private String knowledgePointId;

	/**
	 * 练习用时（毫秒）
	 */
	private Long durationMs;

	/**
	 * 逐题作答结果
	 */
	private List<PracticeItem> items;

	/**
	 * 单题作答。
	 */
	@Data
	public static class PracticeItem {

		/**
		 * 题目 ID（t_template.id）
		 */
		private String questionId;

		/**
		 * 学生答案（前端答案结构）
		 */
		private Map<String, Object> answer;
	}
}

package cn.wisestar.server.mapper;

import cn.wisestar.server.domain.dto.WrongQuestionQuery;
import cn.wisestar.server.domain.dto.WrongQuestionView;
import cn.wisestar.server.domain.model.PracticeDetail;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 练习逐题明细 Mapper。
 *
 * <p><b>所属模块</b>：rdbms 模块 Mapper 包（cn.wisestar.server.mapper）。</p>
 * <p><b>功能</b>：t_practice_detail 表 CRUD，继承 MyBatis-Plus BaseMapper；
 * 自定义 {@link #selectWrongQuestions} 按「题目 + 学员」聚合错题，供错题库管理页查询。</p>
 *
 * <p><b>错题语义</b>：is_correct = 0（含未作答）即错题；聚合口径为
 * t_practice_detail JOIN t_practice_record（取学员）JOIN t_user（取姓名）
 * JOIN t_template（取题目信息）JOIN t_repo（取题库名）。</p>
 *
 * @author zhanghaiyang
 */
@Mapper
public interface PracticeDetailMapper extends BaseMapper<PracticeDetail> {

	/**
	 * 分页查询聚合错题（题目 × 学员粒度）。
	 *
	 * <p><b>聚合口径</b>：同题同人多次做错合并为一条，wrongCount 累计次数，
	 * lastAnswer / lastScore 取最近一次做错记录，lastWrongTime 为最近做错时间。</p>
	 *
	 * <p><b>实现说明</b>：内层用窗口函数（ROW_NUMBER / COUNT OVER PARTITION BY）
	 * 取每人每题最近一次错题明细并携带分区错误次数，外层过滤 rn = 1；
	 * 窗口函数语法 MySQL 8 与 H2 2.x（MODE=MySQL）均兼容。</p>
	 *
	 * <p><b>动态条件</b>：repoId（练习会话或题目归属题库命中即匹配）、questionType、
	 * keyword（题目标题/学员姓名模糊）、startTime/endTime（做错时间范围）。</p>
	 *
	 * @param page  分页参数（MyBatis-Plus 分页插件自动补 count 与 limit）
	 * @param query 筛选条件
	 * @return 当前页聚合错题列表
	 */
	@Select("<script>"
			+ "SELECT x.questionId, x.questionType, x.questionTitle, x.repoId, x.repoName, "
			+ "       x.userId, x.userName, x.wrongCount, x.lastWrongTime, x.lastAnswer, x.lastScore, x.wrongReason "
			+ "FROM ( "
			+ "  SELECT d.question_id AS questionId, d.question_type AS questionType, "
			+ "         t.name AS questionTitle, "
			+ "         COALESCE(r.repo_id, t.repo_id) AS repoId, rp.name AS repoName, "
			+ "         r.user_id AS userId, COALESCE(u.name, st.name) AS userName, "
			+ "         COUNT(*) OVER (PARTITION BY d.question_id, r.user_id) AS wrongCount, "
			+ "         MAX(d.create_at) OVER (PARTITION BY d.question_id, r.user_id) AS lastWrongTime, "
			+ "         d.user_answer AS lastAnswer, d.score AS lastScore, d.wrong_reason AS wrongReason, "
			+ "         ROW_NUMBER() OVER (PARTITION BY d.question_id, r.user_id ORDER BY d.create_at DESC) AS rn "
			+ "  FROM t_practice_detail d "
			+ "  JOIN t_practice_record r ON d.practice_id = r.id AND r.is_deleted = 0 "
			+ "  LEFT JOIN t_user u ON r.user_id = u.id AND u.is_deleted = 0 "
			+ "  LEFT JOIN t_student st ON r.user_id = st.id AND st.is_deleted = 0 "
			+ "  LEFT JOIN t_template t ON d.question_id = t.id AND t.is_deleted = 0 "
			+ "  LEFT JOIN t_repo rp ON COALESCE(r.repo_id, t.repo_id) = rp.id " // t_repo 为遗留表，无 is_deleted 列，不做逻辑删除过滤
			+ "  WHERE d.is_deleted = 0 AND d.is_correct = 0 "
			+ "  <if test=\"query.repoId != null and query.repoId != ''\"> "
			+ "    AND COALESCE(r.repo_id, t.repo_id) = #{query.repoId} "
			+ "  </if> "
			+ "  <if test=\"query.questionType != null and query.questionType != ''\"> "
			+ "    AND d.question_type = #{query.questionType} "
			+ "  </if> "
			+ "  <if test=\"query.userId != null and query.userId != ''\"> "
			+ "    AND r.user_id = #{query.userId} "
			+ "  </if> "
			+ "  <if test=\"query.keyword != null and query.keyword != ''\"> "
			+ "    AND (t.name LIKE CONCAT('%', #{query.keyword}, '%') "
			+ "         OR u.name LIKE CONCAT('%', #{query.keyword}, '%')) "
			+ "  </if> "
			+ "  <if test=\"query.startTime != null\"> "
			+ "    AND d.create_at &gt;= #{query.startTime} "
			+ "  </if> "
			+ "  <if test=\"query.endTime != null\"> "
			+ "    AND d.create_at &lt;= #{query.endTime} "
			+ "  </if> "
			+ ") x "
			+ "WHERE x.rn = 1 "
			+ "ORDER BY x.lastWrongTime DESC "
			+ "</script>")
	IPage<WrongQuestionView> selectWrongQuestions(IPage<WrongQuestionView> page,
			@Param("query") WrongQuestionQuery query);
}

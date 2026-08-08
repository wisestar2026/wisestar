package cn.wisestar.server.mapper;

import cn.wisestar.server.domain.model.AnswerDetail;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 答题明细表（t_answer_detail）数据访问 Mapper（MyBatis-Plus）。
 *
 * 【类职责】
 * 提供 AnswerDetail 实体的基础 CRUD（继承 BaseMapper 自动获得 selectList/insert/delete 等），
 * 供学生答题情况分析相关业务读写"一次答卷中单道题的作答记录"。
 * 本接口没有自定义 SQL，全部走 MyBatis-Plus 通用方法，逻辑删除由 BaseModel.deleted 自动接管。
 *
 * 【被谁调用】
 * - 写入方：AnswerServiceImpl.generateAnswerDetails —— 提交答卷时先按 answerId delete 旧明细，
 *   再逐条 insert 新明细（先删后插保证幂等）
 * - 读取方：AnalysisServiceImpl.knowledgePointStats / studentProfile —— 按学生/学科/章节筛选
 *   明细做知识点聚合统计
 *
 * 【依赖什么】
 * - AnswerDetail 实体（继承 BaseModel，含 createBy=学生ID、isCorrect、knowledgePoint 等分析字段）
 * - MyBatis-Plus BaseMapper 通用能力（需要 Mapper 扫描与 MybatisPlugConfig 分页插件配合）
 *
 * 【核心数据流】
 * 学生提交答卷 → AnswerServiceImpl.generateAnswerDetails 写入本表
 * → 学生画像/知识点统计（AnalysisServiceImpl）→ selectList(Wrappers.lambdaQuery)
 * → SQL: SELECT * FROM t_answer_detail WHERE is_deleted=0 AND create_by=? AND subject=?...
 * → 内存聚合统计 → KnowledgePointStat 列表返回前端。
 *
 * @author zhanghaiyang
 * @date 2026/8/1
 */
public interface AnswerDetailMapper extends BaseMapper<AnswerDetail> {

}

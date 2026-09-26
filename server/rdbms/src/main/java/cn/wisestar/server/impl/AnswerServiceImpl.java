package cn.wisestar.server.impl;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.core.uitls.SecurityContextUtils;
import cn.wisestar.server.domain.dto.ExerciseView;
import cn.wisestar.server.domain.dto.HistoryExerciseQuery;
import cn.wisestar.server.domain.dto.SurveySchema;
import cn.wisestar.server.domain.model.Answer;
import cn.wisestar.server.mapper.AnswerMapper;
import cn.wisestar.server.service.AnswerService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 答卷（Answer）业务实现。
 *
 * 【类职责】
 * 仅保留练习历史查询 historyExercise。原问卷/答卷管理链路（CRUD、考试计分、
 * 答题明细生成、Excel 导出、附件下载、回收站、Excel 批量导入、关联问卷同步）
 * 已随对应前端模块与控制器下线一并移除。
 *
 * 【被谁调用】
 * - ExerciseApi：historyExercise（练习历史）
 * - RepoServiceImpl / UserServiceImpl：直接使用继承自 ServiceImpl 的基础 CRUD
 *   （getOne / updateById / page），因此本类仍需保留为 Spring Bean
 *
 * @author javahuang
 * @date 2021/8/3
 */
@Service
@Transactional
@Slf4j
public class AnswerServiceImpl extends ServiceImpl<AnswerMapper, Answer> implements AnswerService {

    /**
     * 练习历史分页查询（顺序/随机/错题练习记录）。
     *
     * @param query 项目、练习类型（examExerciseType 非空）、状态（tempSave）条件；
     *              仅查当前登录用户本人
     * @return 分页的 ExerciseView（含题目总数/已答数/完成百分比）
     * @implNote 调用链：ExerciseApi.historyExercise。
     * 完成百分比 = 已答题目数 / 问卷快照题目总数 * 100（取整）。
     */
    @Override
    public PaginationResponse<ExerciseView> historyExercise(HistoryExerciseQuery query) {
        Page<Answer> page = new Page<>(query.getCurrent(), query.getPageSize());
        super.page(page, Wrappers.<Answer>lambdaQuery()
                .eq(query.getProjectId() != null, Answer::getProjectId, query.getProjectId())
                .isNotNull(Answer::getExamExerciseType)
                .eq(query.getTempSave() != null, Answer::getTempSave, query.getTempSave())
                .eq(Answer::getCreateBy, SecurityContextUtils.getUserId())
                .orderByDesc(Answer::getCreateAt));

        List<ExerciseView> list = page.getRecords().stream().map(x -> {
            ExerciseView view = new ExerciseView();
            view.setId(x.getId());
            view.setProjectName(x.getProjectId());
            view.setTempSave(x.getTempSave());
            view.setCreateAt(x.getCreateAt());
            view.setExamExerciseType(x.getExamExerciseType());
            view.setAnswerId(x.getId());
            SurveySchema schema = x.getSurvey();
            view.setProjectName(schema.getTitle());
            view.setRepoId(x.getRepoId());
            view.setPercent(0L);

            if (x.getSurvey() == null || x.getTempAnswer() == null || x.getSurvey().getChildren().isEmpty()) {
                return view;
            }
            int totalQuestions = x.getSurvey().getChildren().size();
            int answeredQuestions = x.getTempSave() == 1 ? x.getAnswer().size() : x.getTempAnswer().size();

            double percent = ((double) answeredQuestions / totalQuestions) * 100;
            view.setPercent(Math.round(percent));
            return view;
        }).collect(Collectors.toList());

        return new PaginationResponse<>(page.getTotal(), list);
    }
}

package cn.wisestar.server.impl;

import cn.wisestar.server.domain.dto.student.StudentSupervisionView;
import cn.wisestar.server.domain.model.*;
import cn.wisestar.server.mapper.*;
import cn.wisestar.server.service.StudentSupervisionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 学员督学服务实现
 *
 * @author wisestar
 * @date 2026/9/3
 */
@Service
@RequiredArgsConstructor
public class StudentSupervisionServiceImpl implements StudentSupervisionService {

    private final StudentMapper studentMapper;
    private final StudentRecordMapper studentRecordMapper;
    private final ChapterMapper chapterMapper;
    private final SectionMapper sectionMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final TemplateMapper templateMapper;

    @Override
    public List<StudentSupervisionView> getOnlineStudents() {
        // 获取最近 5 分钟活跃的学员记录
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        String fiveMinutesAgoStr = fiveMinutesAgo.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        List<StudentRecord> records = studentRecordMapper.selectList(
            new LambdaQueryWrapper<StudentRecord>()
                .ge(StudentRecord::getLastActiveTime, fiveMinutesAgoStr)
                .orderByDesc(StudentRecord::getLastActiveTime)
        );

        List<StudentSupervisionView> result = new ArrayList<>();

        for (StudentRecord record : records) {
            Student student = studentMapper.selectById(record.getStudentId());
            if (student == null) continue;

            StudentSupervisionView view = new StudentSupervisionView();
            view.setStudentId(student.getId());
            view.setStudentName(student.getName());
            view.setStudentNo(student.getStudentNo());
            view.setLastActiveTime(record.getLastActiveTime());

            // 判断当前学习状态
            String location = record.getCurrentLocation();
            if (location == null || location.isEmpty()) {
                view.setStatus("offline");
            } else if (location.startsWith("chapter/")) {
                view.setStatus("learning");
                String chapterId = location.replace("chapter/", "");
                Chapter chapter = chapterMapper.selectById(chapterId);
                if (chapter != null) {
                    view.setChapterName(chapter.getName());
                }
                view.setCurrentLocation(chapter != null ? chapter.getName() : location);
            } else if (location.startsWith("section/")) {
                view.setStatus("learning");
                String sectionId = location.replace("section/", "");
                Section section = sectionMapper.selectById(sectionId);
                if (section != null) {
                    view.setSectionName(section.getName());
                    Chapter chapter = chapterMapper.selectById(section.getChapterId());
                    if (chapter != null) {
                        view.setChapterName(chapter.getName());
                    }
                }
                view.setCurrentLocation(section != null ? section.getName() : location);
            } else if (location.startsWith("knowledge/")) {
                view.setStatus("learning");
                String kpId = location.replace("knowledge/", "");
                KnowledgePoint kp = knowledgePointMapper.selectById(kpId);
                if (kp != null) {
                    view.setKnowledgePointName(kp.getName());
                    Section section = sectionMapper.selectById(kp.getSectionId());
                    if (section != null) {
                        view.setSectionName(section.getName());
                        Chapter chapter = chapterMapper.selectById(section.getChapterId());
                        if (chapter != null) {
                            view.setChapterName(chapter.getName());
                        }
                    }
                }
                view.setCurrentLocation(kp != null ? kp.getName() : location);
            } else if (location.startsWith("exercise/")) {
                view.setStatus("exercising");
                String questionId = location.replace("exercise/", "");
                Template question = templateMapper.selectById(questionId);
                if (question != null) {
                    view.setQuestionContent(question.getName());
                    // 获取答案和解析
                    view.setCorrectAnswer(question.getTemplate() != null ? 
                        extractCorrectAnswer(question.getTemplate()) : "");
                    view.setAnswerAnalysis(question.getTemplate() != null ?
                        extractAnalysis(question.getTemplate()) : "");
                    // 学员答案需要从答题记录中获取
                    view.setStudentAnswer("学员答案"); // 待完善
                }
                view.setCurrentLocation(question != null ? question.getName() : location);
            } else {
                view.setStatus("offline");
                view.setCurrentLocation(location);
            }

            result.add(view);
        }

        return result;
    }

    /**
     * 从题目模板中提取正确答案
     */
    private String extractCorrectAnswer(String template) {
        // TODO: 从 JSON 中提取正确答案
        return "答案";
    }

    /**
     * 从题目模板中提取答案解析
     */
    private String extractAnalysis(String template) {
        // TODO: 从 JSON 中提取答案解析
        return "解析";
    }

}

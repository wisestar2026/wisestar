package cn.wisestar.server.api;

import cn.wisestar.server.domain.dto.student.StudentSupervisionView;
import cn.wisestar.server.service.StudentSupervisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 学员督学接口（教师端查看学员学习状态）
 *
 * @author wisestar
 * @date 2026/9/3
 */
@RestController
@RequestMapping("${api.prefix}/student/supervision")
@RequiredArgsConstructor
public class StudentSupervisionApi {

    private final StudentSupervisionService supervisionService;

    /**
     * 获取在线学员列表（只显示在线学员）
     *
     * @return 在线学员列表
     */
    @GetMapping("/online-students")
    @PreAuthorize("hasAuthority('student:supervision')")
    public List<StudentSupervisionView> getOnlineStudents() {
        return supervisionService.getOnlineStudents();
    }

}

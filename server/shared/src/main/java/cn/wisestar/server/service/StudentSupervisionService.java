package cn.wisestar.server.service;

import cn.wisestar.server.domain.dto.student.StudentSupervisionView;

import java.util.List;

/**
 * 学员督学服务
 *
 * @author wisestar
 * @date 2026/9/3
 */
public interface StudentSupervisionService {

    /**
     * 获取在线学员列表
     *
     * @return 在线学员列表
     */
    List<StudentSupervisionView> getOnlineStudents();

}

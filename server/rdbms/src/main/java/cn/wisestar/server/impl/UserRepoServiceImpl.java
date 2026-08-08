package cn.wisestar.server.impl;

import cn.wisestar.server.domain.model.UserRepo;
import cn.wisestar.server.mapper.UserRepoMapper;
import cn.wisestar.server.service.BaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 学员-题库分配服务实现
 *
 * <p><b>所属模块</b>：rdbms 模块 impl 包（cn.wisestar.server.impl）。</p>
 * <p><b>功能</b>：t_user_repo 表 CRUD（继承 BaseService/ServiceImpl），
 * 供 RepoServiceImpl 组装「我的题库」与分配管理使用。</p>
 *
 * @author zhanghaiyang
 */
@Slf4j
@Service
public class UserRepoServiceImpl extends BaseService<UserRepoMapper, UserRepo> {
}

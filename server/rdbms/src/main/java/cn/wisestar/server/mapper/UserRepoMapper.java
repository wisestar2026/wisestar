package cn.wisestar.server.mapper;

import cn.wisestar.server.domain.model.UserRepo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 学员-题库分配 Mapper
 *
 * <p><b>所属模块</b>：rdbms 模块 Mapper 包（cn.wisestar.server.mapper）。</p>
 * <p><b>功能</b>：t_user_repo 表 CRUD，继承 MyBatis-Plus BaseMapper，
 * 支持 LambdaQueryWrapper 条件查询（按 userId/repoId/assignType）。</p>
 *
 * @author zhanghaiyang
 */
@Mapper
public interface UserRepoMapper extends BaseMapper<UserRepo> {
}

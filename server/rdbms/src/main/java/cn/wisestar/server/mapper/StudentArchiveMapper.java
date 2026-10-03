package cn.wisestar.server.mapper;

import cn.wisestar.server.domain.model.StudentArchive;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 学员档案 Mapper。
 *
 * @author wisestar
 * @date 2026/10/3
 */
@Mapper
public interface StudentArchiveMapper extends BaseMapper<StudentArchive> {
}

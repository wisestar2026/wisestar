package cn.wisestar.server.mapper;

import cn.wisestar.server.domain.model.StudentArchiveRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 学员上课记录 Mapper。
 *
 * @author wisestar
 * @date 2026/10/3
 */
@Mapper
public interface StudentArchiveRecordMapper extends BaseMapper<StudentArchiveRecord> {
}

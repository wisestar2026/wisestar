package cn.wisestar.server.mapper;

import cn.wisestar.server.domain.model.StudySummary;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 当日学习总结 Mapper。
 *
 * @author wisestar
 * @date 2026/9/11
 */
@Mapper
public interface StudySummaryMapper extends BaseMapper<StudySummary> {
}

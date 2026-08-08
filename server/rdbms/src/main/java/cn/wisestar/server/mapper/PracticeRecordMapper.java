package cn.wisestar.server.mapper;

import cn.wisestar.server.domain.model.PracticeRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 练习会话记录 Mapper。
 *
 * <p><b>所属模块</b>：rdbms 模块 Mapper 包（cn.wisestar.server.mapper）。</p>
 * <p><b>功能</b>：t_practice_record 表 CRUD，继承 MyBatis-Plus BaseMapper，
 * 支持 LambdaQueryWrapper 按 userId/mode 等条件查询。</p>
 *
 * @author zhanghaiyang
 */
@Mapper
public interface PracticeRecordMapper extends BaseMapper<PracticeRecord> {
}

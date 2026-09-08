package cn.wisestar.server.mapper;

import cn.wisestar.server.domain.model.Campus;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;

/**
 * @author wisestar
 * @date 2026/9/8
 */
public interface CampusMapper extends BaseMapper<Campus> {

	/**
	 * 物理删除校区（零引用时使用；MyBatis-Plus 默认逻辑删除会占用唯一名称，禁止用于校区）。
	 *
	 * @param id 校区ID
	 */
	@Delete("DELETE FROM t_campus WHERE id = #{id}")
	int physicallyDelete(String id);

}

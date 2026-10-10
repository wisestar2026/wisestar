package cn.wisestar.server.service;

import cn.wisestar.server.domain.dto.PageQuery;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

/**
 * @author javahuang
 * @date 2021/10/12
 */
public class BaseService<M extends BaseMapper<T>, T> extends ServiceImpl<M, T> {

	/**
	 * 按分页查询参数执行分页查询。
	 *
	 * @param pageQuery 分页查询参数
	 * @param <E>       分页结果类型
	 * @return 分页结果
	 */
	public <E extends IPage<T>> Page<T> pageByQuery(PageQuery pageQuery) {
		Page<T> page = new Page<T>(pageQuery.getCurrent(), pageQuery.getPageSize());
		return super.page(page);
	}

	/**
	 * 按分页查询参数与查询条件执行分页查询。
	 *
	 * @param pageQuery    分页查询参数
	 * @param queryWrapper 查询条件包装器
	 * @param <E>          分页结果类型
	 * @return 分页结果
	 */
	public <E extends IPage<T>> Page pageByQuery(PageQuery pageQuery, Wrapper<T> queryWrapper) {
		Page<T> page = new Page<T>(pageQuery.getCurrent(), pageQuery.getPageSize());
		return super.page(page, queryWrapper);
	}

}

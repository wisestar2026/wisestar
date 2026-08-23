package cn.wisestar.server.impl;

import cn.wisestar.server.domain.dto.mall.MallGoodsRequest;
import cn.wisestar.server.domain.dto.mall.MallGoodsView;
import cn.wisestar.server.domain.mapper.MallGoodsViewMapper;
import cn.wisestar.server.domain.model.MallGoods;
import cn.wisestar.server.mapper.MallGoodsMapper;
import cn.wisestar.server.service.BaseService;
import cn.wisestar.server.service.MallGoodsService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.validation.ValidationException;
import java.util.List;

/**
 * 积分商城商品服务实现。
 *
 * @author wisestar
 * @date 2026/8/22
 */
@Service
@RequiredArgsConstructor
public class MallGoodsServiceImpl extends BaseService<MallGoodsMapper, MallGoods> implements MallGoodsService {

	private final MallGoodsViewMapper mallGoodsViewMapper;

	@Override
	public List<MallGoodsView> listGoods(Integer status) {
		List<MallGoods> goods = this.baseMapper.selectList(Wrappers.<MallGoods>lambdaQuery()
				.eq(status != null, MallGoods::getStatus, status)
				.orderByAsc(MallGoods::getSort));
		return mallGoodsViewMapper.toView(goods);
	}

	@Override
	public void createGoods(MallGoodsRequest request) {
		if (!StringUtils.hasText(request.getName())) {
			throw new ValidationException("商品名称不能为空");
		}
		MallGoods goods = mallGoodsViewMapper.fromRequest(request);
		if (goods.getPoints() == null) {
			goods.setPoints(0);
		}
		if (goods.getSort() == null) {
			goods.setSort(1);
		}
		if (goods.getStatus() == null) {
			goods.setStatus(1);
		}
		save(goods);
	}

	@Override
	public void updateGoods(MallGoodsRequest request) {
		if (request.getId() == null) {
			throw new ValidationException("商品 ID 不能为空");
		}
		if (!StringUtils.hasText(request.getName())) {
			throw new ValidationException("商品名称不能为空");
		}
		MallGoods goods = mallGoodsViewMapper.fromRequest(request);
		updateById(goods);
	}

	@Override
	public void deleteGoods(MallGoodsRequest request) {
		if (request.getId() == null) {
			throw new ValidationException("商品 ID 不能为空");
		}
		removeById(request.getId());
	}
}

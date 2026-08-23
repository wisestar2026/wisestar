package cn.wisestar.server.domain.mapper;

import cn.wisestar.server.core.base.mapper.BaseModelMapper;
import cn.wisestar.server.domain.dto.mall.MallGoodsRequest;
import cn.wisestar.server.domain.dto.mall.MallGoodsView;
import cn.wisestar.server.domain.model.MallGoods;
import org.mapstruct.Mapper;

/**
 * MallGoods 对象转换（Request ↔ Model ↔ View）。
 *
 * @author wisestar
 * @date 2026/8/22
 */
@Mapper
public interface MallGoodsViewMapper extends BaseModelMapper<MallGoodsRequest, MallGoodsView, MallGoods> {

	MallGoods fromRequest(MallGoodsRequest request);

	MallGoodsView toView(MallGoods item);

}

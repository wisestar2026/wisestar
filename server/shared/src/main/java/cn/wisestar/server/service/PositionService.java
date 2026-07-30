package cn.wisestar.server.service;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.domain.dto.PositionQuery;
import cn.wisestar.server.domain.dto.PositionRequest;
import cn.wisestar.server.domain.dto.PositionView;
import cn.wisestar.server.domain.dto.SelectPositionRequest;

import java.util.List;

/**
 * @author javahuang
 * @date 2021/11/2
 */
public interface PositionService {

	PaginationResponse<PositionView> listPosition(PositionQuery query);

	void addPosition(PositionRequest request);

	void updatePosition(PositionRequest request);

	void deletePosition(String id);

	List<PositionView> selectPositions(SelectPositionRequest request);

}

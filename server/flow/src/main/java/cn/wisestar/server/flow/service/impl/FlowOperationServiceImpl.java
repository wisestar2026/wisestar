package cn.wisestar.server.flow.service.impl;

import cn.wisestar.server.flow.domain.model.FlowOperation;
import cn.wisestar.server.flow.mapper.FlowOperationMapper;
import cn.wisestar.server.flow.service.FlowOperationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 *
 */
@Service
public class FlowOperationServiceImpl extends ServiceImpl<FlowOperationMapper, FlowOperation>
		implements FlowOperationService {

}

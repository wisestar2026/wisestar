package cn.wisestar.server.flow.service.impl;

import cn.wisestar.server.flow.domain.model.FlowInstance;
import cn.wisestar.server.flow.mapper.FlowInstanceMapper;
import cn.wisestar.server.flow.service.FlowInstanceService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 *
 */
@Service
public class FlowInstanceServiceImpl extends ServiceImpl<FlowInstanceMapper, FlowInstance>
		implements FlowInstanceService {

}

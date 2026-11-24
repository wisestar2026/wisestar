package cn.wisestar.research.flow;

import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import cn.wisestar.research.auth.CurrentUser;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FlowLogService {

	private final ResearchFlowLogMapper flowLogMapper;

	public void record(String bizType, String bizId, String action, String fromStatus, String toStatus,
			CurrentUser operator, String remark) {
		ResearchFlowLog log = new ResearchFlowLog();
		log.setBizType(bizType);
		log.setBizId(bizId);
		log.setAction(action);
		log.setFromStatus(fromStatus);
		log.setToStatus(toStatus);
		log.setOperatorId(operator.id());
		log.setOperatorName(operator.name());
		log.setRemark(remark);
		flowLogMapper.insert(log);
	}

	public List<ResearchFlowLog> list(String bizType, String bizId) {
		return flowLogMapper.selectList(Wrappers.<ResearchFlowLog>lambdaQuery()
				.eq(ResearchFlowLog::getBizType, bizType).eq(ResearchFlowLog::getBizId, bizId)
				.orderByDesc(ResearchFlowLog::getCreateAt));
	}

}

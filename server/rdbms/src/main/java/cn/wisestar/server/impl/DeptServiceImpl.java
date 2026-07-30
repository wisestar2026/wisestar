package cn.wisestar.server.impl;

import cn.wisestar.server.domain.dto.DeptRequest;
import cn.wisestar.server.domain.dto.DeptView;
import cn.wisestar.server.domain.dto.DeptSortRequest;
import cn.wisestar.server.domain.dto.SelectDeptRequest;
import cn.wisestar.server.domain.mapper.DeptDtoMapper;
import cn.wisestar.server.domain.model.Dept;
import cn.wisestar.server.domain.model.UserPosition;
import cn.wisestar.server.mapper.DeptMapper;
import cn.wisestar.server.mapper.UserPositionMapper;
import cn.wisestar.server.service.BaseService;
import cn.wisestar.server.service.DeptService;
import cn.wisestar.server.service.UserService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * @author javahuang
 * @date 2021/11/2
 */
@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class DeptServiceImpl extends BaseService<DeptMapper, Dept> implements DeptService {

	private final DeptDtoMapper deptDtoMapper;

	private final UserService userService;

	private final UserPositionMapper userPositionMapper;

	@Override
	public List<DeptView> listDept(SelectDeptRequest request) {
		if (request == null) {
			request = new SelectDeptRequest();
		}
		List<DeptView> result = deptDtoMapper.toView(list(Wrappers.<Dept>lambdaQuery()
				.in(!CollectionUtils.isEmpty(request.getSelected()), Dept::getId, request.getSelected())
				.orderByAsc(Dept::getSortCode)));
		result.forEach(orgView -> {
			String managerId = orgView.getManagerId();
			if (isNotBlank(managerId)) {
				orgView.setManagerName(userService.loadUserById(managerId).getName());
			}
		});
		return result;
	}

	@Override
	public DeptView getDept(String id) {
		return deptDtoMapper.toView(getById(id));
	}

	@Override
	public void addDept(DeptRequest request) {
		Dept dept = deptDtoMapper.fromRequest(request);
		if (StringUtils.isEmpty(request.getParentId())) {
			dept.setParentId("0");
		}
		dept.setSortCode((int) count(Wrappers.<Dept>lambdaQuery().eq(Dept::getParentId, request.getParentId())));
		save(dept);
	}

	@Override
	public void updateDept(DeptRequest request) {
		updateById(deptDtoMapper.fromRequest(request));
	}

	@Override
	public void deleteDept(String id) {
		removeById(id);
		userPositionMapper.delete(Wrappers.<UserPosition>lambdaQuery().eq(UserPosition::getDeptId, id));
	}

	@Override
	public void sortDept(DeptSortRequest request) {
		for (int i = 0; i < request.getNodes().size(); i++) {
			Dept dept = getById(request.getNodes().get(i));
			dept.setSortCode(i);
			updateById(dept);
		}
	}

}

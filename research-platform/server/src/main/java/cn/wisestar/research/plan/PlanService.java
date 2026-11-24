package cn.wisestar.research.plan;

import java.time.LocalDateTime;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import cn.wisestar.research.auth.AuthRole;
import cn.wisestar.research.auth.CurrentUser;
import cn.wisestar.research.auth.UserContext;
import cn.wisestar.research.common.BizException;
import cn.wisestar.research.common.PageResult;
import cn.wisestar.research.flow.FlowLogService;
import cn.wisestar.research.flow.ResearchFlowLog;
import cn.wisestar.research.plan.dto.PlanReviewRequest;
import cn.wisestar.research.plan.dto.PlanSaveRequest;
import cn.wisestar.research.plan.dto.PlanView;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlanService {

	private static final String BIZ_TYPE = "PLAN";

	private final ResearchPlanMapper planMapper;

	private final FlowLogService flowLogService;

	public PageResult<PlanView> list(long current, long pageSize, String semester, String subject, String grade,
			String classType, String status, String keyword) {
		CurrentUser user = UserContext.require();
		LambdaQueryWrapper<ResearchPlan> wrapper = Wrappers.lambdaQuery();
		if (!user.role().seesAllData()) {
			wrapper.eq(ResearchPlan::getCreateBy, user.id());
		}
		wrapper.eq(StringUtils.hasText(semester), ResearchPlan::getSemester, semester)
				.eq(StringUtils.hasText(subject), ResearchPlan::getSubject, subject)
				.eq(StringUtils.hasText(grade), ResearchPlan::getGrade, grade)
				.eq(StringUtils.hasText(classType), ResearchPlan::getClassType, classType)
				.eq(StringUtils.hasText(status), ResearchPlan::getStatus, status)
				.and(StringUtils.hasText(keyword),
						w -> w.like(ResearchPlan::getTitle, keyword).or().like(ResearchPlan::getOwnerName, keyword))
				.orderByDesc(ResearchPlan::getUpdateAt);
		Page<ResearchPlan> page = planMapper.selectPage(new Page<>(current, pageSize), wrapper);
		return new PageResult<>(page.getTotal(),
				page.getRecords().stream().map(p -> PlanView.of(p, p.getOwnerName())).toList(), page.getCurrent(),
				page.getSize());
	}

	public PlanView detail(String id) {
		ResearchPlan plan = requireAccessible(id);
		return PlanView.of(plan, plan.getOwnerName());
	}

	public ResearchPlan requireAccessible(String id) {
		CurrentUser user = UserContext.require();
		ResearchPlan plan = planMapper.selectById(id);
		if (plan == null) {
			throw new BizException(HttpStatus.NOT_FOUND, "方案不存在或已删除");
		}
		if (!user.role().seesAllData() && !user.id().equals(plan.getCreateBy())) {
			throw new BizException(HttpStatus.FORBIDDEN, "无权查看该方案");
		}
		return plan;
	}

	@Transactional
	public PlanView create(PlanSaveRequest request) {
		CurrentUser user = UserContext.require();
		ResearchPlan plan = new ResearchPlan();
		apply(plan, request);
		plan.setStatus(PlanStatus.DRAFT);
		plan.setOwnerName(user.name());
		planMapper.insert(plan);
		flowLogService.record(BIZ_TYPE, plan.getId(), "CREATE", null, PlanStatus.DRAFT, user, null);
		return PlanView.of(plan, plan.getOwnerName());
	}

	@Transactional
	public PlanView update(String id, PlanSaveRequest request) {
		CurrentUser user = UserContext.require();
		ResearchPlan plan = requireAccessible(id);
		requireEditable(plan);
		apply(plan, request);
		planMapper.updateById(plan);
		flowLogService.record(BIZ_TYPE, id, "UPDATE", plan.getStatus(), plan.getStatus(), user, null);
		return PlanView.of(planMapper.selectById(id), plan.getOwnerName());
	}

	@Transactional
	public void delete(String id) {
		CurrentUser user = UserContext.require();
		ResearchPlan plan = requireAccessible(id);
		requireEditable(plan);
		planMapper.deleteById(id);
		flowLogService.record(BIZ_TYPE, id, "DELETE", plan.getStatus(), null, user, null);
	}

	@Transactional
	public PlanView submit(String id) {
		CurrentUser user = UserContext.require();
		ResearchPlan plan = requireAccessible(id);
		if (!PlanStatus.DRAFT.equals(plan.getStatus()) && !PlanStatus.REJECTED.equals(plan.getStatus())) {
			throw new BizException("当前状态不能提交审核");
		}
		String from = plan.getStatus();
		plan.setStatus(PlanStatus.PENDING);
		plan.setSubmitBy(user.name());
		plan.setSubmitAt(LocalDateTime.now());
		plan.setReviewRemark(null);
		planMapper.updateById(plan);
		flowLogService.record(BIZ_TYPE, id, "SUBMIT", from, PlanStatus.PENDING, user, null);
		return PlanView.of(plan, plan.getOwnerName());
	}

	@Transactional
	public PlanView review(String id, PlanReviewRequest request) {
		CurrentUser user = UserContext.require();
		if (!user.role().canReview()) {
			throw new BizException(HttpStatus.FORBIDDEN, "只有教学主管可以审核方案");
		}
		ResearchPlan plan = planMapper.selectById(id);
		if (plan == null) {
			throw new BizException(HttpStatus.NOT_FOUND, "方案不存在或已删除");
		}
		if (!PlanStatus.PENDING.equals(plan.getStatus())) {
			throw new BizException("只有待审核方案可以审核");
		}
		if (!request.approve() && !StringUtils.hasText(request.remark())) {
			throw new BizException("驳回时必须填写审核意见");
		}
		if (request.approve()) {
			Long conflict = planMapper.selectCount(Wrappers.<ResearchPlan>lambdaQuery()
					.eq(ResearchPlan::getSemester, plan.getSemester())
					.eq(ResearchPlan::getSubject, plan.getSubject()).eq(ResearchPlan::getGrade, plan.getGrade())
					.eq(ResearchPlan::getClassType, plan.getClassType())
					.eq(ResearchPlan::getStatus, PlanStatus.APPROVED).ne(ResearchPlan::getId, id));
			if (conflict != null && conflict > 0) {
				throw new BizException(HttpStatus.CONFLICT, "该学期同一学科、年级、班级类型已有已发布方案，请先撤回原方案");
			}
			plan.setStatus(PlanStatus.APPROVED);
			plan.setPublishedKey("1");
			plan.setApproveBy(user.name());
			plan.setApproveAt(LocalDateTime.now());
			plan.setReviewRemark(request.remark());
			try {
				planMapper.updateById(plan);
			}
			catch (DuplicateKeyException e) {
				throw new BizException(HttpStatus.CONFLICT, "该学期同一学科、年级、班级类型已有已发布方案，请先撤回原方案");
			}
			flowLogService.record(BIZ_TYPE, id, "APPROVE", PlanStatus.PENDING, PlanStatus.APPROVED, user,
					request.remark());
		}
		else {
			plan.setStatus(PlanStatus.REJECTED);
			plan.setReviewRemark(request.remark());
			plan.setApproveBy(user.name());
			plan.setApproveAt(LocalDateTime.now());
			planMapper.updateById(plan);
			flowLogService.record(BIZ_TYPE, id, "REJECT", PlanStatus.PENDING, PlanStatus.REJECTED, user,
					request.remark());
		}
		return PlanView.of(plan, plan.getOwnerName());
	}

	@Transactional
	public PlanView withdraw(String id) {
		CurrentUser user = UserContext.require();
		ResearchPlan plan = requireAccessible(id);
		if (!PlanStatus.APPROVED.equals(plan.getStatus())) {
			throw new BizException("只有已发布方案可以撤回");
		}
		plan.setStatus(PlanStatus.DRAFT);
		plan.setPublishedKey(null);
		plan.setApproveBy(null);
		plan.setApproveAt(null);
		planMapper.updateById(plan);
		flowLogService.record(BIZ_TYPE, id, "WITHDRAW", PlanStatus.APPROVED, PlanStatus.DRAFT, user, null);
		return PlanView.of(plan, plan.getOwnerName());
	}

	public java.util.List<ResearchFlowLog> logs(String id) {
		requireAccessible(id);
		return flowLogService.list(BIZ_TYPE, id);
	}

	private void requireEditable(ResearchPlan plan) {
		if (!PlanStatus.DRAFT.equals(plan.getStatus()) && !PlanStatus.REJECTED.equals(plan.getStatus())) {
			throw new BizException("只有草稿或已驳回方案可以修改或删除");
		}
	}

	private void apply(ResearchPlan plan, PlanSaveRequest request) {
		plan.setSemester(request.semester().trim());
		plan.setSubject(request.subject().trim());
		plan.setGrade(request.grade().trim());
		plan.setClassType(request.classType().trim());
		plan.setTitle(request.title().trim());
		plan.setGoal(request.goal());
		plan.setOutline(request.outline());
		plan.setClassHours(request.classHours());
	}

}

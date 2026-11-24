package cn.wisestar.research.plan;

import java.util.List;

import javax.validation.Valid;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cn.wisestar.research.common.PageResult;
import cn.wisestar.research.common.R;
import cn.wisestar.research.flow.ResearchFlowLog;
import cn.wisestar.research.plan.dto.PlanReviewRequest;
import cn.wisestar.research.plan.dto.PlanSaveRequest;
import cn.wisestar.research.plan.dto.PlanView;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/plans")
@RequiredArgsConstructor
public class PlanController {

	private final PlanService planService;

	@GetMapping
	public R<PageResult<PlanView>> list(@RequestParam(defaultValue = "1") long current,
			@RequestParam(defaultValue = "10") long pageSize, @RequestParam(required = false) String semester,
			@RequestParam(required = false) String subject, @RequestParam(required = false) String grade,
			@RequestParam(required = false) String classType, @RequestParam(required = false) String status,
			@RequestParam(required = false) String keyword) {
		return R.ok(planService.list(current, pageSize, semester, subject, grade, classType, status, keyword));
	}

	@GetMapping("/{id}")
	public R<PlanView> detail(@PathVariable String id) {
		return R.ok(planService.detail(id));
	}

	@PostMapping
	public R<PlanView> create(@RequestBody @Valid PlanSaveRequest request) {
		return R.ok(planService.create(request));
	}

	@PutMapping("/{id}")
	public R<PlanView> update(@PathVariable String id, @RequestBody @Valid PlanSaveRequest request) {
		return R.ok(planService.update(id, request));
	}

	@DeleteMapping("/{id}")
	public R<Void> delete(@PathVariable String id) {
		planService.delete(id);
		return R.ok();
	}

	@PostMapping("/{id}/submit")
	public R<PlanView> submit(@PathVariable String id) {
		return R.ok(planService.submit(id));
	}

	@PostMapping("/{id}/review")
	public R<PlanView> review(@PathVariable String id, @RequestBody @Valid PlanReviewRequest request) {
		return R.ok(planService.review(id, request));
	}

	@PostMapping("/{id}/withdraw")
	public R<PlanView> withdraw(@PathVariable String id) {
		return R.ok(planService.withdraw(id));
	}

	@GetMapping("/{id}/logs")
	public R<List<ResearchFlowLog>> logs(@PathVariable String id) {
		return R.ok(planService.logs(id));
	}

}

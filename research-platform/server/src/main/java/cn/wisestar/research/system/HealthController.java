package cn.wisestar.research.system;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.wisestar.research.common.R;

@RestController
public class HealthController {

	@GetMapping("/health")
	public R<Map<String, Object>> health() {
		return R.ok(Map.of("status", "UP", "app", "wisestar-research"));
	}

}

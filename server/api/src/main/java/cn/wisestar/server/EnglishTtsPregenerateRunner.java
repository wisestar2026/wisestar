package cn.wisestar.server;

import cn.wisestar.server.domain.dto.english.EnglishTtsPregenerateSummary;
import cn.wisestar.server.service.EnglishTtsPregenerateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

/**
 * 英语课程音频预生成命令行入口。
 *
 * <p>由部署脚本以 {@code -Denglish.tts.pregenerate.enabled=true} 触发，配合
 * {@code spring.main.web-application-type=none} 在后台跑完即退出，不占用 HTTP 端口。</p>
 *
 * @author wisestar
 * @date 2026/11/19
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "english.tts.pregenerate.enabled", havingValue = "true")
public class EnglishTtsPregenerateRunner implements ApplicationRunner {

	private final EnglishTtsPregenerateService pregenerateService;

	private final ConfigurableApplicationContext context;

	@Override
	public void run(ApplicationArguments args) {
		int exitCode = 0;
		try {
			EnglishTtsPregenerateSummary summary = pregenerateService.pregenerate();
			for (EnglishTtsPregenerateSummary.Source source : summary.getSources().values()) {
				log.info("语音预生成来源：{}，共 {}，新增 {}，命中 {}，失败 {}", source.getName(), source.getTotal(),
						source.getGenerated(), source.getCached(), source.getFailed());
			}
			log.info("语音预生成完成：共 {} 条，新增 {}，命中 {}，失败 {}，耗时 {} 秒", summary.getTotal(),
					summary.getGenerated(), summary.getCached(), summary.getFailed(),
					summary.getElapsedMillis() / 1000);
			if (summary.getFailed() > 0) {
				exitCode = 2;
			}
		} catch (Exception e) {
			log.error("语音预生成失败", e);
			exitCode = 1;
		} finally {
			int code = exitCode;
			SpringApplication.exit(context, () -> code);
			System.exit(code);
		}
	}

}

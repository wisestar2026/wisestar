package cn.wisestar.server.impl;

import cn.wisestar.server.service.EnglishTtsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * 英语朗读音频合成实现：本地神经语音（Piper）优先，回退 espeak-ng；lame 转 mp3，按文本哈希落盘缓存。
 *
 * <p>不依赖任何外部 API Key，也不需要学员设备安装英文语音包。Piper 首次合成要加载 60MB 模型（约 2~4 秒），
 * 因此按单元把整篇课文放进后台线程池预热，学员点「听原音」时多半已命中缓存。</p>
 *
 * @author wisestar
 * @date 2026/10/24
 */
@Slf4j
@Service
public class EnglishTtsServiceImpl implements EnglishTtsService {

	/** 主引擎：piper / espeak / none */
	@Value("${english.tts.engine:piper}")
	private String engine;

	/** 主引擎不可用时的回退引擎，空表示不回退 */
	@Value("${english.tts.fallback-engine:espeak}")
	private String fallbackEngine;

	/** espeak-ng 可执行文件 */
	@Value("${english.tts.espeak-bin:espeak-ng}")
	private String espeakBin;

	/** lame 可执行文件（缺失时直接返回 wav） */
	@Value("${english.tts.lame-bin:lame}")
	private String lameBin;

	/** espeak 音色 */
	@Value("${english.tts.voice:en-gb}")
	private String voice;

	/** Piper 可执行文件 */
	@Value("${english.tts.piper-bin:/opt/wisestar-tts/venv/bin/piper}")
	private String piperBin;

	/** Piper 音色模型（.onnx） */
	@Value("${english.tts.piper-voice:/opt/wisestar-tts/voices/en_GB-jenny_dioco-medium.onnx}")
	private String piperVoice;

	/** 默认语速（espeak 词/分钟口径，Piper 按比例换算；109 ≈ 常速的 0.75 倍） */
	@Value("${english.tts.speed:109}")
	private int defaultSpeed;

	/** espeak 默认语速，作为 Piper length-scale 的换算基准 */
	private static final int REFERENCE_SPEED = 145;

	/** 单句最大字符数 */
	@Value("${english.tts.max-chars:240}")
	private int maxChars;

	/** Piper 句间停顿秒数：句号/问号/感叹号后多留一点，方便学员跟读换气 */
	@Value("${english.tts.sentence-silence:0.3}")
	private double sentenceSilence;

	/** 缓存目录，空则用临时目录 */
	@Value("${english.tts.cache-dir:}")
	private String cacheDir;

	/** 合成超时（秒） */
	@Value("${english.tts.timeout-seconds:30}")
	private int timeoutSeconds;

	/** nice 命令是否可用（用于给后台预热降优先级） */
	private volatile Boolean niceReady;

	/** 预热线程数 */
	@Value("${english.tts.warmup-threads:2}")
	private int warmupThreads;

	/** 是否启用常驻 Piper 进程（模型只加载一次，单句约 0.7s；关闭则每次请求重载模型） */
	@Value("${english.tts.daemon:true}")
	private boolean daemonEnabled;

	/** 常驻进程上限（按语速各一个，默认 109 与 125 各一个） */
	@Value("${english.tts.daemon-max:2}")
	private int daemonMax;

	/** 常驻进程闲置回收秒数：长时间没人播就退出，把常驻进程占用的内存还给系统 */
	@Value("${english.tts.daemon-idle-seconds:0}")
	private long daemonIdleSeconds;

	/** 启动时预热的语速（逗号分隔）：默认句子/课文 109 与单词练习 125 */
	@Value("${english.tts.preload-speeds:109,125}")
	private String preloadSpeeds;

	private static final Pattern BLANK = Pattern.compile("\\s+");

	/** 预热时每个 Piper 进程负责的句子数：模型只加载一次，摊薄到多条 */
	private static final int WARMUP_BATCH_SIZE = 10;

	/** 启动预热文本：走完整「常驻合成 → mp3」链路，把模型首帧和转码开销都留在启动阶段 */
	private static final String PRELOAD_TEXT = "Hello, nice to meet you. Today is a beautiful day, and we are going to learn English together.";

	/** 预热批间隔（毫秒）：给接口请求让出 CPU */
	private static final long WARMUP_BATCH_PAUSE_MILLIS = 250L;

	/** 常驻进程单句等待上限（毫秒）：超时视为进程卡死，杀掉后回退一次性进程 */
	private static final long DAEMON_WAIT_MILLIS = 30_000L;

	/** 缓存键版本：合成链路修复后递增，旧的有问题音频自动失效 */
	private static final String CACHE_VERSION = "v3";

	/** 按语速缓存的常驻 Piper 进程 */
	private final Map<Integer, PiperDaemon> daemons = new ConcurrentHashMap<>();

	/** 允许常驻的语速：来自 preload-speeds，未配置时默认 109/125 */
	private final Set<Integer> daemonRates = ConcurrentHashMap.newKeySet();

	/** 首选语速：常驻名额满时优先保留它（默认取 preload-speeds 第一项，即 109） */
	private volatile int preferredRate = -1;

	private volatile Boolean ready;

	private volatile boolean mp3Ready;

	private volatile boolean espeakReady;

	private volatile boolean piperReady;

	private Path cachePath;

	private ThreadPoolExecutor warmupPool;

	@Override
	public boolean available() {
		if (ready == null) {
			synchronized (this) {
				if (ready == null) {
					ready = checkEngines();
				}
			}
		}
		if (ready) {
			initDaemonRates();
		}
		return ready;
	}

	@Override
	public String contentType() {
		available();
		return mp3Ready ? "audio/mpeg" : "audio/wav";
	}

	@Override
	public boolean cached(String text, String speed) {
		// 先完成引擎探测，确保 mp3Ready 等标志已初始化，否则会按 .wav 去查 .mp3 缓存
		available();
		String normalized = normalize(text);
		if (normalized == null) {
			return false;
		}
		return cacheExists(normalized, parseSpeed(speed));
	}

	@Override
	public String normalizeText(String text) {
		return normalize(text);
	}

	@Override
	public byte[] synthesize(String text, String speed) {
		String normalized = normalize(text);
		if (normalized == null || !available()) {
			return null;
		}
		int rate = parseSpeed(speed);
		Path cached = cacheFile(normalized, rate);
		long started = System.currentTimeMillis();
		boolean viaDaemon = false;
		try {
			if (cached != null && Files.isReadable(cached) && Files.size(cached) > 0) {
				return Files.readAllBytes(cached);
			}
			byte[] wav = null;
			if (piperReady) {
				PiperDaemon daemon = daemon(rate);
				if (daemon != null) {
					viaDaemon = true;
					wav = daemon.synthesize(normalized);
				}
				if (wav == null || wav.length == 0) {
					viaDaemon = false;
					wav = piper(normalized, rate);
				}
			}
			if ((wav == null || wav.length == 0) && espeakReady) {
				wav = espeak(normalized, rate);
			}
			if (wav == null || wav.length == 0) {
				return null;
			}
			byte[] audio = wav;
			if (mp3Ready) {
				byte[] mp3 = toMp3(wav);
				if (mp3 != null && mp3.length > 0) {
					audio = mp3;
				}
			}
			saveCache(cached, audio);
			long cost = System.currentTimeMillis() - started;
			if (cost > 1000) {
				log.info("语音合成耗时 {}ms：语速 {}，常驻进程 {}，文本 {}", cost, rate, viaDaemon ? "命中" : "未命中",
						abbreviate(normalized));
			}
			return audio;
		} catch (Exception e) {
			log.warn("服务端语音合成失败：text={}，原因={}", abbreviate(normalized), e.getMessage());
			return null;
		}
	}

	@Override
	public void warmUp(Collection<String> texts, String speed) {
		if (texts == null || texts.isEmpty() || !available()) {
			return;
		}
		int rate = parseSpeed(speed);
		Set<String> unique = new LinkedHashSet<>();
		for (String text : texts) {
			String normalized = normalize(text);
			if (normalized != null) {
				unique.add(normalized);
			}
		}
		if (unique.isEmpty()) {
			return;
		}
		ThreadPoolExecutor pool = warmupPool();
		List<String> pending = new ArrayList<>(unique);
		pending.removeIf(text -> cacheExists(text, rate));
		for (int i = 0; i < pending.size(); i += WARMUP_BATCH_SIZE) {
			List<String> batch = new ArrayList<>(
					pending.subList(i, Math.min(pending.size(), i + WARMUP_BATCH_SIZE)));
			try {
				// 常驻进程可用时逐句走常驻进程（每句约 60ms，不重载模型）；否则退回一次性批量进程
				pool.execute(() -> {
					if (daemonEnabled && piperReady && daemon(rate) != null) {
						batch.forEach(text -> synthesize(text, String.valueOf(rate)));
					} else {
						synthesizeBatch(batch, rate);
					}
				});
			} catch (RejectedExecutionException e) {
				log.debug("语音预热队列已满，跳过剩余文本");
				return;
			}
		}
	}

	/**
	 * 批量预热：一个 Piper 进程读多行文本（模型只加载一次），再逐条转 mp3 落缓存。
	 *
	 * @param batch 已归一化、尚未缓存的文本
	 */
	private void synthesizeBatch(List<String> batch, int rate) {
		if (!piperReady) {
			batch.forEach(text -> synthesize(text, String.valueOf(rate)));
			return;
		}
		Path dir = null;
		try {
			dir = Files.createTempDirectory("wisestar-piper-batch-");
			List<String> cmd = new ArrayList<>();
			// 后台预热降优先级：两核机器上避免 Piper 抢满 CPU 把接口响应拖到秒级
			if (niceAvailable()) {
				cmd.add("nice");
				cmd.add("-n");
				cmd.add("15");
			}
			cmd.add(piperBin);
			cmd.add("-m");
			cmd.add(piperVoice);
			cmd.add("-d");
			cmd.add(dir.toString());
			cmd.add("--length-scale");
			cmd.add(String.format(Locale.ROOT, "%.2f", lengthScale(rate)));
			addSentenceSilence(cmd);
			byte[] input = (String.join("\n", batch) + "\n").getBytes(StandardCharsets.UTF_8);
			if (run(cmd, input) == null) {
				log.warn("批量语音预热失败，改为逐条合成");
				batch.forEach(text -> synthesize(text, String.valueOf(rate)));
				return;
			}
			List<Path> wavs = new ArrayList<>();
			try (java.util.stream.Stream<Path> stream = Files.list(dir)) {
				stream.filter(path -> path.toString().endsWith(".wav"))
						.sorted(Comparator.comparingLong(this::lastModifiedMillis)).forEach(wavs::add);
			}
			if (wavs.size() != batch.size()) {
				log.warn("批量语音预热输出数量不符：期望 {}，实际 {}", batch.size(), wavs.size());
				batch.forEach(text -> synthesize(text, String.valueOf(rate)));
				return;
			}
			for (int i = 0; i < batch.size(); i += 1) {
				byte[] wav = Files.readAllBytes(wavs.get(i));
				byte[] audio = wav;
				if (mp3Ready) {
					byte[] mp3 = toMp3(wav);
					if (mp3 != null && mp3.length > 0) {
						audio = mp3;
					}
				}
				saveCache(cacheFile(batch.get(i), rate), audio);
			}
			log.info("语音预热完成 {} 条", batch.size());
			// 批与批之间留个空档，双核机器上不要让 Piper 长时间 100% 占满
			Thread.sleep(WARMUP_BATCH_PAUSE_MILLIS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (Exception e) {
			log.warn("批量语音预热异常：{}", e.getMessage());
		} finally {
			deleteTree(dir);
		}
	}

	private long lastModifiedMillis(Path path) {
		try {
			return Files.getLastModifiedTime(path).toMillis();
		} catch (IOException e) {
			return 0L;
		}
	}

	/**
	 * 按语速取常驻 Piper 进程；没有则按需启动（模型冷加载 10 秒以上，通常已在启动阶段完成）。
	 *
	 * @param rate 语速
	 * @return 可用进程；未启用、超出上限或启动失败返回 null，调用方回退一次性进程
	 */
	private PiperDaemon daemon(int rate) {
		if (!daemonEnabled || !piperReady) {
			return null;
		}
		PiperDaemon existing = daemons.get(rate);
		if (existing != null && existing.alive()) {
			return existing;
		}
		// 只为 preload-speeds 里的语速起常驻进程（默认 109/125）；未列入的语速走一次性合成，
		// 避免首句请求抢占唯一名额，把单词练习的常驻进程挤掉
		if (!daemonRates.contains(rate) && !(daemonRates.isEmpty() && rate == defaultSpeed)) {
			return null;
		}
		synchronized (daemons) {
			existing = daemons.get(rate);
			if (existing != null && existing.alive()) {
				return existing;
			}
			if (existing != null) {
				existing.close();
			}
			closeIdleDaemons();
			int limit = Math.max(1, daemonMax);
			if (daemons.size() >= limit) {
				if (rate != preferredRate) {
					return null;
				}
				// 首选语速要占名额：回收其他语速的常驻进程（课文那档改走批量预热）
				List<Integer> victims = new ArrayList<>(daemons.keySet());
				victims.stream().filter(other -> other != rate).forEach(other -> daemons.get(other).close());
				if (daemons.size() >= limit) {
					return null;
				}
			}
			PiperDaemon created = new PiperDaemon(rate);
			if (!created.start()) {
				return null;
			}
			daemons.put(rate, created);
			return created;
		}
	}

	/** 回收闲置常驻进程：长时间没人播就退出，把 150MB 左右的内存还给系统 */
	private void closeIdleDaemons() {
		if (daemonIdleSeconds <= 0) {
			return;
		}
		long idleMillis = daemonIdleSeconds * 1000L;
		for (Map.Entry<Integer, PiperDaemon> entry : daemons.entrySet()) {
			PiperDaemon daemon = entry.getValue();
			if (daemon.idleMillis() > idleMillis) {
				daemon.close();
			}
		}
	}

	/** 持续读走子进程输出，避免管道写满后 Piper 阻塞；「Wrote ...」行作为合成完成信号 */
	private void drain(InputStream stream, BlockingQueue<Path> written) {
		Thread thread = new Thread(() -> {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					int index = line.indexOf("Wrote ");
					if (index >= 0) {
						String path = line.substring(index + "Wrote ".length()).trim();
						if (!path.isEmpty()) {
							written.offer(Paths.get(path));
						}
					}
				}
			} catch (IOException e) {
				log.debug("常驻语音进程输出流已关闭：{}", e.getMessage());
			}
		}, "tts-daemon-drain");
		thread.setDaemon(true);
		thread.start();
	}

	/**
	 * 常驻 Piper 进程：模型只加载一次，逐行喂文本、从输出目录取 wav，按语速串行合成。
	 *
	 * <p>Piper 的 CLI 只有 {@code --length-scale} 能改语速且是启动参数，因此每个语速一个进程。</p>
	 */
	private final class PiperDaemon {

		private final int rate;

		private Process process;

		private Writer stdin;

		private Path workDir;

		/** Piper 每写完一个 wav 输出一行「Wrote <路径>」，据此判断文件已完整落盘 */
		private final BlockingQueue<Path> written = new LinkedBlockingQueue<>();

		private volatile long lastUsed = System.currentTimeMillis();

		PiperDaemon(int rate) {
			this.rate = rate;
		}

		boolean alive() {
			return process != null && process.isAlive();
		}

		long idleMillis() {
			return System.currentTimeMillis() - lastUsed;
		}

		boolean start() {
			try {
				workDir = Files.createTempDirectory("wisestar-piper-daemon-");
				List<String> cmd = new ArrayList<>();
				cmd.add(piperBin);
				cmd.add("-m");
				cmd.add(piperVoice);
				cmd.add("-d");
				cmd.add(workDir.toString());
				cmd.add("--length-scale");
				cmd.add(String.format(Locale.ROOT, "%.2f", lengthScale(rate)));
				addSentenceSilence(cmd);
				ProcessBuilder builder = new ProcessBuilder(cmd);
				builder.redirectErrorStream(true);
				process = builder.start();
				stdin = new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8);
				drain(process.getInputStream(), written);
				log.info("常驻语音进程已启动：语速 {}，音色 {}", rate, piperVoice);
				return true;
			} catch (Exception e) {
				log.warn("常驻语音进程启动失败：{}", e.getMessage());
				close();
				return false;
			}
		}

		/** 同步合成一句：写一行文本进去，等 Piper 报出「Wrote」后再读文件 */
		synchronized byte[] synthesize(String text) {
			lastUsed = System.currentTimeMillis();
			if (!alive()) {
				return null;
			}
			try {
				// 先清掉可能残留的输出（上次超时或读取失败），避免把上一句的音频当成本句
				clearWavs();
				written.clear();
				stdin.write(text);
				stdin.write("\n");
				stdin.flush();
				Path wav = awaitWav();
				if (wav == null) {
					log.warn("常驻语音进程无输出，重启后回退一次性合成");
					close();
					return null;
				}
				byte[] data = Files.readAllBytes(wav);
				Files.deleteIfExists(wav);
				return data;
			} catch (Exception e) {
				log.warn("常驻语音进程合成失败：{}", e.getMessage());
				close();
				return null;
			}
		}

		/** 清空输出目录里的 wav，保证随后出现的文件一定是本次请求的产物 */
		private void clearWavs() {
			if (workDir == null) {
				return;
			}
			try (java.util.stream.Stream<Path> stream = Files.list(workDir)) {
				stream.filter(path -> path.toString().endsWith(".wav"))
						.forEach(EnglishTtsServiceImpl.this::deleteQuietly);
			} catch (IOException e) {
				log.debug("常驻语音输出目录清理失败：{}", e.getMessage());
			}
		}

		/** 等 Piper 报出「Wrote」：此时文件已写完并关闭，直接读不会拿到半截音频 */
		private Path awaitWav() throws InterruptedException {
			long deadline = System.currentTimeMillis() + DAEMON_WAIT_MILLIS;
			while (System.currentTimeMillis() < deadline) {
				long remain = Math.max(1L, deadline - System.currentTimeMillis());
				Path wav = written.poll(remain, TimeUnit.MILLISECONDS);
				if (wav == null) {
					return null;
				}
				if (!alive()) {
					return null;
				}
				if (Files.isReadable(wav)) {
					return wav;
				}
			}
			return null;
		}

		void close() {
			Process current = process;
			process = null;
			if (current != null) {
				try {
					current.destroy();
				} catch (Exception e) {
					log.debug("常驻语音进程退出失败：{}", e.getMessage());
				}
			}
			if (stdin != null) {
				try {
					stdin.close();
				} catch (IOException e) {
					log.debug("常驻语音进程输入流关闭失败：{}", e.getMessage());
				}
				stdin = null;
			}
			deleteTree(workDir);
			workDir = null;
			daemons.remove(rate);
		}
	}

	/** 缓存是否已存在（缓存键含语速，须按实际播放语速判断） */
	private boolean cacheExists(String text, int rate) {
		Path target = cacheFile(text, rate);
		return target != null && Files.isReadable(target);
	}

	private void deleteTree(Path dir) {
		if (dir == null) {
			return;
		}
		try (java.util.stream.Stream<Path> stream = Files.walk(dir)) {
			stream.sorted(Comparator.reverseOrder()).forEach(this::deleteQuietly);
		} catch (IOException e) {
			log.debug("临时目录清理失败：{}", e.getMessage());
		}
	}

	/** 懒创建预热线程池：守护线程 + 有界队列，避免拖垮请求线程 */
	private ThreadPoolExecutor warmupPool() {
		if (warmupPool == null) {
			synchronized (this) {
				if (warmupPool == null) {
					AtomicInteger seq = new AtomicInteger();
					warmupPool = new ThreadPoolExecutor(Math.max(1, warmupThreads), Math.max(1, warmupThreads), 60,
							TimeUnit.SECONDS, new ArrayBlockingQueue<>(64), runnable -> {
								Thread thread = new Thread(runnable, "tts-warmup-" + seq.incrementAndGet());
								thread.setDaemon(true);
								return thread;
							}, new ThreadPoolExecutor.DiscardPolicy());
					warmupPool.allowCoreThreadTimeOut(true);
				}
			}
		}
		return warmupPool;
	}

	/**
	 * 初始化允许常驻的语速。
	 *
	 * <p>不能只在 ApplicationReadyEvent 里做：批量预生成走 ApplicationRunner，
	 * 会在 ready 事件之前开始合成，否则 125 这类非默认语速会被误判为「不允许常驻」，
	 * 每个单词都退回一次性 Piper，速度慢几十倍。</p>
	 */
	private void initDaemonRates() {
		if (!daemonRates.isEmpty()) {
			return;
		}
		synchronized (daemonRates) {
			if (!daemonRates.isEmpty()) {
				return;
			}
			List<Integer> rates = new ArrayList<>();
			if (preloadSpeeds != null) {
				for (String part : preloadSpeeds.split(",")) {
					if (!part.trim().isEmpty()) {
						rates.add(parseSpeed(part));
					}
				}
			}
			if (rates.isEmpty()) {
				rates.add(defaultSpeed);
			}
			preferredRate = rates.get(0);
			daemonRates.addAll(rates);
		}
	}

	/**
	 * 启动后预热 Piper 模型：模型冷加载要 10 秒以上（磁盘 + ONNX 初始化），
	 * 放在启动阶段跑掉，学员第一次点「听发音」就不用等这一下。
	 *
	 * <p>按 {@code english.tts.preload-speeds}（默认 109,125）起常驻进程，
	 * 句子/课文 109 与单词练习 125 都直接命中已加载的模型，首次点击也在 1s 内。</p>
	 */
	@EventListener(ApplicationReadyEvent.class)
	public void preloadModel() {
		if (!available()) {
			return;
		}
		List<Integer> rates = new ArrayList<>(daemonRates);
		if (rates.isEmpty()) {
			rates.add(defaultSpeed);
		}
		try {
			warmupPool().execute(() -> rates.forEach(rate -> {
				// 起进程加载模型后走一次完整合成（含 mp3 转码），把 ONNX 首帧与 lame 启动开销都放在启动阶段
				daemon(rate);
				synthesize(PRELOAD_TEXT, String.valueOf(rate));
			}));
		} catch (RejectedExecutionException e) {
			log.debug("启动预热队列已满，跳过模型预热");
		}
	}

	@PreDestroy
	public void shutdown() {
		if (warmupPool != null) {
			warmupPool.shutdownNow();
		}
		daemons.values().forEach(PiperDaemon::close);
		daemons.clear();
	}

	/** 探测引擎可用性 */
	private boolean checkEngines() {
		String primary = engine == null ? "none" : engine.trim().toLowerCase(Locale.ROOT);
		if ("none".equals(primary)) {
			log.info("服务端语音合成未启用（english.tts.engine=none）");
			return false;
		}
		piperReady = "piper".equals(primary) && piperUsable();
		espeakReady = "espeak".equals(primary) || ("espeak".equalsIgnoreCase(fallbackEngine) && espeakUsable());
		mp3Ready = run(List.of(lameBin, "--version"), null) != null;
		boolean ok = piperReady || espeakReady;
		if (ok) {
			log.info("服务端语音合成已启用：piper={}（{}），espeak={}，mp3={}，cache={}", piperReady, piperVoice,
					espeakReady, mp3Ready, resolveCacheDir());
		} else {
			log.warn("服务端语音合成不可用：piper={} 模型或可执行文件缺失，espeak-ng 不可用", piperBin);
		}
		return ok;
	}

	private boolean piperUsable() {
		boolean binOk = Files.isExecutable(Paths.get(piperBin));
		boolean modelOk = Files.isRegularFile(Paths.get(piperVoice));
		if (!binOk || !modelOk) {
			log.warn("Piper 不可用：bin={}（{}），model={}（{}）", piperBin, binOk, piperVoice, modelOk);
			return false;
		}
		return true;
	}

	/** nice 是否存在：存在就给预热进程让路，缺了也不影响合成 */
	private boolean niceAvailable() {
		if (niceReady == null) {
			niceReady = Files.isExecutable(Paths.get("/usr/bin/nice"))
					|| Files.isExecutable(Paths.get("/bin/nice"))
					|| Files.isExecutable(Paths.get("/usr/local/bin/nice"));
		}
		return niceReady;
	}

	private boolean espeakUsable() {
		return run(List.of(espeakBin, "--version"), null) != null;
	}

	/**
	 * 调用 Piper 合成 wav（文本走标准输入，结果写临时文件）。
	 *
	 * @param text 已归一化的英文文本
	 * @param rate 语速（espeak 口径，换算为 length-scale）
	 * @return wav 字节
	 */
	private byte[] piper(String text, int rate) throws IOException {
		Path dir = Files.createTempDirectory("wisestar-piper-");
		Path wav = dir.resolve("out.wav");
		try {
			List<String> cmd = new ArrayList<>();
			cmd.add(piperBin);
			cmd.add("-m");
			cmd.add(piperVoice);
			cmd.add("-f");
			cmd.add(wav.toString());
			cmd.add("--length-scale");
			cmd.add(String.format(Locale.ROOT, "%.2f", lengthScale(rate)));
			addSentenceSilence(cmd);
			if (run(cmd, text.getBytes(StandardCharsets.UTF_8)) == null || !Files.isReadable(wav)) {
				return null;
			}
			return Files.readAllBytes(wav);
		} finally {
			deleteQuietly(wav);
			deleteQuietly(dir);
		}
	}

	/**
	 * 调用 espeak-ng 输出 wav（--stdout 直接写标准输出，避免临时文件）。
	 *
	 * @param text 已归一化的英文文本
	 * @param rate 语速
	 * @return wav 字节
	 */
	private byte[] espeak(String text, int rate) {
		List<String> cmd = new ArrayList<>();
		cmd.add(espeakBin);
		cmd.add("-v");
		cmd.add(voice);
		cmd.add("-s");
		cmd.add(String.valueOf(rate));
		cmd.add("-p");
		cmd.add("55");
		cmd.add("--stdout");
		cmd.add(text);
		return run(cmd, null);
	}

	/**
	 * wav 转 mp3（64kbps 单声道）。
	 *
	 * <p>32kbps 在 22050Hz 语音上齿音发闷、带明显压缩杂音，语言学习场景不能接受；
	 * 64kbps 体积约为 wav 的十分之一，课堂外放与人声清晰度都够用。</p>
	 *
	 * @param wav wav 字节
	 * @return mp3 字节，失败返回 null
	 */
	private byte[] toMp3(byte[] wav) throws IOException {
		Path tmpDir = Files.createTempDirectory("wisestar-tts-");
		Path wavFile = tmpDir.resolve("in.wav");
		Path mp3File = tmpDir.resolve("out.mp3");
		try {
			Files.write(wavFile, wav);
			List<String> cmd = List.of(lameBin, "-b", "64", "-m", "m", "-q", "2", "--quiet", wavFile.toString(),
					mp3File.toString());
			if (run(cmd, null) == null || !Files.isReadable(mp3File)) {
				return null;
			}
			return Files.readAllBytes(mp3File);
		} finally {
			deleteQuietly(wavFile);
			deleteQuietly(mp3File);
			deleteQuietly(tmpDir);
		}
	}

	/**
	 * 执行命令并返回标准输出。
	 *
	 * @param cmd   命令与参数
	 * @param stdin 写入标准输入的字节，null 表示不写
	 * @return 标准输出字节，非 0 退出或异常返回 null
	 */
	private byte[] run(List<String> cmd, byte[] stdin) {
		Process process = null;
		try {
			ProcessBuilder builder = new ProcessBuilder(cmd);
			builder.redirectErrorStream(false);
			// stderr 不参与返回值：直接丢弃，避免长时间合成时管道写满把子进程卡住
			builder.redirectError(ProcessBuilder.Redirect.DISCARD);
			process = builder.start();
			if (stdin != null) {
				try (OutputStream out = process.getOutputStream()) {
					out.write(stdin);
				}
			} else {
				process.getOutputStream().close();
			}
			byte[] out;
			try (InputStream in = process.getInputStream()) {
				out = in.readAllBytes();
			}
			if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
				process.destroyForcibly();
				log.warn("语音合成超时（{}s）：{}", timeoutSeconds, cmd.get(0));
				return null;
			}
			if (process.exitValue() != 0) {
				return null;
			}
			return out;
		} catch (Exception e) {
			if (process != null) {
				process.destroyForcibly();
			}
			log.debug("命令执行失败：{}，原因={}", cmd.get(0), e.getMessage());
			return null;
		}
	}

	/** 追加句间停顿参数（Piper 在每个句号/问号/感叹号后插入固定静音） */
	private void addSentenceSilence(List<String> cmd) {
		double seconds = Math.max(0d, Math.min(1.5d, sentenceSilence));
		cmd.add("--sentence-silence");
		cmd.add(String.format(Locale.ROOT, "%.2f", seconds));
	}

	/**
	 * espeak 语速（词/分钟）换算为 Piper 的 length-scale（>1 更慢）。
	 * 基准固定为 espeak 默认语速，不随配置漂移，这样调慢 speed 才能真正放慢朗读。
	 */
	private double lengthScale(int rate) {
		double scale = REFERENCE_SPEED / (double) Math.max(60, rate);
		return Math.max(0.7, Math.min(1.8, scale));
	}

	/** 文本归一化：合并空白、截断超长文本 */
	private String normalize(String text) {
		if (text == null) {
			return null;
		}
		String value = BLANK.matcher(text.trim()).replaceAll(" ").trim();
		if (value.isEmpty()) {
			return null;
		}
		if (value.length() > maxChars) {
			log.debug("语音文本超过 {} 字符，已截断：{}", maxChars, abbreviate(value));
			value = value.substring(0, maxChars);
		}
		return value;
	}

	private int parseSpeed(String speed) {
		if (speed == null || speed.trim().isEmpty()) {
			return defaultSpeed;
		}
		try {
			int rate = Integer.parseInt(speed.trim());
			return Math.max(80, Math.min(220, rate));
		} catch (NumberFormatException e) {
			return defaultSpeed;
		}
	}

	/** 缓存文件路径：引擎 + 音色 + 语速 + 文本 的 SHA-1 */
	private Path cacheFile(String text, int rate) {
		try {
			Path dir = resolveCacheDir();
			Files.createDirectories(dir);
			String key = CACHE_VERSION + "|" + engine + "|" + piperVoice + "|" + voice + "|" + rate + "|" + text;
			MessageDigest digest = MessageDigest.getInstance("SHA-1");
			byte[] hash = digest.digest(key.getBytes(StandardCharsets.UTF_8));
			StringBuilder name = new StringBuilder();
			for (byte b : hash) {
				name.append(String.format("%02x", b));
			}
			return dir.resolve(name + (mp3Ready ? ".mp3" : ".wav"));
		} catch (Exception e) {
			log.warn("语音缓存目录不可用：{}", e.getMessage());
			return null;
		}
	}

	private Path resolveCacheDir() {
		if (cachePath == null) {
			String dir = cacheDir;
			if (dir == null || dir.trim().isEmpty()) {
				dir = System.getProperty("java.io.tmpdir") + File.separator + "wisestar-tts";
			}
			cachePath = Paths.get(dir);
		}
		return cachePath;
	}

	/** 先写临时文件再原子移动，避免并发读到半截文件 */
	private void saveCache(Path target, byte[] audio) {
		if (target == null) {
			return;
		}
		Path tmp = null;
		try {
			tmp = Files.createTempFile(target.getParent(), "tts-", ".tmp");
			Files.write(tmp, audio);
			Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (Exception e) {
			log.debug("语音缓存写入失败：{}", e.getMessage());
			deleteQuietly(tmp);
		}
	}

	private void deleteQuietly(Path path) {
		if (path == null) {
			return;
		}
		try {
			Files.deleteIfExists(path);
		} catch (IOException e) {
			log.debug("临时文件清理失败：{}", e.getMessage());
		}
	}

	private String abbreviate(String text) {
		return text.length() <= 40 ? text : text.substring(0, 40) + "...";
	}
}

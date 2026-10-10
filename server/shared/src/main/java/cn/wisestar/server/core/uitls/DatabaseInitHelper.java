package cn.wisestar.server.core.uitls;

import cn.wisestar.server.domain.dto.DatabaseInitProperties;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * @author javahuang
 * @date 2022/1/25
 */
@Slf4j
public class DatabaseInitHelper {

	private static final String CONFIG_FILE = "application.properties";

	private static final String IP_PATTERN = "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

	private static final String INIT_DB_SCRIPT_FILE_NAME = "init-%s.sql";

	private static final String MYSQL_DRIVER_DOWNLOAD_URL = "https://repo1.maven.org/maven2/mysql/mysql-connector-java/8.0.28/mysql-connector-java-8.0.28.jar";

	static Scanner sc = new Scanner(System.in);

	private static String db = "mysql";

	/**
	 * 数据库初始化命令行入口：根据启动参数决定执行交互式初始化或按参数生成配置文件。
	 *
	 * @param args 启动命令行参数；首参为 "i" 时进入交互式初始化流程，否则解析参数生成配置文件
	 */
	public static void init(String[] args) {
		try {
			needCreateProperties();
			// 初始化启动配置文件，初始化数据库脚本
			if ("i".equals(args[0])) {
				initializeDatabase();
			}
			else {
				// 通过启动命令行参数来创建配置文件
				createFileFromProperties(setPropertiesFromArguments(args));
			}
		}
		finally {
			sc.close();
		}
	}

	/**
	 * 交互式数据库初始化：提示用户选择数据库类型（MySQL/H2），并据此触发后续初始化。
	 */
	private static void initializeDatabase() {
		colorOutput("请选择您的数据库类型 : ");
		colorOutput("1) MySql");
		colorOutput("2) H2");
		String selection = sc.nextLine();
		switch (selection) {
		case "1":
			db = "mysql";
			break;
		case "2":
			db = "h2";
			break;
		default:
			return;
		}
		initDb();
	}

	/**
	 * 按当前选定的数据库类型执行初始化；目前仅支持 MySQL。
	 */
	private static void initDb() {
		if ("mysql".equals(db)) {
			initMySql();
		}
	}

	/**
	 * 执行 MySQL 初始化流程：收集连接参数、下载驱动、测试连接、生成启动配置文件并执行建库脚本。
	 */
	public static void initMySql() {
		// 设置连接参数
		DatabaseInitProperties properties = setProperties();
		// 下载数据库驱动
		downloadDriverJar(properties);
		// 测试数据库链接
		testDatabaseConnection(properties);
		// 创建启动文件
		createFileFromProperties(properties);
		// 执行数据库初始化操作
		initDatabaseSchema(properties);
		// 完成
		finish();
	}

	/**
	 * 执行 H2 数据库初始化（当前尚未实现）。
	 */
	public static void initH2() {
		// TODO
	}

	/**
	 * 确保启动配置文件存在，缺失时创建空文件。
	 *
	 * @return 始终返回 true，表示需要（重新）写入配置
	 */
	@SneakyThrows
	private static boolean needCreateProperties() {
		File f = new File(CONFIG_FILE);
		if (!f.exists()) {
			f.createNewFile();
		}
		// 总是覆盖之前的配置
		return true;
	}

	/**
	 * 通过交互式提问收集 MySQL 连接参数并组装初始化配置对象。
	 *
	 * @return 数据库初始化配置
	 */
	private static DatabaseInitProperties setProperties() {
		DatabaseInitProperties properties = new DatabaseInitProperties();
		properties.setApplicationName(setProperty("请输入应用名称:", "名称不能为空", ".+", "SurveyKing"));
		properties.setServerPort(Integer.parseInt(setProperty("请输入应用端口号(默认 1991):", "端口号不能为空", "\\d{1,5}", "1991")));
		properties.setDbIp(setProperty("请输入数据库地址(默认 127.0.0.1):", "IP 地址格式不正确，请重新输入", IP_PATTERN, "127.0.0.1"));
		properties.setDbPort(Integer.parseInt(setProperty("请输入数据库端口(默认 3306):", "端口格式不正确，请重新输入", "\\d{1,5}", "3306")));
		properties.setDbName(setProperty("请输入数据库名称:", "数据库名称不能为空", "\\w+", ""));
		properties.setUsername(setProperty("请输入数据库用户名:", "数据库用户名不能为空", ".+", ""));
		properties.setPassword(setProperty("请输入数据库密码:", "数据库密码不能为空", ".+", ""));
		properties.setDriverClassName("com.mysql.cj.jdbc.Driver");
		return properties;
	}

	/**
	 * 从命令行参数解析连接配置（端口 / URL / 用户名 / 密码）。
	 *
	 * @param args 形如 key=value 的启动参数数组
	 * @return 从参数解析得到的数据库初始化配置
	 */
	private static DatabaseInitProperties setPropertiesFromArguments(String[] args) {
		DatabaseInitProperties properties = new DatabaseInitProperties();
		for (String arg : args) {
			String[] kv = arg.split("=", 2);
			if (kv[0].toLowerCase().contains("port")) {
				properties.setServerPort(Integer.parseInt(kv[1]));
			}
			else if (kv[0].toLowerCase().contains("url")) {
				properties.setDataSourceUrl(kv[1]);
			}
			else if (kv[0].toLowerCase().contains("username")) {
				properties.setUsername(kv[1]);
			}
			else if (kv[0].toLowerCase().contains("password")) {
				properties.setPassword(kv[1]);
			}
		}

		return properties;
	}

	/**
	 * 交互式读取单个配置项，支持默认值与正则校验，非法输入时提示并重试。
	 *
	 * @param prompt       输入提示语
	 * @param errorMsg     校验失败时的错误提示
	 * @param regex        输入需匹配的正则表达式
	 * @param defaultValue 默认值，输入为空时采用
	 * @return 用户输入值或默认值
	 */
	private static String setProperty(String prompt, String errorMsg, String regex, String defaultValue) {
		String result;
		colorOutput(prompt);
		while (true) {
			result = sc.nextLine().trim();
			if (result.isEmpty() && !defaultValue.isEmpty()) {
				result = defaultValue;
				break;
			}
			else if (!result.matches(regex)) {
				colorOutput(errorMsg, 2);
				continue;
			}
			else {
				break;
			}
		}
		return result;
	}

	/**
	 * 检测数据库驱动是否可用，并在用户确认后下载驱动 JAR 包。
	 *
	 * @param properties 数据库初始化配置，其中的驱动类名用于检测驱动是否可用
	 */
	private static void downloadDriverJar(DatabaseInitProperties properties) {
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			Class.forName(properties.getDriverClassName());
		}
		catch (ClassNotFoundException e) {
			if (yesOrNo("未监测到数据库驱动，是否下载？")) {
				// TODO: 将数据库驱动下载到当前 lib 目录
				colorOutput("开始下载驱动");
			}
		}
	}

	/**
	 * 交互式测试数据库连接；连接不可用且用户确认时重新进入初始化流程。
	 *
	 * @param properties 数据库初始化配置
	 */
	private static void testDatabaseConnection(DatabaseInitProperties properties) {
		if (!yesOrNo("是否测试数据库连接？")) {
			return;
		}
		boolean reachable;
		try (Connection connection = DriverManager.getConnection(String.format("jdbc:mysql://%s:%d/%s",
				properties.getDbIp(), properties.getDbPort(), properties.getDbName()), properties.getUsername(),
				properties.getPassword())) {
			reachable = connection.isValid(10);
		}
		catch (SQLException e) {
			reachable = false;
		}
		if (reachable) {
			colorOutput("数据库连接成功", 1);
		}
		else {
			if (yesOrNo("数据库连接不可用，是否重新设置？")) {
				initMySql();
			}
		}

	}

	/**
	 * 数据库初始化
	 * @param properties
	 */
	@SneakyThrows
	private static void initDatabaseSchema(DatabaseInitProperties properties) {
		if (!yesOrNo("是否执行数据库初始化操作？")) {
			return;
		}
		try (Connection connection = DriverManager.getConnection(String.format("jdbc:mysql://%s:%d/%s",
				properties.getDbIp(), properties.getDbPort(), properties.getDbName()), properties.getUsername(),
				properties.getPassword())) {
			ScriptRunner runner = new ScriptRunner(connection, false, false);
			String file = getJarFilePath() + File.separator + String.format(INIT_DB_SCRIPT_FILE_NAME, db);
			if (!new File(file).exists()) {
				colorOutput(String.format("初始化脚本不存在 %s", file), 2);
				initDatabaseSchema(properties);
				return;
			}
			runner.runScript(new BufferedReader(new FileReader(file)));
		}
		catch (SQLException e) {
			log.error("数据库初始化失败", e);
		}
		colorOutput("数据库初始化完成", 1);
	}

	/**
	 * 输出初始化完成提示。
	 */
	private static void finish() {
		colorOutput(String.format("初始化完成，有问题请联系作者。"), 1);
	}

	/**
	 * 依据配置对象生成（覆盖写入）启动配置文件 application.properties。
	 *
	 * @param properties 数据库初始化配置
	 */
	@SneakyThrows
	private static void createFileFromProperties(DatabaseInitProperties properties) {
		File f = new File(CONFIG_FILE);
		List<String> fileContent = new ArrayList<>(Files.readAllLines(f.toPath(), StandardCharsets.UTF_8));

		writeLine(fileContent, "spring.application.name=%s", properties.getApplicationName());
		writeLine(fileContent, "server.port=%d", properties.getServerPort());
		writeLine(fileContent, "spring.datasource.driver-class-name=%s", properties.getDriverClassName());
		if (properties.getDataSourceUrl() == null && properties.getDbName() != null) {
			writeLine(fileContent, "spring.datasource.url=jdbc:mysql://%s:%d/%s", properties.getDbIp(),
					properties.getDbPort(), properties.getDbName());
		}
		else if (properties.getDataSourceUrl() != null) {
			writeLine(fileContent, "spring.datasource.url=%s", properties.getDataSourceUrl());
		}
		writeLine(fileContent, "spring.datasource.username=%s", properties.getUsername());
		writeLine(fileContent, "spring.datasource.password=%s", properties.getPassword());

		Files.write(f.toPath(), fileContent, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
	}

	/**
	 * 向配置内容中写入或更新一行配置：已存在同名键则覆盖，否则追加。
	 *
	 * @param fileContent 配置文件内容行列表，方法会就地修改
	 * @param key         配置项模板（可含格式化占位符）
	 * @param value       配置项取值；为空（null）或未提供时跳过
	 */
	@SneakyThrows
	private static void writeLine(List<String> fileContent, String key, Object... value) {
		if (value.length > 0 && value[0] != null) {
			String newLine = String.format(key, value);
			String propertyKey = newLine.split("=", 2)[0];
			for (int i = 0; i < fileContent.size(); i++) {
				if (fileContent.get(i).startsWith(propertyKey)) {
					fileContent.set(i, newLine);
					return;
				}
			}
			fileContent.add(newLine);
		}
	}

	/**
	 * 交互式询问用户是/否，回答包含 Y 视为“是”。
	 *
	 * @param prompt 提示语
	 * @return 用户选择“是”返回 true，否则 false
	 */
	private static boolean yesOrNo(String prompt) {
		colorOutput(String.format("%s Y(是) N(否)", prompt));
		String yesOrNo = sc.nextLine();
		if (yesOrNo.toUpperCase().contains("Y")) {
			return true;
		}
		return false;
	}

	/**
	 * 获取当前 jar 运行目录
	 * @return
	 */
	@SneakyThrows
	public static String getJarFilePath() {
		String path = DatabaseInitHelper.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		String decodedPath = URLDecoder.decode(path, "UTF-8");
		return new File(decodedPath.substring(0, decodedPath.indexOf("!/BOOT-INF")).split(":")[1]).getParent();
	}

	/**
	 * 以默认信息级别输出带颜色的提示文本。
	 *
	 * @param output 待输出的文本
	 */
	private static void colorOutput(String output) {
		colorOutput(output, 0);
	}

	/**
	 * 按级别输出带颜色的提示文本（级别 1 成功、2 失败、其他为普通信息）。
	 *
	 * @param output 待输出的文本
	 * @param level  输出级别
	 */
	private static void colorOutput(String output, int level) {
		switch (level) {
		case 1: // success
			log.info(Ansi.Green.colorize(output));
			break;
		case 2: // fail
			log.error(Ansi.Red.colorize(output));
			break;
		default: // info
			log.info(Ansi.Blue.colorize(output));
			break;
		}
	}

}

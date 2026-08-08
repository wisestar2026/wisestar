package cn.wisestar.server.core.constant;

/**
 * 本地存储文件名策略枚举（LocalStorageNameStrategyEnum）。
 *
 * <p><b>所属模块</b>：shared 模块常量包（cn.wisestar.server.core.constant）。</p>
 * <p><b>类职责</b>：定义本地存储（LocalStorageService）保存文件时的
 * 文件名生成策略，供 {@link cn.wisestar.server.storage.StorageProperties}
 * 中 name-strategy 配置项引用（如 {@code storage.name-strategy: uuid}）。</p>
 *
 * <p><b>取值说明</b>：</p>
 * <ul>
 *   <li>seqAndOriginalName：序列号 + 原文件名（1653122982531_fileName.jpg）；</li>
 *   <li>originalNameAndSeq：原文件名 + 序列号（fileName_1653122982531.jpg）；</li>
 *   <li>seq：仅序列号（1653122982531.jpg）；</li>
 *   <li>uuid：无短杠 UUID（8328839eae07f93443733bc7b0468f04.jpg）。</li>
 * </ul>
 *
 * @author Jiutwo
 */
public enum LocalStorageNameStrategyEnum {

	/**
	 * 序列号加原文件名，例如：1653122982531_fileName.jpg
	 */
	SEQ_ADN_ORIGINAL_NAME("seqAndOriginalName"),
	/**
	 * 原文件名+序列号，例如：fileName_1653122982531.jpg
	 */
	ORIGINAL_NAME_AND_SEQ("originalNameAndSeq"),
	/**
	 * 序列号（项目启动时间戳的自增），例如：1653122982531.jpg
	 */
	SEQ("seq"),
	/**
	 * 去除短杠'-'的UUID，例如：8328839eae07f93443733bc7b0468f04.jpg
	 */
	UUID("uuid");

	/**
	 * 策略标识（配置文件中使用的字符串值）。
	 */
	private final String strategy;

	LocalStorageNameStrategyEnum(String strategy) {
		this.strategy = strategy;
	}

	/**
	 * 获取策略标识。
	 *
	 * @return 策略字符串（如 "uuid"）
	 */
	public String getStrategy() {
		return strategy;
	}

}

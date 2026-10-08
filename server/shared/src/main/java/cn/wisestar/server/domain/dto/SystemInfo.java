package cn.wisestar.server.domain.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author javahuang
 * @date 2022/2/11
 */
@Data
public class SystemInfo {

	/**
	 * 主键
	 */
	private String id;

	/**
	 * 系统名称
	 */
	private String name;

	/**
	 * 系统描述信息
	 */
	private String description;

	/**
	 * 图标
	 */
	private String avatar;

	/**
	 * 默认语言
	 */
	private String locale;

	/**
	 * 默认语言
	 */
	private String version;

	private RegisterInfo registerInfo;

	private SystemSetting setting;

	private String publicKey;

	private Boolean aiEnabled;

	/**
	 * 全局出题策略
	 */
	private PracticeStrategy practiceStrategy;

	@Data
	public static class RegisterInfo {

		/**
		 * 是否开启注册
		 */
		private Boolean registerEnabled;

		/**
		 * 注册用户可选角色列表
		 */
		private List<String> roles = new ArrayList<>();

		/**
		 * 开启强密码验证
		 */
		private Boolean strongPasswordEnabled;

	}

	@Data
	public static class SystemSetting {

		/**
		 * 是否开启验证码
		 */
		private Boolean captchaEnabled;

		/**
		 * 版权
		 */
		private String copyright;

		/**
		 * 备案号
		 */
		private String recordNum;

	}

	@Data
	public static class AiSetting {

		private Boolean enabled;

		private List<String> models;

		private String token;

		private String prompt;

	}

	/**
	 * 全局出题策略（t_sys_info.practice_strategy JSON 的强类型视图）。
	 *
	 * <p>所有小节默认继承本策略；小节可在「练习设置」中按字段覆盖。
	 * 字段为空时使用下方默认值（字段初始化值即默认值，Jackson 反序列化缺失字段时保留）。</p>
	 */
	@Data
	public static class PracticeStrategy {

		/** 例题学习之后的检测题量（默认 4） */
		private Integer previewCount = 4;

		/** 专项训练每个知识点出题上限（默认 8） */
		private Integer drillPerKp = 8;

		/** 小节通关：知识点数 &lt;= trialSmallMaxKp 时的题量（默认 12） */
		private Integer trialSmallCount = 12;

		/** 小节通关：知识点数在 (trialSmallMaxKp, trialMediumMaxKp] 时的题量（默认 15） */
		private Integer trialMediumCount = 15;

		/** 小节通关：知识点数 &gt; trialMediumMaxKp 时的题量（默认 20） */
		private Integer trialLargeCount = 20;

		/** 小节通关「小」档知识点数上限（默认 2） */
		private Integer trialSmallMaxKp = 2;

		/** 小节通关「中」档知识点数上限（默认 5） */
		private Integer trialMediumMaxKp = 5;

		/** 章节测试（exam 小节）基础题量（默认 20） */
		private Integer examCount = 20;

		/** 章节测试题量不足该章知识点数时是否自动扩容到知识点数（默认 true） */
		private Boolean examExpandByKp = true;

		/** 防重复滑窗：出题时排除该学员最近 N 次同范围练习已做过的题（默认 2，0 表示不去重） */
		private Integer repeatWindow = 2;

		/** 缺题检测：一般知识点储备目标（默认 16） */
		private Integer reserveNormal = 16;

		/** 缺题检测：次重点知识点储备目标（默认 20） */
		private Integer reserveMinor = 20;

		/** 缺题检测：重点知识点储备目标（默认 24） */
		private Integer reserveKey = 24;

	}

}

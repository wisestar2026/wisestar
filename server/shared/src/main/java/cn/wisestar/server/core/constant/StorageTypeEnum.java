package cn.wisestar.server.core.constant;

/**
 * 存储文件类型枚举（StorageTypeEnum）。
 *
 * <p><b>所属模块</b>：shared 模块常量包（cn.wisestar.server.core.constant）。</p>
 * <p><b>类职责</b>：定义上传文件（附件/图片）的业务类型，决定文件存储目录
 * 归属、命名规则与访问控制策略。与 {@link cn.wisestar.server.storage.StorageService}
 * 配合，上传接口通过本枚举标识文件用途。</p>
 *
 * <p><b>取值说明</b>：BACKGROUND_IMAGE=1（问卷背景图）、HEADER_IMAGE=2（顶部图）、
 * QUESTION_IMAGE=3（题目图片）、ANSWER_ATTACHMENT=4（答卷附件）、
 * TEMPLATE_PREVIEW_IMAGE=5（模板预览图）。
 * 与 {@link cn.wisestar.server.core.constant.AppConsts.FileType} 数值一致，
 * 存储层以本枚举为准。</p>
 *
 * @author javahuang
 * @date 2022/5/5
 */
public enum StorageTypeEnum {

	/** 背景图片 */
	BACKGROUND_IMAGE(1),

	/** 顶部图片 */
	HEADER_IMAGE(2),

	/** 问题图片 */
	QUESTION_IMAGE(3),

	/** 答卷附件 */
	ANSWER_ATTACHMENT(4),

	/** 问题模板预览图 */
	TEMPLATE_PREVIEW_IMAGE(5);

	/**
	 * 类型数值（数据库/接口中使用的 int 值）。
	 */
	private int type;

	StorageTypeEnum(int type) {
		this.type = type;
	}

	/**
	 * 获取类型数值。
	 *
	 * @return 类型 int 值
	 */
	public int getType() {
		return type;
	}

}

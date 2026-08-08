package cn.wisestar.server.core.constant;

/**
 * 业务错误码枚举（ErrorCode）。
 *
 * <p><b>所属模块</b>：shared 模块常量包（cn.wisestar.server.core.constant）。</p>
 * <p><b>类职责</b>：定义系统全部业务错误码（code）与默认错误消息（message）。
 * 业务代码通过抛出 {@link cn.wisestar.server.core.exception.ErrorCodeException}
 * 携带本枚举，由
 * {@link cn.wisestar.server.core.mvc.advice.GlobalExceptionHandler#handleErrorCodeException}
 * 捕获后把 code/message 透传给前端，前端按 code 做国际化或分支提示。</p>
 *
 * <p><b>编码规则</b>：前两位区分模块（10xx 账号、14xx 注册、40xx 问卷回收/
 * 文件/关联、60xx 考试、70xx 公开查询），后两位区分具体错误消息。</p>
 *
 * @author javahuang
 * @date 2022/2/23
 */
public enum ErrorCode {

	/**
	 * 账号或者密码错误
	 */
	UsernameOrPasswordError(1024, "账号或者密码错误"),
	/**
	 * 注册失败
	 */
	RegisterError(1401, "注册失败"),
	/**
	 * 账号已存在
	 */
	UsernameExists(1025, "账号已存在"),
	/**
	 * 暂停回收
	 */
	SurveySuspend(4000, "问卷已暂停回收"),
	/**
	 * 页面不存在
	 */
	ProjectNotFound(4004, "对不起，你访问的页面不存在"),
	/**
	 * 验证失败
	 */
	ValidationError(4005, "验证失败"),
	/**
	 * 权限校验失败
	 */
	PermVerifyFailed(4006, "没有权限访问本问卷"),
	/**
	 * 停止收集
	 */
	ExceededMaxAnswers(4010, "已达到回收上限，问卷停止收集"),
	/**
	 * 停止收集
	 */
	ExceededEndTime(4011, "超出截止时间，问卷停止收集"),
	/**
	 * 问卷已提交
	 */
	SurveySubmitted(4012, "问卷已提交"),
	/**
	 * 答案不允许修改
	 */
	AnswerChangeDisabled(4020, "答案不允许修改"),
	/**
	 * 附件不存在
	 */
	FileNotExists(4040, "附件不存在"),
	/**
	 * 文件上传失败
	 */
	FileUploadError(4041, "文件上传失败"),
	/**
	 * 文件解析失败
	 */
	FileParseError(4042, "文件解析失败"),

	/**
	 * 未配置关联条件
	 */
	LinkConditionNotFound(4050, "未配置关联条件"),
	/**
	 * 关联问题不存在
	 */
	LinkQuestionNotFound(4051, "关联问题不存在"),
	/**
	 * 问题不存在
	 */
	LinkOptionNotFound(4052, "关联问题不存在"),

	/**
	 * 考试未开始
	 */
	ExamUnStarted(6000, "考试未开始"),
	/**
	 * 考试已结束
	 */
	ExamFinished(6010, "考试已结束"),

	/**
	 * 公开查询链接不存在
	 */
	QueryNotExist(7000, "该查询链接已失效，请联系项目的发布者"),
	/**
	 * 公开查询链接已停止
	 */
	QueryDisabled(7001, "该查询链接已停止，请联系项目的发布者"),
	/**
	 * 公开查询条件不能为空
	 */
	QueryConditionNull(7002, "查询条件不能为空"),
	/**
	 * 查询密码认证失败
	 */
	QueryPasswordError(7003, "查询密码认证失败"),
	/**
	 * 查询条件不存在
	 */
	QueryConditionNotExist(7004, "查询密码认证失败"),
	/**
	 * 公开查询结果不存在
	 */
	QueryResultNotExist(7010, "没有查询到结果，请确认所填信息正确"),
	/**
	 * 公开查询答案更新失败
	 */
	QueryResultUpdateError(7020, "答案更新失败"),;

	/**
	 * 错误码数值（前两位区分模块，后两位区分错误消息）
	 */
	public int code;

	/**
	 * 默认错误消息（可直接展示给用户）
	 */
	public String message;

	/**
	 * 仅含错误码的构造器（message 为空）。
	 *
	 * @param code 错误码
	 */
	ErrorCode(int code) {
		this.code = code;
	}

	/**
	 * 含错误码与消息的构造器。
	 *
	 * @param code    错误码
	 * @param message 默认错误消息
	 */
	ErrorCode(int code, String message) {
		this.code = code;
		this.message = message;
	}

}

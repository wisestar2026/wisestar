package cn.wisestar.server.core.constant;

/**
 * 考试练习类型枚举（ExamExerciseTypeEnum）。
 *
 * <p><b>所属模块</b>：shared 模块常量包（cn.wisestar.server.core.constant）。</p>
 * <p><b>类职责</b>：定义考试模式下"练习"的类型标识，用于区分练习题的出题方式。
 * 与 AI 自习室系统的练习场景（随机抽题练习）配合使用。</p>
 *
 * <p><b>取值说明</b>：R = Random（随机练习）、O = Order（顺序练习）、
 * W = Wrong（错题练习）。</p>
 */
public enum ExamExerciseTypeEnum {

    /**
     * 随机练习
     */
    R,

    /**
     * 顺序练习
     */
    O,
    /**
     * 错题练习
     */
    W
}

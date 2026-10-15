package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.model.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 英语错题消灭记录实体（对应数据库表 t_english_wrong_redo）。
 *
 * <p>英语复习的错题来自检测记录（t_detect_record）中的逐题明细，本身没有可写的
 * 「已订正」标记。学员在复习页把某道错题重做正确后，本表落一条记录，作为
 * 「该错题已消灭」的持久化依据；后续聚合错题时会剔除本表命中的题目。</p>
 *
 * @author wisestar
 * @date 2026/10/14
 */
@Data
@TableName("t_english_wrong_redo")
@EqualsAndHashCode(callSuper = false)
public class EnglishWrongRedo extends BaseModel {

	/** 学员（用户）ID */
	private String studentId;

	/** 已消灭（重做正确）的题目 ID */
	private String questionId;

}

package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.model.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 学科学期学习币实体（对应数据库表 t_subject_semester，分学科、单学期上限 10000，仅作兑换）。
 *
 * @author wisestar
 * @date 2026/9/10
 */
@Data
@TableName("t_subject_semester")
@EqualsAndHashCode(callSuper = false)
public class SubjectSemester extends BaseModel {

	/** 学员ID */
	private String userId;

	/** 学科ID（t_subject.id） */
	private String subjectId;

	/** 学期键，如 2026-1 */
	private String semester;

	/** 本学期该学科学习币（0..10000） */
	private Integer coins;

	/** 是否已达单科上限 */
	private Boolean reachedLimit;

}

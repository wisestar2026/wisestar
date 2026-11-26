/**
 * 英语教材版本/年级的合法取值范围。
 *
 * 平台只维护两套英语教材：小学外研版（三、四、五、六年级）与初中人教版（七、八、九年级）。
 * 后台所有英语相关的版本/年级下拉都从这里取，避免再出现苏教版、北师大版或一、二年级。
 */

/** 可选教材版本（顺序即下拉展示顺序） */
export const ENGLISH_VERSIONS = ['外研版', '人教版']

/** 外研版（小学）年级 */
export const PRIMARY_GRADES = ['三年级', '四年级', '五年级', '六年级']

/** 人教版（初中）年级 */
export const JUNIOR_GRADES = ['七年级', '八年级', '九年级']

/** 全部合法年级（未选版本时的兜底列表） */
export const ENGLISH_GRADES = [...PRIMARY_GRADES, ...JUNIOR_GRADES]

/** 版本下拉选项 */
export const ENGLISH_VERSION_OPTIONS = ENGLISH_VERSIONS.map((v) => ({ value: v, label: v }))

/** 全量年级下拉选项 */
export const ENGLISH_GRADE_OPTIONS = ENGLISH_GRADES.map((g) => ({ value: g, label: g }))

/** 按版本返回合法年级 */
export function gradesOfVersion(version) {
  if (version === '外研版') return PRIMARY_GRADES
  if (version === '人教版') return JUNIOR_GRADES
  return ENGLISH_GRADES
}

/** 按版本返回年级下拉选项 */
export function gradeOptionsOfVersion(version) {
  return gradesOfVersion(version).map((g) => ({ value: g, label: g }))
}

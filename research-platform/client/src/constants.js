export const SEMESTERS = [
  { value: '2026-2027-1', label: '2026-2027 学年第一学期' },
  { value: '2026-2027-2', label: '2026-2027 学年第二学期' },
  { value: '2027-2028-1', label: '2027-2028 学年第一学期' },
]

export const SUBJECTS = ['语文', '数学', '英语', '物理', '化学', '生物']

export const GRADES = ['七年级', '八年级', '九年级', '高一', '高二', '高三']

export const CLASS_TYPES = ['同步班', '提高班', '冲刺班', '一对一']

export const PLAN_STATUS = [
  { value: 'DRAFT', label: '草稿', color: 'default' },
  { value: 'PENDING', label: '待审核', color: 'gold' },
  { value: 'APPROVED', label: '已发布', color: 'green' },
  { value: 'REJECTED', label: '已驳回', color: 'red' },
]

export const statusMeta = (value) => PLAN_STATUS.find((item) => item.value === value) || PLAN_STATUS[0]

/**
 * TaskAssignmentPage.jsx - 任务分配页（教师端）
 *
 * 功能:
 *   1. 选择学员
 *   2. 添加任务（最多 3 个任务给同一学员）
 *   3. 任务类型选择（章节/小节/知识点/习题）
 *   4. 批量分配（一次可添加最多 3 个任务）
 *
 * URL: /student/task-assignment
 */

import { useState } from 'react';
import { Form, Input, Select, Button, message, Card, Space, InputNumber } from 'antd';

const API_BASE = '/api/student/task';

export default function TaskAssignmentPage() {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [taskCount, setTaskCount] = useState(1);

  // 任务类型选项
  const taskTypes = [
    { value: 'chapter', label: '章节学习' },
    { value: 'section', label: '小节学习' },
    { value: 'knowledge', label: '知识点学习' },
    { value: 'exercise', label: '习题练习' },
  ];

  // 提交任务
  const handleSubmit = () => {
    form.validateFields().then((values) => {
      // 构建任务数组
      const taskContents = [];
      const taskTypes = [];
      const taskTargets = [];

      for (let i = 0; i < taskCount; i++) {
        if (values[`taskContent_${i}`]) {
          taskContents.push(values[`taskContent_${i}`]);
          taskTypes.push(values[`taskType_${i}`]);
          taskTargets.push(values[`taskTarget_${i}`]);
        }
      }

      if (taskContents.length === 0) {
        message.warning('请至少填写一个任务');
        return;
      }

      setLoading(true);
      fetch(`${API_BASE}/assign`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          studentId: values.studentId,
          taskContents,
          taskTypes,
          taskTargets,
        }),
      })
        .then((res) => res.json())
        .then((res) => {
          if (res) {
            message.success('任务分配成功');
            form.resetFields();
            setTaskCount(1);
          } else {
            message.error('任务分配失败，该学员今日任务已达上限（最多 3 个）');
          }
        })
        .catch(() => message.error('任务分配失败'))
        .finally(() => setLoading(false));
    });
  };

  // 增加任务
  const addTask = () => {
    if (taskCount < 3) {
      setTaskCount(taskCount + 1);
    } else {
      message.warning('最多只能给同一学员分配 3 个任务');
    }
  };

  // 删除任务
  const removeTask = (index) => {
    if (taskCount > 1) {
      setTaskCount(taskCount - 1);
      form.setFieldsValue({
        [`taskContent_${index}`]: undefined,
        [`taskType_${index}`]: undefined,
        [`taskTarget_${index}`]: undefined,
      });
    }
  };

  return (
    <div style={{ maxWidth: 800, margin: '0 auto', padding: 20 }}>
      <Card title="任务分配">
        <Form form={form} layout="vertical">
          <Form.Item
            name="studentId"
            label="选择学员"
            rules={[{ required: true, message: '请选择学员' }]}
          >
            <Select placeholder="选择学员">
              {/* TODO: 从 API 加载学员列表 */}
              <Select.Option value="student1">学员 1</Select.Option>
              <Select.Option value="student2">学员 2</Select.Option>
            </Select>
          </Form.Item>

          {/* 任务列表 */}
          {[...Array(taskCount)].map((_, index) => (
            <Card
              key={index}
              size="small"
              title={`任务 ${index + 1}`}
              style={{ marginBottom: 16 }}
              extra={
                taskCount > 1 && (
                  <Button
                    type="link"
                    danger
                    size="small"
                    onClick={() => removeTask(index)}
                  >
                    删除此任务
                  </Button>
                )
              }
            >
              <Form.Item
                name={`taskContent_${index}`}
                label="任务内容"
                rules={[{ required: true, message: '请填写任务内容' }]}
              >
                <Input.TextArea
                  rows={2}
                  placeholder="请输入任务内容（文本形式，如：完成第 1 章节的学习）"
                />
              </Form.Item>

              <Form.Item
                name={`taskType_${index}`}
                label="任务类型"
              >
                <Select placeholder="选择任务类型">
                  {taskTypes.map((type) => (
                    <Select.Option key={type.value} value={type.value}>
                      {type.label}
                    </Select.Option>
                  ))}
                </Select>
              </Form.Item>

              <Form.Item
                name={`taskTarget_${index}`}
                label="任务目标 ID"
              >
                <Input placeholder="请输入章节/小节/知识点/习题 ID（可选）" />
              </Form.Item>
            </Card>
          ))}

          {taskCount < 3 && (
            <Button type="dashed" block onClick={addTask} style={{ marginBottom: 16 }}>
              + 添加另一个任务（最多 3 个）
            </Button>
          )}

          <Form.Item>
            <Button type="primary" htmlType="submit" loading={loading} block>
              分配任务
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
}

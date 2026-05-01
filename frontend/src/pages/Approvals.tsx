import React, { useEffect, useState } from 'react';
import { Table, Button, Tag, Space, Typography, message, Modal, Input, Spin } from 'antd';
import { CheckOutlined, CloseOutlined } from '@ant-design/icons';
import { getApprovals, approvePage, rejectPage, ApprovalItem } from '../api';

const { Title } = Typography;
const { TextArea } = Input;

export default function Approvals() {
  const [approvals, setApprovals] = useState<ApprovalItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [currentAction, setCurrentAction] = useState<{ id: string; type: 'approve' | 'reject' } | null>(null);
  const [comment, setComment] = useState('');

  const loadApprovals = async () => {
    setLoading(true);
    try {
      const res = await getApprovals();
      setApprovals(res.data);
    } catch (e) {
      message.error('加载审批列表失败');
    }
    setLoading(false);
  };

  useEffect(() => { loadApprovals(); }, []);

  const handleAction = async () => {
    if (!currentAction) return;
    try {
      if (currentAction.type === 'approve') {
        await approvePage(currentAction.id, comment);
        message.success('已批准');
      } else {
        await rejectPage(currentAction.id, comment);
        message.success('已拒绝');
      }
      setModalVisible(false);
      setComment('');
      loadApprovals();
    } catch (e) {
      message.error('操作失败');
    }
  };

  const openModal = (id: string, type: 'approve' | 'reject') => {
    setCurrentAction({ id, type });
    setModalVisible(true);
  };

  return (
    <div>
      <Title level={4}>审批队列</Title>
      <Table
        dataSource={approvals}
        rowKey="id"
        loading={loading}
        columns={[
          { title: '页面ID', dataIndex: 'pageId', key: 'pageId', render: (id: string) => id?.substring(0, 8) + '...' },
          { title: '操作', dataIndex: 'action', key: 'action', width: 80, render: (a: string) => (
            <Tag color={a === 'CREATE' ? 'blue' : a === 'DELETE' ? 'red' : 'orange'}>{a}</Tag>
          )},
          { title: '状态', dataIndex: 'status', key: 'status', width: 100, render: (s: string) => (
            <Tag color={s === 'PENDING' ? 'orange' : s === 'APPROVED' ? 'green' : 'red'}>{s}</Tag>
          )},
          { title: '备注', dataIndex: 'comment', key: 'comment', ellipsis: true },
          { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 180, render: (d: string) => new Date(d).toLocaleString() },
          {
            title: '操作', key: 'ops', width: 160,
            render: (_: any, record: ApprovalItem) => record.status === 'PENDING' ? (
              <Space>
                <Button type="primary" size="small" icon={<CheckOutlined />} onClick={() => openModal(record.id, 'approve')}>批准</Button>
                <Button danger size="small" icon={<CloseOutlined />} onClick={() => openModal(record.id, 'reject')}>拒绝</Button>
              </Space>
            ) : <span style={{ color: '#999' }}>已处理</span>
          },
        ]}
      />

      <Modal
        title={currentAction?.type === 'approve' ? '批准页面' : '拒绝页面'}
        open={modalVisible}
        onOk={handleAction}
        onCancel={() => setModalVisible(false)}
        okText={currentAction?.type === 'approve' ? '批准' : '拒绝'}
        okButtonProps={{ danger: currentAction?.type === 'reject' }}
      >
        <TextArea
          placeholder="备注（可选）"
          value={comment}
          onChange={e => setComment(e.target.value)}
          rows={3}
        />
      </Modal>
    </div>
  );
}

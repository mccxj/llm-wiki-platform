import React, { useEffect, useState } from 'react';
import { Table, Tag, Button, Space, Typography, message, Spin } from 'antd';
import { useNavigate } from 'react-router-dom';
import { getPages, Page } from '../api';

const { Title } = Typography;

const statusColors: Record<string, string> = {
  PENDING_APPROVAL: 'orange',
  APPROVED: 'green',
  REJECTED: 'red',
  ARCHIVED: 'default',
};

const statusLabels: Record<string, string> = {
  PENDING_APPROVAL: '待审批',
  APPROVED: '已发布',
  REJECTED: '已拒绝',
  ARCHIVED: '已归档',
};

export default function Pages() {
  const [pages, setPages] = useState<Page[]>([]);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const loadPages = async () => {
    setLoading(true);
    try {
      const res = await getPages();
      setPages(res.data);
    } catch (e) {
      message.error('加载页面列表失败');
    }
    setLoading(false);
  };

  useEffect(() => { loadPages(); }, []);

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <Title level={4}>页面管理</Title>
        <Button onClick={loadPages} loading={loading}>刷新</Button>
      </div>

      <Table
        dataSource={pages}
        rowKey="id"
        loading={loading}
        columns={[
          { title: '标题', dataIndex: 'title', key: 'title', render: (t: string, r: Page) => (
            <a onClick={() => navigate(`/pages/${r.id}`)}>{t}</a>
          )},
          { title: '类型', dataIndex: 'pageType', key: 'pageType', width: 100 },
          { title: '状态', dataIndex: 'status', key: 'status', width: 120, render: (s: string) => (
            <Tag color={statusColors[s]}>{statusLabels[s] || s}</Tag>
          )},
          { title: '评分', dataIndex: 'aiScore', key: 'score', width: 80, render: (s: number) => s?.toFixed(1) || '-' },
          { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 180, render: (d: string) => new Date(d).toLocaleString() },
        ]}
        pagination={{ pageSize: 20 }}
      />
    </div>
  );
}

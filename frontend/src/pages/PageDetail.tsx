import React, { useEffect, useState } from 'react';
import { Card, Typography, Tag, Button, Space, Descriptions, message } from 'antd';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeftOutlined } from '@ant-design/icons';
import { getPage } from '../api';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';

const { Title } = Typography;

export default function PageDetail() {
  const { id } = useParams<{ id: string }>();
  const [page, setPage] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    if (!id) return;
    getPage(id).then(res => {
      setPage(res.data);
      setLoading(false);
    }).catch(() => {
      message.error('页面不存在');
      navigate('/pages');
    });
  }, [id]);

  if (loading || !page) return <div>加载中...</div>;

  return (
    <div>
      <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/pages')} style={{ marginBottom: 16 }}>
        返回列表
      </Button>

      <Card loading={loading}>
        <Title level={3}>{page.title}</Title>
        <Space style={{ marginBottom: 16 }}>
          <Tag color={page.status === 'APPROVED' ? 'green' : page.status === 'REJECTED' ? 'red' : 'orange'}>
            {page.status}
          </Tag>
          <Tag>{page.pageType}</Tag>
          {page.aiScore && <Tag color="blue">评分: {page.aiScore.toFixed(1)}</Tag>}
        </Space>

        <Descriptions size="small" column={2}>
          <Descriptions.Item label="Slug">{page.slug}</Descriptions.Item>
          <Descriptions.Item label="版本">v{page.version}</Descriptions.Item>
          <Descriptions.Item label="创建时间">{new Date(page.createdAt).toLocaleString()}</Descriptions.Item>
          <Descriptions.Item label="发布时间">{page.publishedAt ? new Date(page.publishedAt).toLocaleString() : '-'}</Descriptions.Item>
        </Descriptions>

        <div style={{ marginTop: 24, borderTop: '1px solid #f0f0f0', paddingTop: 16 }}>
          <ReactMarkdown remarkPlugins={[remarkGfm]}>{page.content}</ReactMarkdown>
        </div>
      </Card>
    </div>
  );
}

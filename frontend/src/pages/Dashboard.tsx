import React, { useEffect, useState } from 'react';
import { Card, Row, Col, Statistic, List, Tag, Typography, Button, Space } from 'antd';
import { SyncOutlined, FileTextOutlined, CheckCircleOutlined, ClockCircleOutlined } from '@ant-design/icons';
import { getPages, getApprovals, getSyncLogs, triggerSyncAll } from '../api';

const { Title } = Typography;

export default function Dashboard() {
  const [pageStats, setPageStats] = useState({ total: 0, pending: 0, approved: 0 });
  const [approvalCount, setApprovalCount] = useState(0);
  const [recentLogs, setRecentLogs] = useState<any[]>([]);
  const [syncing, setSyncing] = useState(false);

  const loadData = async () => {
    try {
      const [pagesRes, approvalsRes, logsRes] = await Promise.all([
        getPages(),
        getApprovals(),
        getSyncLogs()
      ]);
      const pages = pagesRes.data;
      setPageStats({
        total: pages.length,
        pending: pages.filter((p: any) => p.status === 'PENDING_APPROVAL').length,
        approved: pages.filter((p: any) => p.status === 'APPROVED').length,
      });
      setApprovalCount(approvalsRes.data.length);
      setRecentLogs(logsRes.data.slice(0, 5));
    } catch (e) {
      console.error('Failed to load dashboard data', e);
    }
  };

  useEffect(() => { loadData(); }, []);

  const handleSync = async () => {
    setSyncing(true);
    try {
      await triggerSyncAll();
      setTimeout(loadData, 2000);
    } catch (e) {
      console.error('Sync failed', e);
    }
    setSyncing(false);
  };

  return (
    <div>
      <Row gutter={16}>
        <Col span={6}>
          <Card>
            <Statistic title="总页面数" value={pageStats.total} prefix={<FileTextOutlined />} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="待审批" value={pageStats.pending} prefix={<ClockCircleOutlined />} valueStyle={{ color: '#faad14' }} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="已发布" value={pageStats.approved} prefix={<CheckCircleOutlined />} valueStyle={{ color: '#52c41a' }} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="审批队列" value={approvalCount} />
          </Card>
        </Col>
      </Row>

      <Card title="同步操作" style={{ marginTop: 16 }}>
        <Space>
          <Button type="primary" icon={<SyncOutlined />} loading={syncing} onClick={handleSync}>
            手动同步
          </Button>
          <Button onClick={loadData}>刷新数据</Button>
        </Space>
      </Card>

      <Card title="最近同步记录" style={{ marginTop: 16 }}>
        <List
          size="small"
          dataSource={recentLogs}
          renderItem={(item: any) => (
            <List.Item>
              <Space>
                <Tag color={
                  item.status === 'SUCCESS' ? 'green' :
                  item.status === 'FAILED' ? 'red' : 'blue'
                }>{item.status}</Tag>
                <span>获取 {item.fetchedCount} | 处理 {item.processedCount} | 跳过 {item.skippedCount} | 失败 {item.failedCount}</span>
                <span style={{ color: '#999' }}>{new Date(item.startedAt).toLocaleString()}</span>
              </Space>
            </List.Item>
          )}
        />
      </Card>
    </div>
  );
}

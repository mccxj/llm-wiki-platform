import React, { useEffect, useState } from 'react';
import { Typography, Card, Table, Button, Space, Tag, message, Modal, Input, Descriptions, Progress, Divider, List } from 'antd';
import { SettingOutlined, ToolOutlined, ReloadOutlined, WarningOutlined, CopyOutlined, ExclamationCircleOutlined } from '@ant-design/icons';
import { getConfig, updateConfig, getMaintenanceReport, MaintenanceReport } from '../api';

const { Title, Text } = Typography;
const { TextArea } = Input;

interface ConfigItem {
  key: string;
  value: string;
  description?: string;
  updatedAt?: string;
}

interface MaintenanceReportItem {
  id?: string;
  title?: string;
}

interface MaintenanceReportExtended extends MaintenanceReport {
  orphans?: MaintenanceReportItem[];
  stalePages?: MaintenanceReportItem[];
  duplicates?: MaintenanceReportItem[][];
  contradictions?: MaintenanceReportItem[];
}

export default function Admin() {
  const [configs, setConfigs] = useState<ConfigItem[]>([]);
  const [report, setReport] = useState<MaintenanceReportExtended | null>(null);
  const [loading, setLoading] = useState(false);
  const [reportLoading, setReportLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingKey, setEditingKey] = useState('');
  const [editingValue, setEditingValue] = useState('');
  const [editingDesc, setEditingDesc] = useState('');

  const loadConfigs = async () => {
    setLoading(true);
    try {
      const res = await getConfig();
      setConfigs(res.data);
    } catch (e) {
      message.error('加载配置失败');
    }
    setLoading(false);
  };

  const loadReport = async () => {
    setReportLoading(true);
    try {
      const res = await getMaintenanceReport();
      setReport(res.data);
    } catch (e) {
      message.error('加载维护报告失败');
    }
    setReportLoading(false);
  };

  useEffect(() => {
    loadConfigs();
    loadReport();
  }, []);

  const openEdit = (item: ConfigItem) => {
    setEditingKey(item.key);
    setEditingValue(item.value);
    setEditingDesc(item.description || '');
    setModalVisible(true);
  };

  const handleSaveConfig = async () => {
    try {
      await updateConfig(editingKey, { value: editingValue, description: editingDesc });
      message.success('配置已更新');
      setModalVisible(false);
      loadConfigs();
    } catch (e) {
      message.error('保存失败');
    }
  };

  const healthScore = report
    ? Math.max(0, 100 - report.orphanCount * 5 - report.staleCount * 2 - report.duplicateGroups * 10 - report.contradictionCount * 8)
    : 0;

  return (
    <div>
      <Title level={4}>管理后台</Title>

      {/* Health Score */}
      {report && (
        <Card style={{ marginBottom: 24 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 24 }}>
            <div style={{ flex: 1 }}>
              <Text strong>知识库健康度</Text>
              <Progress
                percent={healthScore}
                status={healthScore >= 80 ? 'success' : healthScore >= 50 ? 'normal' : 'exception'}
                strokeColor={healthScore >= 80 ? '#52c41a' : healthScore >= 50 ? '#1677ff' : '#ff4d4f'}
              />
            </div>
            <Text type="secondary" style={{ fontSize: 12 }}>
              生成时间: {new Date(report.generatedAt).toLocaleString()}
            </Text>
            <Button icon={<ReloadOutlined />} onClick={loadReport} loading={reportLoading}>刷新</Button>
          </div>
        </Card>
      )}

      {/* Maintenance Summary */}
      {report && (
        <Card title={<><ToolOutlined /> 维护摘要</>} style={{ marginBottom: 24 }}>
          <Descriptions column={4} size="small">
            <Descriptions.Item label="总页面数">{report.totalPages}</Descriptions.Item>
            <Descriptions.Item label="孤儿页面">
              <Tag color={report.orphanCount > 0 ? 'orange' : 'green'}>{report.orphanCount}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="过时页面(30天)">
              <Tag color={report.staleCount > 0 ? 'orange' : 'green'}>{report.staleCount}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="重复组">
              <Tag color={report.duplicateGroups > 0 ? 'red' : 'green'}>{report.duplicateGroups}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="矛盾页面">
              <Tag color={report.contradictionCount > 0 ? 'red' : 'green'}>{report.contradictionCount}</Tag>
            </Descriptions.Item>
          </Descriptions>

          <Divider />

          {/* Orphans list */}
          {report.orphans && report.orphans.length > 0 && (
            <div style={{ marginBottom: 16 }}>
              <Text strong><WarningOutlined style={{ color: '#faad14' }} /> 孤儿页面</Text>
              <List
                size="small"
                dataSource={report.orphans}
                renderItem={(item: MaintenanceReportItem) => (
                  <List.Item><Text>{item.title}</Text> <Text type="secondary">({item.id?.substring(0, 8)}...)</Text></List.Item>
                )}
                style={{ marginTop: 8 }}
              />
            </div>
          )}

          {/* Contradictions list */}
          {report.contradictions && report.contradictions.length > 0 && (
            <div>
              <Text strong><ExclamationCircleOutlined style={{ color: '#ff4d4f' }} /> 矛盾页面</Text>
              <List
                size="small"
                dataSource={report.contradictions}
                renderItem={(item: MaintenanceReportItem) => (
                  <List.Item><Text>{item.title}</Text> <Text type="secondary">({item.id?.substring(0, 8)}...)</Text></List.Item>
                )}
                style={{ marginTop: 8 }}
              />
            </div>
          )}
        </Card>
      )}

      {/* System Config */}
      <Card title={<><SettingOutlined /> 系统配置</>}>
        <Table
          dataSource={configs}
          rowKey="key"
          loading={loading}
          size="small"
          columns={[
            { title: '配置键', dataIndex: 'key', key: 'key', width: 200 },
            { title: '配置值', dataIndex: 'value', key: 'value', ellipsis: true },
            { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
            { title: '更新时间', dataIndex: 'updatedAt', key: 'updatedAt', width: 180, render: (d: string) => d ? new Date(d).toLocaleString() : '-' },
            {
              title: '操作', key: 'ops', width: 80,
              render: (_: any, record: ConfigItem) => (
                <Button type="link" size="small" onClick={() => openEdit(record)}>编辑</Button>
              ),
            },
          ]}
        />
      </Card>

      <Modal
        title={`编辑配置: ${editingKey}`}
        open={modalVisible}
        onOk={handleSaveConfig}
        onCancel={() => setModalVisible(false)}
        okText="保存"
      >
        <div style={{ marginBottom: 16 }}>
          <Text strong>配置值</Text>
          <TextArea
            value={editingValue}
            onChange={e => setEditingValue(e.target.value)}
            rows={4}
            style={{ marginTop: 8 }}
          />
        </div>
        <div>
          <Text strong>描述</Text>
          <Input
            value={editingDesc}
            onChange={e => setEditingDesc(e.target.value)}
            style={{ marginTop: 8 }}
          />
        </div>
      </Modal>
    </div>
  );
}

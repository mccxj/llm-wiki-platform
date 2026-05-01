import React from 'react';
import { Layout as AntLayout, Menu, Typography } from 'antd';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import {
  DashboardOutlined, FileTextOutlined, CheckCircleOutlined,
  SearchOutlined, ShareAltOutlined
} from '@ant-design/icons';

const { Header, Sider, Content } = AntLayout;
const { Title } = Typography;

const menuItems = [
  { key: '/dashboard', icon: <DashboardOutlined />, label: '仪表盘' },
  { key: '/pages', icon: <FileTextOutlined />, label: '页面管理' },
  { key: '/approvals', icon: <CheckCircleOutlined />, label: '审批队列' },
  { key: '/search', icon: <SearchOutlined />, label: '搜索问答' },
  { key: '/graph', icon: <ShareAltOutlined />, label: '知识图谱' },
];

export default function Layout() {
  const navigate = useNavigate();
  const location = useLocation();

  return (
    <AntLayout style={{ minHeight: '100vh' }}>
      <Sider width={200} theme="light">
        <div style={{ padding: 16, textAlign: 'center' }}>
          <Title level={4} style={{ margin: 0 }}>📚 LLM Wiki</Title>
        </div>
        <Menu
          mode="inline"
          selectedKeys={[location.pathname]}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
        />
      </Sider>
      <AntLayout>
        <Header style={{ background: '#fff', padding: '0 24px', boxShadow: '0 1px 4px rgba(0,0,0,0.08)' }}>
          <Title level={4} style={{ margin: '16px 0' }}>
            {menuItems.find(m => m.key === location.pathname)?.label || 'LLM Wiki 自动化平台'}
          </Title>
        </Header>
        <Content style={{ margin: 24, padding: 24, background: '#fff', borderRadius: 8, minHeight: 400 }}>
          <Outlet />
        </Content>
      </AntLayout>
    </AntLayout>
  );
}

import React, { useState, useEffect } from 'react';
import { Layout as AntLayout, Menu, Typography, Button, Modal, Form, Input, message, Dropdown, Space } from 'antd';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import {
  DashboardOutlined, FileTextOutlined, CheckCircleOutlined,
  SearchOutlined, ShareAltOutlined, LoginOutlined, LogoutOutlined, UserOutlined
} from '@ant-design/icons';
import { login as loginApi, register as registerApi, verifyToken, LoginRequest } from '../api';

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
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [username, setUsername] = useState('');
  const [modalVisible, setModalVisible] = useState(false);
  const [isRegister, setIsRegister] = useState(false);
  const [form] = Form.useForm();

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (token) {
      verifyToken()
        .then((res: any) => {
          if (res.data.valid) {
            setIsLoggedIn(true);
            setUsername(res.data.userId);
          } else {
            localStorage.removeItem('token');
          }
        })
        .catch(() => {
          localStorage.removeItem('token');
        });
    }
  }, []);

  const handleAuth = async (values: LoginRequest) => {
    try {
      const apiCall = isRegister ? registerApi : loginApi;
      const res: any = await apiCall(values);
      const { token, username: user, role } = res.data;
      localStorage.setItem('token', token);
      setIsLoggedIn(true);
      setUsername(user);
      setModalVisible(false);
      form.resetFields();
      message.success(isRegister ? '注册成功' : '登录成功');
    } catch (err: any) {
      message.error(err.response?.data?.error || (isRegister ? '注册失败' : '登录失败'));
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('token');
    setIsLoggedIn(false);
    setUsername('');
    message.success('已退出登录');
    navigate('/dashboard');
  };

  const userMenuItems = [
    { key: 'logout', icon: <LogoutOutlined />, label: '退出登录', onClick: handleLogout },
  ];

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
        <Header style={{ background: '#fff', padding: '0 24px', boxShadow: '0 1px 4px rgba(0,0,0,0.08)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Title level={4} style={{ margin: '16px 0' }}>
            {menuItems.find(m => m.key === location.pathname)?.label || 'LLM Wiki 自动化平台'}
          </Title>
          <div>
            {isLoggedIn ? (
              <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
                <Button type="text" icon={<UserOutlined />}>
                  <Space>{username}</Space>
                </Button>
              </Dropdown>
            ) : (
              <Button type="primary" icon={<LoginOutlined />} onClick={() => { setIsRegister(false); setModalVisible(true); }}>
                登录
              </Button>
            )}
          </div>
        </Header>
        <Content style={{ margin: 24, padding: 24, background: '#fff', borderRadius: 8, minHeight: 400 }}>
          <Outlet />
        </Content>
      </AntLayout>

      <Modal
        title={isRegister ? '用户注册' : '用户登录'}
        open={modalVisible}
        onCancel={() => { setModalVisible(false); form.resetFields(); }}
        footer={null}
      >
        <Form form={form} onFinish={handleAuth} layout="vertical">
          <Form.Item name="username" label="用户名" rules={[{ required: true, message: '请输入用户名' }]}>
            <Input placeholder="请输入用户名" />
          </Form.Item>
          <Form.Item name="password" label="密码" rules={[{ required: true, message: '请输入密码' }]}>
            <Input.Password placeholder="请输入密码" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" block>
              {isRegister ? '注册' : '登录'}
            </Button>
          </Form.Item>
          <div style={{ textAlign: 'center' }}>
            <Button type="link" onClick={() => setIsRegister(!isRegister)}>
              {isRegister ? '已有账号？去登录' : '没有账号？去注册'}
            </Button>
          </div>
        </Form>
      </Modal>
    </AntLayout>
  );
}

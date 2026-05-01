import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import Layout from './components/Layout';
import Dashboard from './pages/Dashboard';
import Pages from './pages/Pages';
import PageDetail from './pages/PageDetail';
import Approvals from './pages/Approvals';
import Search from './pages/Search';
import Graph from './pages/Graph';
import Qa from './pages/Qa';
import Admin from './pages/Admin';

export default function App() {
  return (
    <ConfigProvider locale={zhCN}>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<Layout />}>
            <Route index element={<Navigate to="/dashboard" />} />
            <Route path="dashboard" element={<Dashboard />} />
            <Route path="pages" element={<Pages />} />
            <Route path="pages/:id" element={<PageDetail />} />
            <Route path="approvals" element={<Approvals />} />
            <Route path="search" element={<Search />} />
            <Route path="graph" element={<Graph />} />
            <Route path="qa" element={<Qa />} />
            <Route path="admin" element={<Admin />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </ConfigProvider>
  );
}

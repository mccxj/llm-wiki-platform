import axios from 'axios';

const api = axios.create({
  baseURL: '/api',
  timeout: 30000
});

// ==================== Pipeline ====================
export interface SyncLog {
  id: string;
  sourceId: string;
  startedAt: string;
  finishedAt?: string;
  status: string;
  fetchedCount: number;
  processedCount: number;
  skippedCount: number;
  failedCount: number;
  errorMessage?: string;
}

export interface Page {
  id: string;
  title: string;
  slug: string;
  content: string;
  pageType: string;
  status: string;
  aiScore: number;
  createdAt: string;
  publishedAt?: string;
}

export interface ProcessingLog {
  id: string;
  step: string;
  status: string;
  score: number;
  detail: string;
  startedAt: string;
}

export const triggerSync = (sourceId: string) => api.post(`/sync/trigger/${sourceId}`);
export const triggerSyncAll = () => api.post('/sync/trigger-all');
export const getSyncLogs = () => api.get<SyncLog[]>('/sync/logs');
export const getSyncSources = () => api.get('/sync/sources');
export const getPages = (status = 'ALL') => api.get<Page[]>('/pages', { params: { status } });
export const getPage = (id: string) => api.get<Page>(`/pages/${id}`);
export const deletePage = (id: string) => api.delete(`/pages/${id}`);
export const getPageLinks = (id: string) => api.get(`/pages/${id}/links`);
export const getPageHistory = (id: string) => api.get<ProcessingLog[]>(`/pages/${id}/history`);
export const processDocument = (rawDocId: string) => api.post(`/pipeline/process/${rawDocId}`);
export const getProcessingLogs = (rawDocId: string) => api.get<ProcessingLog[]>(`/pipeline/logs/${rawDocId}`);
export const getPendingDocs = () => api.get('/pipeline/pending');

// ==================== Approvals ====================
export interface ApprovalItem {
  id: string;
  pageId: string;
  action: string;
  status: string;
  comment?: string;
  reviewerId?: string;
  reviewedAt?: string;
  createdAt: string;
}

export const getApprovals = (status = 'PENDING') => api.get<ApprovalItem[]>('/approvals', { params: { status } });
export const approvePage = (approvalId: string, comment = '') =>
  api.post(`/approvals/approve/${approvalId}`, { reviewerId: 'admin', comment });
export const rejectPage = (approvalId: string, comment = '') =>
  api.post(`/approvals/reject/${approvalId}`, { reviewerId: 'admin', comment });

// ==================== Search & Q&A ====================
export interface SearchResult {
  nodeId: string;
  nodeName: string;
  nodeType: string;
  description?: string;
  similarity: number;
  pageTitle?: string;
  pageSlug?: string;
  pageContent?: string;
}

export interface SearchRequest {
  query: string;
  types?: string[];
  tags?: string[];
  limit?: number;
  offset?: number;
}

export interface AnswerResult {
  answer: string;
  source: string;
  citations: string[];
}

export const search = (request: SearchRequest) => api.post<SearchResult[]>('/search', request);
export const askQuestion = (question: string) => api.post<AnswerResult>('/qa/ask', { question });
export const searchByTag = (tag: string, limit = 20) =>
  api.get<SearchResult[]>('/search/by-tag', { params: { tag, limit } });
export const searchByRelation = (nodeId: string, relationType?: string, limit = 20) =>
  api.get<SearchResult[]>('/search/by-relation', { params: { nodeId, relationType, limit } });

// ==================== Knowledge Graph ====================
export interface GraphNode {
  id: string;
  name: string;
  type: string;
  description?: string;
}

export interface GraphEdge {
  source: string;
  target: string;
  type: string;
  weight: number;
}

export interface GraphData {
  nodes: GraphNode[];
  edges: GraphEdge[];
}

export const getGraphData = () => api.get<GraphData>('/graph');
export const getNeighborhood = (nodeId: string) => api.get<GraphData>(`/graph/neighborhood/${nodeId}`);
export const getOrphans = () => api.get<GraphNode[]>('/graph/orphans');

// ==================== Admin ====================
export interface SystemConfig {
  key: string;
  value: string;
  description?: string;
  updatedAt?: string;
}

export interface MaintenanceReport {
  generatedAt: string;
  totalPages: number;
  orphanCount: number;
  staleCount: number;
  duplicateGroups: number;
  contradictionCount: number;
}

export const getConfig = () => api.get<SystemConfig[]>('/admin/config');
export const updateConfig = (key: string, body: { value: string; description?: string }) =>
  api.put<SystemConfig>(`/admin/config/${key}`, body);
export const triggerOrphanCheck = () => api.post('/admin/maintenance/orphans');
export const triggerStaleCheck = (days = 30) => api.post('/admin/maintenance/stale', null, { params: { days } });
export const triggerDuplicateCheck = () => api.post('/admin/maintenance/duplicates');
export const triggerContradictionCheck = () => api.post('/admin/maintenance/contradictions');
export const getMaintenanceReport = () => api.get<MaintenanceReport>('/admin/reports/maintenance');

// ==================== Auth ====================
export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  username: string;
  role: string;
}

export const login = (data: LoginRequest) => api.post<LoginResponse>('/auth/login', data);
export const register = (data: LoginRequest) => api.post<LoginResponse>('/auth/register', data);
export const verifyToken = () => api.get('/auth/verify');

// 请求拦截器：自动添加Token
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 响应拦截器：处理401
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      window.location.href = '/';
    }
    return Promise.reject(error);
  }
);

export default api;

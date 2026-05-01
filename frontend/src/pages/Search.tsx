import React, { useState } from 'react';
import { Input, List, Typography, Tag, Card, Divider, Spin, Empty, message, Alert } from 'antd';
import { SearchOutlined, RobotOutlined, BookOutlined, ExclamationCircleOutlined } from '@ant-design/icons';
import { search, askQuestion, SearchResult, AnswerResult } from '../api';

const { Title, Text, Paragraph } = Typography;

export default function Search() {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<SearchResult[]>([]);
  const [answer, setAnswer] = useState<AnswerResult | null>(null);
  const [loading, setLoading] = useState(false);
  const [asking, setAsking] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSearch = async () => {
    if (!query.trim()) {
      message.warning('请输入搜索关键词');
      return;
    }
    setLoading(true);
    setAnswer(null);
    setError(null);
    try {
      const res = await search(query);
      setResults(res.data);
    } catch (e: any) {
      setError(e?.response?.data?.message || '搜索失败，请稍后重试');
      message.error('搜索失败');
    }
    setLoading(false);
  };

  const handleAsk = async () => {
    if (!query.trim()) {
      message.warning('请输入问题');
      return;
    }
    setAsking(true);
    setResults([]);
    setError(null);
    try {
      const res = await askQuestion(query);
      setAnswer(res.data);
    } catch (e: any) {
      setError(e?.response?.data?.message || '问答服务暂时不可用');
      message.error('问答失败');
    }
    setAsking(false);
  };

  return (
    <div>
      <Title level={4}>搜索 & 智能问答</Title>

      <div style={{ display: 'flex', gap: 8, marginBottom: 24 }}>
        <Input.Search
          placeholder="输入搜索问题..."
          value={query}
          onChange={e => setQuery(e.target.value)}
          onSearch={handleSearch}
          enterButton={<><SearchOutlined /> 语义搜索</>}
          size="large"
          style={{ flex: 1 }}
          loading={loading}
        />
        <button
          onClick={handleAsk}
          disabled={asking || !query.trim()}
          style={{
            padding: '0 24px', fontSize: 16, borderRadius: 6, border: 'none',
            background: '#1677ff', color: '#fff', cursor: 'pointer'
          }}
        >
          <RobotOutlined /> 智能问答
        </button>
      </div>

      {error && (
        <Alert
          type="error"
          showIcon
          icon={<ExclamationCircleOutlined />}
          message="请求失败"
          description={error}
          style={{ marginBottom: 16 }}
        />
      )}

      {/* Q&A Result */}
      {answer && (
        <Card style={{ marginBottom: 24 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 12 }}>
            <RobotOutlined style={{ color: '#1677ff', fontSize: 18 }} />
            <Title level={5} style={{ margin: 0 }}>AI 回答</Title>
            <Tag color={answer.source === 'KNOWLEDGE_BASE' ? 'green' : 'orange'}>
              {answer.source === 'KNOWLEDGE_BASE' ? '来自知识库' : 'AI 生成（回退）'}
            </Tag>
          </div>

          {answer.source !== 'KNOWLEDGE_BASE' && (
            <Alert
              type="warning"
              showIcon
              message="此答案由AI生成，可能不完全准确"
              style={{ marginBottom: 16 }}
            />
          )}

          <Paragraph style={{ whiteSpace: 'pre-wrap' }}>{answer.answer}</Paragraph>
          {answer.citations?.length > 0 && (
            <>
              <Divider style={{ margin: '8px 0' }} />
              <div>
                <Text type="secondary">引用来源：</Text>
                {answer.citations.map((c: string, i: number) => (
                  <Tag key={i} icon={<BookOutlined />} color="blue">{c}</Tag>
                ))}
              </div>
            </>
          )}
        </Card>
      )}

      {/* Search Results */}
      {loading && <Spin tip="搜索中..." style={{ display: 'block', textAlign: 'center', padding: 40 }} />}

      {results.length > 0 && (
        <List
          header={<Text strong>搜索结果 ({results.length})</Text>}
          dataSource={results}
          renderItem={item => (
            <List.Item>
              <List.Item.Meta
                title={
                  <span>
                    {item.nodeName}
                    <Tag color="cyan" style={{ marginLeft: 8 }}>{item.nodeType}</Tag>
                    <Text type="secondary" style={{ marginLeft: 8, fontSize: 12 }}>
                      相似度: {(item.similarity * 100).toFixed(1)}%
                    </Text>
                  </span>
                }
                description={item.description || '无描述'}
              />
            </List.Item>
          )}
        />
      )}

      {!loading && !asking && results.length === 0 && !answer && query && (
        <Empty description="无搜索结果" />
      )}
    </div>
  );
}

import React, { useState } from 'react';
import { Input, List, Typography, Tag, Card, Divider, Spin, Empty, message, Alert, Tabs } from 'antd';
import { SearchOutlined, RobotOutlined, BookOutlined, ExclamationCircleOutlined, TagsOutlined, LinkOutlined } from '@ant-design/icons';
import { search, askQuestion, searchByTag, searchByRelation, SearchResult, SearchRequest, AnswerResult } from '../api';

const { Title, Text, Paragraph } = Typography;

export default function Search() {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<SearchResult[]>([]);
  const [answer, setAnswer] = useState<AnswerResult | null>(null);
  const [loading, setLoading] = useState(false);
  const [asking, setAsking] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Tag browsing state
  const [tagQuery, setTagQuery] = useState('');
  const [tagResults, setTagResults] = useState<SearchResult[]>([]);
  const [tagLoading, setTagLoading] = useState(false);

  // Relation browsing state
  const [nodeIdInput, setNodeIdInput] = useState('');
  const [relationType, setRelationType] = useState('');
  const [relationResults, setRelationResults] = useState<SearchResult[]>([]);
  const [relationLoading, setRelationLoading] = useState(false);

  const handleSearch = async () => {
    if (!query.trim()) {
      message.warning('请输入搜索关键词');
      return;
    }
    setLoading(true);
    setAnswer(null);
    setError(null);
    try {
      const request: SearchRequest = { query: query.trim() };
      const res = await search(request);
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

  const handleTagSearch = async () => {
    if (!tagQuery.trim()) {
      message.warning('请输入标签名');
      return;
    }
    setTagLoading(true);
    setError(null);
    try {
      const res = await searchByTag(tagQuery.trim(), 20);
      setTagResults(res.data);
      if (res.data.length === 0) {
        message.info('未找到相关标签的内容');
      }
    } catch (e: any) {
      setError(e?.response?.data?.message || '标签搜索失败');
      message.error('标签搜索失败');
    }
    setTagLoading(false);
  };

  const handleRelationSearch = async () => {
    if (!nodeIdInput.trim()) {
      message.warning('请输入节点 ID');
      return;
    }
    setRelationLoading(true);
    setError(null);
    try {
      const res = await searchByRelation(nodeIdInput.trim(), relationType.trim() || undefined, 20);
      setRelationResults(res.data);
      if (res.data.length === 0) {
        message.info('未找到相关节点');
      }
    } catch (e: any) {
      setError(e?.response?.data?.message || '关系搜索失败');
      message.error('关系搜索失败');
    }
    setRelationLoading(false);
  };

  const renderSearchResult = (item: SearchResult) => (
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
  );

  const searchTab = (
    <>
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
          renderItem={renderSearchResult}
        />
      )}

      {!loading && !asking && results.length === 0 && !answer && query && (
        <Empty description="无搜索结果" />
      )}
    </>
  );

  const tagTab = (
    <>
      <div style={{ display: 'flex', gap: 8, marginBottom: 24 }}>
        <Input.Search
          placeholder="输入标签名，如：java、python、tutorial..."
          value={tagQuery}
          onChange={e => setTagQuery(e.target.value)}
          onSearch={handleTagSearch}
          enterButton={<><TagsOutlined /> 按标签浏览</>}
          size="large"
          style={{ flex: 1 }}
          loading={tagLoading}
        />
      </div>

      {tagLoading && <Spin tag="搜索中..." style={{ display: 'block', textAlign: 'center', padding: 40 }} />}

      {tagResults.length > 0 && (
        <List
          header={<Text strong>标签 "{tagQuery}" 的搜索结果 ({tagResults.length})</Text>}
          dataSource={tagResults}
          renderItem={renderSearchResult}
        />
      )}

      {!tagLoading && tagResults.length === 0 && tagQuery && (
        <Empty description="未找到该标签的内容" />
      )}

      {!tagQuery && (
        <Empty description="输入标签名开始浏览" />
      )}
    </>
  );

  const relationTab = (
    <>
      <div style={{ display: 'flex', gap: 8, marginBottom: 24, flexWrap: 'wrap' }}>
        <Input
          placeholder="节点 ID (UUID)"
          value={nodeIdInput}
          onChange={e => setNodeIdInput(e.target.value)}
          size="large"
          style={{ flex: 2, minWidth: 200 }}
        />
        <Input
          placeholder="关系类型 (可选，如 RELATED_TO)"
          value={relationType}
          onChange={e => setRelationType(e.target.value)}
          size="large"
          style={{ flex: 1, minWidth: 150 }}
        />
        <button
          onClick={handleRelationSearch}
          disabled={relationLoading || !nodeIdInput.trim()}
          style={{
            padding: '0 24px', fontSize: 16, borderRadius: 6, border: 'none',
            background: '#1677ff', color: '#fff', cursor: 'pointer'
          }}
        >
          <LinkOutlined /> 按关系查找
        </button>
      </div>

      {relationLoading && <Spin tip="搜索中..." style={{ display: 'block', textAlign: 'center', padding: 40 }} />}

      {relationResults.length > 0 && (
        <List
          header={<Text strong>关系搜索结果 ({relationResults.length})</Text>}
          dataSource={relationResults}
          renderItem={renderSearchResult}
        />
      )}

      {!relationLoading && relationResults.length === 0 && nodeIdInput && (
        <Empty description="未找到相关节点" />
      )}

      {!nodeIdInput && (
        <Empty description="输入节点 ID 开始探索关系" />
      )}
    </>
  );

  return (
    <div>
      <Title level={4}>搜索 & 智能问答</Title>

      <Tabs
        defaultActiveKey="search"
        items={[
          {
            key: 'search',
            label: (
              <span><SearchOutlined /> 语义搜索</span>
            ),
            children: searchTab,
          },
          {
            key: 'tag',
            label: (
              <span><TagsOutlined /> 按标签浏览</span>
            ),
            children: tagTab,
          },
          {
            key: 'relation',
            label: (
              <span><LinkOutlined /> 按关系探索</span>
            ),
            children: relationTab,
          },
        ]}
      />
    </div>
  );
}

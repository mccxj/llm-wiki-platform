import React, { useState } from 'react';
import { Input, Typography, Card, Divider, Spin, Empty, message, Tag, Alert } from 'antd';
import { RobotOutlined, BookOutlined, ExclamationCircleOutlined } from '@ant-design/icons';
import { askQuestion, AnswerResult } from '../api';

const { Title, Text, Paragraph } = Typography;

export default function Qa() {
  const [question, setQuestion] = useState('');
  const [answer, setAnswer] = useState<AnswerResult | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleAsk = async () => {
    if (!question.trim()) {
      message.warning('请输入问题');
      return;
    }
    setLoading(true);
    setError(null);
    setAnswer(null);
    try {
      const res = await askQuestion(question);
      setAnswer(res.data);
    } catch (e: any) {
      setError(e?.response?.data?.message || '问答服务暂时不可用，请稍后重试');
      message.error('问答失败');
    }
    setLoading(false);
  };

  return (
    <div>
      <Title level={4}>智能问答</Title>
      <Text type="secondary">基于知识库内容回答您的问题。如果知识库中没有相关信息，AI会尝试生成答案。</Text>

      <div style={{ display: 'flex', gap: 8, margin: '24px 0' }}>
        <Input.Search
          placeholder="输入您的问题，例如：什么是知识图谱？"
          value={question}
          onChange={e => setQuestion(e.target.value)}
          onSearch={handleAsk}
          enterButton={<><RobotOutlined /> 提问</>}
          size="large"
          style={{ flex: 1 }}
          loading={loading}
        />
      </div>

      {loading && (
        <Spin tip="AI正在思考..." style={{ display: 'block', textAlign: 'center', padding: 60 }} />
      )}

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
              message="此答案由AI生成，可能不完全准确。建议核实关键信息。"
              style={{ marginBottom: 16 }}
            />
          )}

          <Paragraph style={{ whiteSpace: 'pre-wrap', fontSize: 15, lineHeight: 1.8 }}>
            {answer.answer}
          </Paragraph>

          {answer.citations && answer.citations.length > 0 && (
            <>
              <Divider style={{ margin: '12px 0' }} />
              <div>
                <Text type="secondary" strong>引用来源：</Text>
                <div style={{ marginTop: 8 }}>
                  {answer.citations.map((c: string, i: number) => (
                    <Tag key={i} icon={<BookOutlined />} color="blue" style={{ marginBottom: 4 }}>{c}</Tag>
                  ))}
                </div>
              </div>
            </>
          )}
        </Card>
      )}

      {!loading && !answer && !error && (
        <Empty description="输入问题开始问答" />
      )}
    </div>
  );
}

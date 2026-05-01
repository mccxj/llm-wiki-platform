import React from 'react';
import { Tag, Typography } from 'antd';

const { Text } = Typography;

interface DiffViewerProps {
  before: string;
  after: string;
  mode?: 'unified' | 'split';
}

interface DiffLine {
  type: 'unchanged' | 'added' | 'removed';
  content: string;
  oldLineNum?: number;
  newLineNum?: number;
}

/**
 * 文本差异对比组件
 * 支持 unified 和 split 两种展示模式
 */
export default function DiffViewer({ before, after, mode = 'unified' }: DiffViewerProps) {
  const computeLines = (): DiffLine[] => {
    if (!before && !after) return [];

    const beforeLines = (before || '').split('\n');
    const afterLines = (after || '').split('\n');
    const result: DiffLine[] = [];

    // Simple LCS-based diff
    const m = beforeLines.length;
    const n = afterLines.length;
    const dp: number[][] = Array.from({ length: m + 1 }, () => Array(n + 1).fill(0));

    for (let i = 1; i <= m; i++) {
      for (let j = 1; j <= n; j++) {
        if (beforeLines[i - 1] === afterLines[j - 1]) {
          dp[i][j] = dp[i - 1][j - 1] + 1;
        } else {
          dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
        }
      }
    }

    // Backtrack
    let i = m, j = n;
    const ops: DiffLine[] = [];
    while (i > 0 || j > 0) {
      if (i > 0 && j > 0 && beforeLines[i - 1] === afterLines[j - 1]) {
        ops.push({ type: 'unchanged', content: beforeLines[i - 1], oldLineNum: i, newLineNum: j });
        i--; j--;
      } else if (j > 0 && (i === 0 || dp[i][j - 1] >= dp[i - 1][j])) {
        ops.push({ type: 'added', content: afterLines[j - 1], newLineNum: j });
        j--;
      } else {
        ops.push({ type: 'removed', content: beforeLines[i - 1], oldLineNum: i });
        i--;
      }
    }
    ops.reverse();
    return ops;
  };

  const lines = computeLines();

  const lineColor = (type: string) => {
    switch (type) {
      case 'added': return { background: '#e6fffb', color: '#006d75' };
      case 'removed': return { background: '#fff1f0', color: '#cf1322' };
      default: return {};
    }
  };

  const linePrefix = (type: string) => {
    switch (type) {
      case 'added': return '+';
      case 'removed': return '-';
      default: return ' ';
    }
  };

  if (mode === 'unified') {
    return (
      <div style={{ border: '1px solid #d9d9d9', borderRadius: 6, overflow: 'hidden', fontFamily: 'monospace', fontSize: 13 }}>
        <div style={{ background: '#fafafa', padding: '8px 12px', borderBottom: '1px solid #d9d9d9' }}>
          <Tag color="blue">变更前</Tag>
          <Tag color="green" style={{ marginLeft: 8 }}>变更后</Tag>
          <Text type="secondary" style={{ marginLeft: 16, fontSize: 12 }}>
            共 {lines.filter(l => l.type === 'added').length} 行新增, {lines.filter(l => l.type === 'removed').length} 行删除
          </Text>
        </div>
        <div style={{ maxHeight: 500, overflow: 'auto' }}>
          {lines.map((line, idx) => (
            <div
              key={idx}
              style={{
                display: 'flex',
                ...lineColor(line.type),
                borderBottom: '1px solid #f0f0f0',
              }}
            >
              <span style={{ width: 40, padding: '2px 8px', textAlign: 'right', color: '#999', userSelect: 'none', borderRight: '1px solid #f0f0f0', flexShrink: 0 }}>
                {line.oldLineNum || ''}
              </span>
              <span style={{ width: 40, padding: '2px 8px', textAlign: 'right', color: '#999', userSelect: 'none', borderRight: '1px solid #f0f0f0', flexShrink: 0 }}>
                {line.newLineNum || ''}
              </span>
              <span style={{ width: 20, padding: '2px 4px', textAlign: 'center', userSelect: 'none', flexShrink: 0, fontWeight: 'bold' }}>
                {linePrefix(line.type)}
              </span>
              <span style={{ padding: '2px 8px', whiteSpace: 'pre-wrap', flex: 1 }}>
                {line.content || '\u00A0'}
              </span>
            </div>
          ))}
        </div>
      </div>
    );
  }

  // Split mode
  const removedLines = lines.filter(l => l.type === 'removed');
  const addedLines = lines.filter(l => l.type === 'added');
  const maxLen = Math.max(removedLines.length, addedLines.length);

  return (
    <div style={{ display: 'flex', gap: 16, fontFamily: 'monospace', fontSize: 13 }}>
      <div style={{ flex: 1, border: '1px solid #ffa39e', borderRadius: 6, overflow: 'hidden' }}>
        <div style={{ background: '#fff1f0', padding: '8px 12px', borderBottom: '1px solid #ffa39e' }}>
          <Tag color="red">变更前</Tag>
        </div>
        <div style={{ maxHeight: 500, overflow: 'auto' }}>
          {Array.from({ length: maxLen }).map((_, idx) => {
            const line = removedLines[idx];
            return (
              <div key={idx} style={{ padding: '2px 8px', background: line ? '#fff1f0' : '#fff', borderBottom: '1px solid #f0f0f0', minHeight: 24 }}>
                {line?.content || '\u00A0'}
              </div>
            );
          })}
        </div>
      </div>
      <div style={{ flex: 1, border: '1px solid #b7eb8f', borderRadius: 6, overflow: 'hidden' }}>
        <div style={{ background: '#f6ffed', padding: '8px 12px', borderBottom: '1px solid #b7eb8f' }}>
          <Tag color="green">变更后</Tag>
        </div>
        <div style={{ maxHeight: 500, overflow: 'auto' }}>
          {Array.from({ length: maxLen }).map((_, idx) => {
            const line = addedLines[idx];
            return (
              <div key={idx} style={{ padding: '2px 8px', background: line ? '#f6ffed' : '#fff', borderBottom: '1px solid #f0f0f0', minHeight: 24 }}>
                {line?.content || '\u00A0'}
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}

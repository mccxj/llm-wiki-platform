import React from 'react';

interface SearchHighlightProps {
  text: string;
  query: string;
  maxLength?: number;
}

/**
 * 搜索关键词高亮组件
 * 在文本中高亮显示搜索关键词，支持截断上下文
 */
export default function SearchHighlight({ text, query, maxLength = 200 }: SearchHighlightProps) {
  if (!text) return <span>-</span>;

  // Truncate text around the first match
  const getSnippet = (): string => {
    if (!query) {
      return text.length > maxLength ? text.substring(0, maxLength) + '...' : text;
    }
    const lowerText = text.toLowerCase();
    const lowerQuery = query.toLowerCase();
    const matchIdx = lowerText.indexOf(lowerQuery);
    if (matchIdx === -1) {
      return text.length > maxLength ? text.substring(0, maxLength) + '...' : text;
    }
    const start = Math.max(0, matchIdx - Math.floor((maxLength - query.length) / 2));
    const end = Math.min(text.length, start + maxLength);
    const snippet = text.substring(start, end);
    return (start > 0 ? '...' : '') + snippet + (end < text.length ? '...' : '');
  };

  const snippet = getSnippet();

  if (!query) {
    return <span>{snippet}</span>;
  }

  // Split by query and insert highlight markers
  const parts = snippet.split(new RegExp(`(${query.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`, 'gi'));

  return (
    <span>
      {parts.map((part, i) =>
        part.toLowerCase() === query.toLowerCase() ? (
          <mark key={i} style={{ background: '#ffe58f', padding: '0 2px', borderRadius: 2 }}>
            {part}
          </mark>
        ) : (
          <span key={i}>{part}</span>
        )
      )}
    </span>
  );
}

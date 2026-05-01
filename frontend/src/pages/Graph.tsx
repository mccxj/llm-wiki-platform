import React, { useEffect, useRef, useState, useCallback } from 'react';
import { Card, Typography, Spin, message, Button, Space, Tag, Drawer, Descriptions } from 'antd';
import { ReloadOutlined, NodeIndexOutlined } from '@ant-design/icons';
import * as d3 from 'd3';
import { getGraphData, getOrphans, GraphData, GraphNode } from '../api';

const { Title, Text } = Typography;

const NODE_COLORS: Record<string, string> = {
  ENTITY: '#1677ff',
  CONCEPT: '#52c41a',
  COMPARISON: '#faad14',
  RAW_SOURCE: '#722ed1',
  QUERY: '#eb2f96',
};

interface SimNode extends d3.SimulationNodeDatum {
  id: string;
  name: string;
  type: string;
  description?: string;
}

interface SimLink extends d3.SimulationLinkDatum<SimNode> {
  type: string;
  weight: number;
}

export default function Graph() {
  const svgRef = useRef<SVGSVGElement>(null);
  const [loading, setLoading] = useState(true);
  const [graphData, setGraphData] = useState<{ nodes: SimNode[]; links: SimLink[] }>({ nodes: [], links: [] });
  const [selectedNode, setSelectedNode] = useState<SimNode | null>(null);
  const [orphans, setOrphans] = useState<GraphNode[]>([]);
  const [showOrphans, setShowOrphans] = useState(false);

  const loadGraph = useCallback(async () => {
    setLoading(true);
    try {
      const [graphRes, orphansRes] = await Promise.all([getGraphData(), getOrphans()]);
      const data = graphRes.data;
      setGraphData({
        nodes: data.nodes.map((n: GraphNode) => ({ ...n })),
        links: data.edges.map((e: any) => ({ ...e })),
      });
      setOrphans(orphansRes.data);
    } catch (e) {
      message.error('加载图谱失败');
    }
    setLoading(false);
  }, []);

  useEffect(() => { loadGraph(); }, [loadGraph]);

  useEffect(() => {
    if (!svgRef.current || graphData.nodes.length === 0) return;

    const svg = d3.select(svgRef.current);
    svg.selectAll('*').remove();

    const width = svgRef.current.clientWidth || 900;
    const height = 600;

    svg.attr('viewBox', `0 0 ${width} ${height}`);

    // Arrow marker
    svg.append('defs').append('marker')
      .attr('id', 'arrowhead')
      .attr('viewBox', '-0 -5 10 10')
      .attr('refX', 20)
      .attr('refY', 0)
      .attr('orient', 'auto')
      .attr('markerWidth', 6)
      .attr('markerHeight', 6)
      .append('path')
      .attr('d', 'M 0,-5 L 10,0 L 0,5')
      .attr('fill', '#999');

    const g = svg.append('g');

    // Zoom
    const zoom = d3.zoom<SVGSVGElement, unknown>()
      .scaleExtent([0.1, 4])
      .on('zoom', (event) => g.attr('transform', event.transform));
    svg.call(zoom);

    const simulation = d3.forceSimulation<SimNode>(graphData.nodes)
      .force('link', d3.forceLink<SimNode, SimLink>(graphData.links).id((d: any) => d.id).distance(100))
      .force('charge', d3.forceManyBody().strength(-300))
      .force('center', d3.forceCenter(width / 2, height / 2))
      .force('collision', d3.forceCollide().radius(30));

    // Links
    const link = g.append('g')
      .selectAll('line')
      .data(graphData.links)
      .enter().append('line')
      .attr('stroke', '#999')
      .attr('stroke-opacity', 0.6)
      .attr('stroke-width', (d: any) => Math.max(1, d.weight * 3))
      .attr('marker-end', 'url(#arrowhead)');

    // Link labels
    const linkLabel = g.append('g')
      .selectAll('text')
      .data(graphData.links)
      .enter().append('text')
      .attr('font-size', 9)
      .attr('fill', '#999')
      .text((d: any) => d.type);

    // Nodes
    const node = g.append('g')
      .selectAll('circle')
      .data(graphData.nodes)
      .enter().append('circle')
      .attr('r', 12)
      .attr('fill', (d: any) => NODE_COLORS[d.type] || '#999')
      .attr('stroke', '#fff')
      .attr('stroke-width', 2)
      .style('cursor', 'pointer')
      .call(d3.drag<SVGCircleElement, SimNode>()
        .on('start', (event, d: any) => {
          if (!event.active) simulation.alphaTarget(0.3).restart();
          d.fx = d.x; d.fy = d.y;
        })
        .on('drag', (event, d: any) => { d.fx = event.x; d.fy = event.y; })
        .on('end', (event, d: any) => {
          if (!event.active) simulation.alphaTarget(0);
          d.fx = null; d.fy = null;
        }))
      .on('click', (_event, d: any) => setSelectedNode(d));

    // Node labels
    const label = g.append('g')
      .selectAll('text')
      .data(graphData.nodes)
      .enter().append('text')
      .attr('font-size', 11)
      .attr('fill', '#333')
      .attr('dx', 15)
      .attr('dy', 4)
      .text((d: any) => d.name);

    simulation.on('tick', () => {
      link.attr('x1', (d: any) => d.source.x).attr('y1', (d: any) => d.source.y)
          .attr('x2', (d: any) => d.target.x).attr('y2', (d: any) => d.target.y);
      linkLabel.attr('x', (d: any) => ((d.source as any).x + (d.target as any).x) / 2)
               .attr('y', (d: any) => ((d.source as any).y + (d.target as any).y) / 2);
      node.attr('cx', (d: any) => d.x!).attr('cy', (d: any) => d.y!);
      label.attr('x', (d: any) => d.x!).attr('y', (d: any) => d.y!);
    });

  }, [graphData]);

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Space>
          <Title level={4} style={{ margin: 0 }}>知识图谱</Title>
          <Text type="secondary">共 {graphData.nodes.length} 个节点, {graphData.links.length} 条边</Text>
        </Space>
        <Space>
          <Button icon={<NodeIndexOutlined />} onClick={() => setShowOrphans(true)}>
            孤儿节点 ({orphans.length})
          </Button>
          <Button icon={<ReloadOutlined />} onClick={loadGraph} loading={loading}>刷新</Button>
        </Space>
      </div>

      {/* Legend */}
      <Space style={{ marginBottom: 8 }}>
        {Object.entries(NODE_COLORS).map(([type, color]) => (
          <Tag key={type} color={color}>{type}</Tag>
        ))}
      </Space>

      <Card bodyStyle={{ padding: 0 }}>
        {loading ? (
          <Spin tip="加载中..." style={{ display: 'block', padding: 100, textAlign: 'center' }} />
        ) : graphData.nodes.length === 0 ? (
          <div style={{ padding: 100, textAlign: 'center' }}>暂无图谱数据。请先同步文档并运行处理流水线。</div>
        ) : (
          <svg ref={svgRef} width="100%" height={600} style={{ display: 'block' }} />
        )}
      </Card>

      {/* Node detail drawer */}
      <Drawer title="节点详情" open={!!selectedNode} onClose={() => setSelectedNode(null)} width={400}>
        {selectedNode && (
          <Descriptions column={1} size="small">
            <Descriptions.Item label="名称">{selectedNode.name}</Descriptions.Item>
            <Descriptions.Item label="类型"><Tag color={NODE_COLORS[selectedNode.type]}>{selectedNode.type}</Tag></Descriptions.Item>
            <Descriptions.Item label="ID">{selectedNode.id}</Descriptions.Item>
            <Descriptions.Item label="描述">{selectedNode.description || '无'}</Descriptions.Item>
          </Descriptions>
        )}
      </Drawer>

      {/* Orphans drawer */}
      <Drawer title="孤儿节点" open={showOrphans} onClose={() => setShowOrphans(false)} width={500}>
        {orphans.length === 0 ? (
          <Text type="secondary">没有孤儿节点</Text>
        ) : (
          <div>
            {orphans.map((n: GraphNode) => (
              <div key={n.id} style={{ padding: 8, borderBottom: '1px solid #f0f0f0' }}>
                <Tag color={NODE_COLORS[n.type] || '#999'}>{n.type}</Tag>
                <Text strong>{n.name}</Text>
                {n.description && <Text type="secondary" style={{ display: 'block', fontSize: 12 }}>{n.description}</Text>}
              </div>
            ))}
          </div>
        )}
      </Drawer>
    </div>
  );
}

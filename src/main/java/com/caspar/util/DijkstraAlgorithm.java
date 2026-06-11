package com.caspar.util;

import com.caspar.entity.CampusPathEdge;
import com.caspar.entity.CampusPathNode;
import com.caspar.entity.dto.RouteResponse;

import java.math.BigDecimal;
import java.util.*;

/**
 * Dijkstra算法实现类
 * 用于计算校园路径规划中的最短路径
 *
 * 算法复杂度: O((V + E) * log V)，其中V为节点数，E为边数
 */
public class DijkstraAlgorithm {

    /**
     * 图的邻接表表示
     * key: 节点ID, value: 从该节点出发的所有边
     */
    private final Map<Long, List<Edge>> adjacencyList;

    /**
     * 节点ID到节点对象的映射
     */
    private final Map<Long, CampusPathNode> nodeMap;

    /**
     * 内部边类
     */
    private static class Edge {
        long to;
        double weight;

        Edge(long to, double weight) {
            this.to = to;
            this.weight = weight;
        }
    }

    /**
     * 构造函数
     *
     * @param nodes  所有节点
     * @param edges  所有边
     * @param wayType 道路类型过滤（WALK或CYCLE）
     */
    public DijkstraAlgorithm(List<CampusPathNode> nodes, List<CampusPathEdge> edges, String wayType) {
        this.adjacencyList = new HashMap<>();
        this.nodeMap = new HashMap<>();

        // 初始化节点映射
        for (CampusPathNode node : nodes) {
            nodeMap.put(node.getId(), node);
            adjacencyList.put(node.getId(), new ArrayList<>());
        }

        // 构建邻接表
        for (CampusPathEdge edge : edges) {
            // 如果指定了道路类型，则过滤
            if (wayType != null && !wayType.equals(edge.getWayType())) {
                continue;
            }
            adjacencyList.get(edge.getFromNodeId()).add(new Edge(edge.getToNodeId(), edge.getDistance()));
        }
    }

    /**
     * 计算从起点到终点的最短路径
     *
     * @param fromNodeId 起点节点ID
     * @param toNodeId   终点节点ID
     * @return 路径结果，包含路径点和总距离
     */
    public PathResult findShortestPath(Long fromNodeId, Long toNodeId) {
        // 距离表：节点ID -> 最短距离
        Map<Long, Double> distances = new HashMap<>();
        // 前驱表：节点ID -> 前驱节点ID（用于路径重建）
        Map<Long, Long> predecessors = new HashMap<>();
        // 已访问节点集合
        Set<Long> visited = new HashSet<>();
        // 优先队列：(节点ID, 距离)
        PriorityQueue<NodeDistance> pq = new PriorityQueue<>(Comparator.comparingDouble(nd -> nd.distance));

        // 初始化
        for (Long nodeId : adjacencyList.keySet()) {
            distances.put(nodeId, Double.MAX_VALUE);
        }
        distances.put(fromNodeId, 0.0);
        pq.offer(new NodeDistance(fromNodeId, 0.0));

        // Dijkstra算法主循环
        while (!pq.isEmpty()) {
            NodeDistance current = pq.poll();
            Long currentNodeId = current.nodeId;

            // 如果已访问，跳过
            if (visited.contains(currentNodeId)) {
                continue;
            }
            visited.add(currentNodeId);

            // 找到终点，可以提前结束
            if (currentNodeId.equals(toNodeId)) {
                break;
            }

            // 遍历相邻节点
            List<Edge> edges = adjacencyList.get(currentNodeId);
            if (edges == null) {
                continue;
            }

            for (Edge edge : edges) {
                if (visited.contains(edge.to)) {
                    continue;
                }

                double newDistance = distances.get(currentNodeId) + edge.weight;
                if (newDistance < distances.get(edge.to)) {
                    distances.put(edge.to, newDistance);
                    predecessors.put(edge.to, currentNodeId);
                    pq.offer(new NodeDistance(edge.to, newDistance));
                }
            }
        }

        // 如果终点不可达，返回null
        if (distances.get(toNodeId) == Double.MAX_VALUE) {
            return null;
        }

        // 重建路径
        List<Long> path = reconstructPath(predecessors, fromNodeId, toNodeId);
        return new PathResult(path, distances.get(toNodeId));
    }

    /**
     * 重建路径
     */
    private List<Long> reconstructPath(Map<Long, Long> predecessors, Long fromNodeId, Long toNodeId) {
        List<Long> path = new ArrayList<>();
        Long current = toNodeId;

        while (current != null) {
            path.add(current);
            current = predecessors.get(current);
        }

        Collections.reverse(path);
        return path;
    }

    /**
     * 内部类：节点-距离对
     */
    private static class NodeDistance {
        long nodeId;
        double distance;

        NodeDistance(long nodeId, double distance) {
            this.nodeId = nodeId;
            this.distance = distance;
        }
    }

    /**
     * 路径结果类
     */
    public static class PathResult {
        public final List<Long> nodeIds;
        public final double totalDistance;

        public PathResult(List<Long> nodeIds, double totalDistance) {
            this.nodeIds = nodeIds;
            this.totalDistance = totalDistance;
        }
    }

    /**
     * 计算两点之间的直线距离（米）
     * 使用Haversine公式
     *
     * @param lat1 起点纬度
     * @param lon1 起点经度
     * @param lat2 终点纬度
     * @param lon2 终点经度
     * @return 距离（米）
     */
    public static double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000; // 地球半径（米）

        double latRad1 = Math.toRadians(lat1);
        double latRad2 = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(latRad1) * Math.cos(latRad2) *
                Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    /**
     * 将PathResult转换为RouteResponse
     *
     * @param fromLat  起点纬度
     * @param fromLng  起点经度
     * @param toLat    终点纬度
     * @param toLng    终点经度
     * @param pathResult 路径结果
     * @return RouteResponse
     */
    public RouteResponse toRouteResponse(BigDecimal fromLat, BigDecimal fromLng,
                                         BigDecimal toLat, BigDecimal toLng,
                                         PathResult pathResult) {
        if (pathResult == null) {
            return null;
        }

        List<RouteResponse.RoutePoint> points = new ArrayList<>();
        double cumulativeDistance = 0;

        for (int i = 0; i < pathResult.nodeIds.size(); i++) {
            Long nodeId = pathResult.nodeIds.get(i);
            CampusPathNode node = nodeMap.get(nodeId);

            if (node != null) {
                cumulativeDistance = (i == 0) ? 0 :
                        cumulativeDistance + calculateHaversineDistance(
                                nodeMap.get(pathResult.nodeIds.get(i - 1)).getLatitude().doubleValue(),
                                nodeMap.get(pathResult.nodeIds.get(i - 1)).getLongitude().doubleValue(),
                                node.getLatitude().doubleValue(),
                                node.getLongitude().doubleValue()
                        );

                points.add(RouteResponse.RoutePoint.builder()
                        .nodeId(node.getId())
                        .nodeName(node.getName())
                        .latitude(node.getLatitude())
                        .longitude(node.getLongitude())
                        .nodeType(node.getNodeType())
                        .cumulativeDistance(cumulativeDistance)
                        .build());
            }
        }

        // 计算预计时间（假设步行速度5km/h）
        double walkingSpeed = 5.0 * 1000 / 3600; // 米/秒
        int estimatedTime = (int) (pathResult.totalDistance / walkingSpeed);

        // 生成路径描述
        String description = generateDescription(points);

        return RouteResponse.builder()
                .fromLat(fromLat)
                .fromLng(fromLng)
                .toLat(toLat)
                .toLng(toLng)
                .totalDistance(pathResult.totalDistance)
                .estimatedTime(estimatedTime)
                .points(points)
                .description(description)
                .build();
    }

    /**
     * 生成路径描述
     */
    private String generateDescription(List<RouteResponse.RoutePoint> points) {
        if (points == null || points.isEmpty()) {
            return "未找到可行路径";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("从 ").append(points.get(0).getNodeName()).append(" 到 ").append(points.get(points.size() - 1).getNodeName()).append("，");
        sb.append("途经 ").append(points.size()).append(" 个节点，");
        sb.append("总距离约 ").append(String.format("%.0f", points.get(points.size() - 1).getCumulativeDistance())).append(" 米");

        return sb.toString();
    }
}

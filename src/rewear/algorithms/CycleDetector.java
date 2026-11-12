package rewear.algorithms;

import java.util.*;

/**
 * Directed Graph Representation and Cycle Detection Algorithm for Multi-Party Barter (SwapChain).
 * Demonstrates:
 * - Graph Data Structure (Adjacency List using Map and List)
 * - Depth-First Search (DFS) Traversal
 * - Recursion and Backtracking
 * - Cycle Detection with Path Reconstruction
 * Core academic viva requirement.
 */
public class CycleDetector {

    public static class Edge {
        private final String fromStudentId;
        private final String toStudentId;
        private final String fromListingId;
        private final String toListingId;

        public Edge(String fromStudentId, String toStudentId, String fromListingId, String toListingId) {
            this.fromStudentId = fromStudentId;
            this.toStudentId = toStudentId;
            this.fromListingId = fromListingId;
            this.toListingId = toListingId;
        }

        public String getFromStudentId() {
            return fromStudentId;
        }

        public String getToStudentId() {
            return toStudentId;
        }

        public String getFromListingId() {
            return fromListingId;
        }

        public String getToListingId() {
            return toListingId;
        }
    }

    private final Map<String, List<Edge>> adjacencyList = new HashMap<>();

    public void addEdge(String fromStudent, String toStudent, String fromListing, String toListing) {
        adjacencyList.computeIfAbsent(fromStudent, k -> new ArrayList<>())
                     .add(new Edge(fromStudent, toStudent, fromListing, toListing));
    }

    /**
     * Finds simple directed cycles of length >= 3 using DFS with recursion stack tracking.
     */
    public List<List<Edge>> findDirectedCycles(int minLength, int maxLength) {
        List<List<Edge>> allCycles = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        List<Edge> currentPath = new ArrayList<>();
        Set<String> inCurrentPath = new HashSet<>();

        for (String node : adjacencyList.keySet()) {
            dfs(node, node, visited, inCurrentPath, currentPath, allCycles, minLength, maxLength);
            visited.add(node);
        }

        return allCycles;
    }

    private void dfs(String startNode, String currentNode, Set<String> visited,
                     Set<String> inCurrentPath, List<Edge> currentPath,
                     List<List<Edge>> allCycles, int minLength, int maxLength) {

        inCurrentPath.add(currentNode);

        List<Edge> edges = adjacencyList.getOrDefault(currentNode, Collections.emptyList());
        for (Edge edge : edges) {
            String nextNode = edge.getToStudentId();

            if (nextNode.equals(startNode) && currentPath.size() + 1 >= minLength) {
                // Cycle found!
                List<Edge> cycle = new ArrayList<>(currentPath);
                cycle.add(edge);
                if (cycle.size() <= maxLength) {
                    allCycles.add(cycle);
                }
            } else if (!visited.contains(nextNode) && !inCurrentPath.contains(nextNode)) {
                if (currentPath.size() + 1 < maxLength) {
                    currentPath.add(edge);
                    dfs(startNode, nextNode, visited, inCurrentPath, currentPath, allCycles, minLength, maxLength);
                    currentPath.remove(currentPath.size() - 1);
                }
            }
        }

        inCurrentPath.remove(currentNode);
    }
}


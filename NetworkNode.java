import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

class NetworkNode{
    private String name;

    private

    private List<NetworkNode> shortestPath = new LinkedList<>();

    private Integer distance = Integer.MAX_VALUE;

    Map<NetworkNode, Integer> adjacentNodes = new HashMap<>();

    public void addDestination(NetworkNode destination, int distance) {
        adjacentNodes.put(destination, distance);
    }

    public NetworkNode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<NetworkNode> getShortestPath() {
        return shortestPath;
    }

    public void setShortestPath(List<NetworkNode> shortestPath) {
        this.shortestPath = shortestPath;
    }

    public Integer getDistance() {
        return distance;
    }

    public void setDistance(Integer distance) {
        this.distance = distance;
    }

    public Map<NetworkNode, Integer> getAdjacentNodes() {
        return adjacentNodes;
    }

    public void setAdjacentNodes(Map<NetworkNode, Integer> adjacentNodes) {
        this.adjacentNodes = adjacentNodes;
    }
}
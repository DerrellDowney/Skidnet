import java.util.HashSet;
import java.util.Set;


public class Graph {
    private Set<NetworkNode> nodes = new HashSet<>();

    public void addNode(NetworkNode nodeA) {
        nodes.add(nodeA);
    }

    public Set<NetworkNode> getNodes() {
        return nodes;
    }

    public void setNodes(Set<NetworkNode> nodes) {
        this.nodes = nodes;
    }
}

package Cache;

import GraphInstance.SingleGraphInstance;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by The Illsionist on 2018/10/20.
 */
public class ClassCache {
    private final static int DEFAULT_CAPACITY = 682;  //TODO:默认初始容量的选择还有待调研
    private final static GraphDatabaseService graphDb = SingleGraphInstance.getInstance();  //从单例拿数据库实例
    private final static ConcurrentHashMap<Node,ConcurrentHashMap<Node,Relationship>> classWithRels = new ConcurrentHashMap<>(170);

    static {

    }
}

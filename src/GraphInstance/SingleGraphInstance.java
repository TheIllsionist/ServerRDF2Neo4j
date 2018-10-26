package GraphInstance;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import java.io.File;

/**
 * Created by The Illsionist on 2018/10/20.
 * 获取Neo4j内置数据库实例,对于一个数据库同时最多只能有一个实例,该实例是线程安全的,所以使用单例模式
 * 使用DCL双锁检查机制实现
 * TODO:后面可以通过延迟初始化占位类模式实现
 */
public class SingleGraphInstance {

    private final static String dbDir = "/home/awakedreaming/Documents/Database/Neo4j/neo4j-community-3.3.3/data/databases/ksedb.db"; //数据库实例文件夹的路径
    private volatile static GraphDatabaseService graphDb = null;  //注意该引用一定要用volatile变量修饰

    public static GraphDatabaseService getInstance(){
        if(graphDb == null){
            synchronized (SingleGraphInstance.class){
                if(graphDb == null){
                    graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(new File(dbDir));
                }
            }
        }
        return graphDb;
    }
}

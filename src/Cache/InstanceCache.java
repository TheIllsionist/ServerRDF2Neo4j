package Cache;

import GraphInstance.SingleGraphInstance;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by The Illsionist on 2018/10/22.
 * 实例缓存,多线程读,多线程写
 * 多线程读/写实例缓存,由于存在“先检查-后执行”竞态条件,因此必须要保证每个线程所写入的实例集间互不相交才可保证不重复写
 */
public class InstanceCache {

    private final static int DEFAULT_INSCOUNT = 3000;  //TODO:默认初始容量的选择还有待调研
    private final static ConcurrentHashMap<String,Object> individuals = new ConcurrentHashMap<>();
    private final static GraphDatabaseService graphDb = SingleGraphInstance.getInstance();  //从单例获取

    static {
        try(Transaction tx = graphDb.beginTx()){
            Result result = graphDb.execute("match(ins:OWL_NAMEDINDIVIDUAL) return ins.preLabel as preLabel");
            tx.success();  //TODO:提交事务的位置不确定,先在此处提交
            Map<String,Object> tmpRes = null;
            while(result.hasNext()){
                tmpRes = result.next();
                String preLabel = tmpRes.get("preLabel").toString();
                if(preLabel.equals("null"))
                    continue;
                if(!individuals.containsKey(preLabel)){
                    individuals.put(preLabel,new Object());
                }
            }
        }
    }

    /**
     * 判断某实例是否已存在于数据库中
     * @param preLabel
     * @return
     */
    public static boolean insContained(String preLabel){
        return individuals.containsKey(preLabel);
    }

    /**
     * 往缓存中加入新实例
     * @param preLabel
     */
    public static void addIndividual(String preLabel){
        individuals.put(preLabel,new Object());  //ConcurrentHashMap的key和value都不能为空
    }


}

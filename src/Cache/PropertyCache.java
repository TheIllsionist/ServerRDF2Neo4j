package Cache;

import GraphInstance.SingleGraphInstance;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PropertyCache {

    private final static int DEFAULT_CAPACITY = 5460;  //TODO:默认初始容量的选择还有待调研
    private final static GraphDatabaseService graphDb = SingleGraphInstance.getInstance();  //从单例获取
    /** 利用静态初始化器保证对象引用的可见性,当前实现为只缓存每个属性的preLabel,当前初始容量默认为5460 **/
    private final static ConcurrentHashMap<String,ConcurrentHashMap<String,Integer>> propWithRels = new ConcurrentHashMap<>(84);

    //TODO:此时加载知识库中的内容到内存中是一个合适的时机吗?从性能上考虑
    static {  //类加载时即查询知识库中已有的属性
        try(Transaction tx = graphDb.beginTx()){
            Result result = graphDb.execute("match(p:OWL_DATATYPEPROPERTY) " +
                    " optional match(p)-[r:RDFS_SUBPROPERTYOF|:EQUIVALENT_PROPERTY|:DISJOINT_PROPERTY|:INVERSE_PROPERTY]->(anop:OWL_DATATYPEPROPERTY) " +
                    " return p.preLabel as p,case r.preLabel when \"rdfs:subPropertyOf\" then 1 when \"owl:equivalentProperty\" then 2 " +
                    " when \"owl:propertyDisjointWith\" then 3 when \"owl:inverseOf\" then 4 end as tag,anop.preLabel as anop union " +
                    " match(p:OWL_OBJECTPROPERTY) optional match(p)-[r:RDFS_SUBPROPERTYOF|:EQUIVALENT_PROPERTY|:DISJOINT_PROPERTY|:INVERSE_PROPERTY]->(anop:OWL_OBJECTPROPERTY) " +
                    " return p.preLabel as p,case r.preLabel when \"rdfs:subPropertyOf\" then 1 when \"owl:equivalentProperty\" then 2 " +
                    " when \"owl:propertyDisjointWith\" then 3 when \"owl:inverseOf\" then 4 end as tag,anop.preLabel as anop ");
            tx.success();
            Map<String,Object> tmpRes = null;
            while(result.hasNext()){
                tmpRes = result.next();
                String p = tmpRes.get("p").toString();
                if(p.equals("null"))
                    continue;
                if(!propWithRels.containsKey(p)){
                    propWithRels.put(p,new ConcurrentHashMap<>());
                }
                if(tmpRes.get("anop") == null || tmpRes.get("anop").equals("null"))
                    continue;
                String anop = tmpRes.get("anop").toString();
                propWithRels.get(p).put(anop,Integer.valueOf(tmpRes.get("tag").toString()));
            }
        }
    }

    /**
     * 判断某个属性是否早已被写入知识库
     * @param preLabel &nbsp 唯一标识该属性的preLabel(这只是目前的评价指标)
     * @return true表示该属性已存在于知识库中,false表示该属性未存在于知识库中
     */
    public static boolean isPropertyContained(String preLabel){
        return propWithRels.containsKey(preLabel);
    }

    /**
     * 往缓存中写入新属性,缓存写入紧接着知识库写入并且一定要在知识库写入之后(禁止指令重排序)
     * @param preLabel &nbsp 唯一标识该类的preLabel
     */
    public static void addProperty(String preLabel){
        propWithRels.put(preLabel,new ConcurrentHashMap<>());
    }

    /**
     * 判断某两个属性之间的关系(这里的关系是具有先序性的)是否早已存在于知识库中
     * @param fPre 先序属性
     * @param lPre 后序属性
     * @return true 如果两个属性之间的关系已被写入知识库
     * 注:这个方法调用之前会必须先调用两次isPropertyContained方法判断两个属性是否都被写入知识库中了
     */
    public static boolean isRelExisted(String fPre,String lPre){
        return propWithRels.get(fPre).get(lPre) != null;
    }

    /**
     * 将一个新加入知识库的关系写入缓存
     * @param fPre 先序属性
     * @param lPre 后序属性
     * @param tag 关系种类标签
     * 注：在调用isRelExisted方法并确定关系不在知识库中后,将关系写入知识库然后才能调用该方法写缓存
     */
    public static void addRelation(String fPre,String lPre,int tag){
        propWithRels.get(fPre).put(lPre,tag);
    }
}

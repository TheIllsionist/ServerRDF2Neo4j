package Cache;

import GraphInstance.SingleGraphInstance;
import org.neo4j.graphdb.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by The Illsionist on 2018/10/20.
 * 类及其关系缓存
 * 知识库中类数目有限,所以可缓存全部的类及其之间的关系
 * 类之间主要有三种语义关系：subClassOf,equivalentClass,disjointClass
 * 当前缓存基于：{实例封闭 + 将线程安全性委托给现有线程安全类 + 特殊的线程读写方式} 实现线程安全性
 * 两个类间的3种关系是互斥存在的,即假如类A是类B的子类,则类A与类B之间不会再有其他关系
 */
public class ClassCache {

    private final static int DEFAULT_CAPACITY = 682;  //TODO:默认初始容量的选择还有待调研
    private final static GraphDatabaseService graphDb = SingleGraphInstance.getInstance();  //从单例拿数据库实例
    private final static ConcurrentHashMap<String,ConcurrentHashMap<String,Integer>> classWithRels = new ConcurrentHashMap<>(170);

    static {
        try(Transaction tx = graphDb.beginTx())
        {
            Result result = graphDb.execute("match(cls:OWL_CLASS) optional match(cls)-[r:RDFS_SUBCLASSOF|:EQUIVALENT_CLASS|:DISJOINT_CLASS]->(anoCls:OWL_CLASS) " +
                    "return cls.preLabel as cls, case r.preLabel when \"rdfs:subClassOf\" then 1 when \"owl:equivalentClass\" then 2 " +
                    " when \"owl:disjointWith\" then 3 end as tag,anoCls.preLabel as anoCls ");  //同样是执行查询,但是此时是在本机执行查询
            tx.success();     //TODO:事务的提交位置暂定此处
            Map<String, Object> tmpRes = null;
            while(result.hasNext()){
                tmpRes = result.next();
                String cls = tmpRes.get("cls").toString();
                if(cls.equals("null"))
                    continue;
                if(!classWithRels.containsKey(cls)){  //保证了第1个键的值不会被延迟初始化,避免"先检查后执行"竞态条件发生
                    classWithRels.put(cls,new ConcurrentHashMap<>());
                }
                if(tmpRes.get("anoCls") == null || tmpRes.get("anoCls").toString().equals("null")){
                    continue;
                }
                String anoCls = tmpRes.get("anoCls").toString();
                classWithRels.get(cls).put(anoCls,Integer.valueOf(tmpRes.get("tag").toString()));
            }
        }
    }

    /**
     * 判断某个类是否早已被写入知识库
     * @param preLabel &nbsp 唯一标识该类的preLabel(这只是目前的评价指标)
     * @return true表示该类已存在于知识库中,false表示该类未存在于知识库中
     */
    public static boolean classContained(String preLabel){
        return classWithRels.containsKey(preLabel);
    }

    /**
     * 往缓存中写入新类,缓存写入紧接着知识库写入并且一定要在知识库写入之后(禁止指令重排序)
     * @param preLabel &nbsp 唯一标识该类的preLabel
     */
    public static void addClass(String preLabel){
        classWithRels.put(preLabel,new ConcurrentHashMap<>());
    }

    /**
     * 判断某两个类之间的关系(这里的关系是具有先序性的)是否早已存在于知识库中
     * @param fPre 先序类
     * @param lPre 后序类
     * @return true 如果两个类之间的关系已被写入知识库
     * 注:此方法调用前必须先调用两次isClassContained方法并确定两个类都已存在在知识库中
     */
    public static boolean relExisted(String fPre,String lPre){
        return classWithRels.get(fPre).get(lPre) != null;
    }

    /**
     * 将一个新加入知识库的关系写入缓存
     * @param fPre 先序类
     * @param lPre 后序类
     * @param tag 关系种类标签
     * 注：在调用relExisted方法并确定关系不在知识库中后,将关系写入知识库然后才能调用该方法写缓存
     */
    public static void addRelation(String fPre,String lPre,int tag){
        classWithRels.get(fPre).put(lPre,tag);
    }

}

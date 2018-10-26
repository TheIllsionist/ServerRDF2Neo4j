package Importer.ApiImpl;

import Cache.InstanceCache;
import GraphInstance.SingleGraphInstance;
import Importer.InsImporter;
import annotation.GuardedBy;
import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.neo4j.graphdb.*;
import util.UriUtil;
import util.Words;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by The Illsionist on 2018/10/22.
 */
public class ApiInsImporter implements InsImporter{

    private static GraphDatabaseService graphDb = SingleGraphInstance.getInstance();  //注意一个数据库同时只能存在一个实例
    private final static int DEFAULT_RELCOUNT = 6000;  //TODO:默认初始容量的选择还有待调研
    private final static ReentrantReadWriteLock relLock = new ReentrantReadWriteLock();  //实例关系缓存的读写锁
    @GuardedBy("relLock") private final static HashMap<String,HashMap<String,HashSet<String>>> insRels = new HashMap<>();

    static {
        try(Transaction tx = graphDb.beginTx()){
            Result result = graphDb.execute("match(obj:OWL_OBJECTPROPERTY),(ins1:OWL_NAMEDINDIVIDUAL)-[r]->(ins2:OWL_NAMEDINDIVIDUAL) " +
                    " where r.preLabel = obj.preLabel return ins1.preLabel as fPre,r.preLabel as rel,ins2.preLabel as lPre ");
            tx.success();  //TODO:提交事务的位置暂定此处
            Map<String,Object> tmpRes = null;
            while(result.hasNext()){
                tmpRes = result.next();
                String fPre = tmpRes.get("fPre").toString();
                String rel = tmpRes.get("rel").toString();
                String lPre = tmpRes.get("lPre").toString();
                if(fPre.equals("null") || lPre.equals("null"))
                    continue;
                if(!insRels.containsKey(fPre)){
                    insRels.put(fPre,new HashMap<>());
                }
                if(!insRels.get(fPre).containsKey(lPre)){
                    insRels.get(fPre).put(lPre,new HashSet<>());
                }
                if(!insRels.get(fPre).get(lPre).contains(rel)){
                    insRels.get(fPre).get(lPre).add(rel);
                }
            }
        }
    }

    public static enum RelTypes implements RelationshipType
    {
        RDF_TYPE,
        RDFS_LABEL,
        RDFS_COMMENT,
        OWL_SAME_AS,   //相等实例
        OWL_DFINS     //不等实例
    }

    @Override
    public boolean loadInsIn(Individual individual) throws Exception {
        String preLabel = UriUtil.getPreLabel(individual.getURI());
        if(!InstanceCache.insContained(preLabel)){
            try(Transaction tx = graphDb.beginTx()){
                Node insWord = graphDb.findNode(Label.label("OWL_WORD"),"preLabel","owl:NamedIndividual");
                Node insNode = graphDb.createNode();
                insNode.addLabel(Label.label("OWL_NAMEDINDIVIDUAL"));
                insNode.setProperty("uri",individual.getURI());
                insNode.setProperty("preLabel",UriUtil.getPreLabel(individual.getURI()));
                Relationship rel = insNode.createRelationshipTo(insWord,RelTypes.RDF_TYPE);
                rel.setProperty("uri", RDF.type.getURI());
                rel.setProperty("preLabel",UriUtil.getPreLabel(RDF.type.getURI()));
                Iterator<RDFNode> labelNodes = individual.listLabels(null);  //该实例的所有rdfs:labels
                while(labelNodes.hasNext()){
                    Node labelNode = graphDb.createNode();
                    labelNode.setProperty("value",labelNodes.next().toString());
                    Relationship tmpRel = insNode.createRelationshipTo(labelNode, RelTypes.RDFS_LABEL);
                    tmpRel.setProperty("uri", RDFS.label.getURI());
                    tmpRel.setProperty("preLabel",UriUtil.getPreLabel(RDFS.label.getURI()));
                }
                Iterator<RDFNode> commentNodes = individual.listComments(null);  //该实例的所有rdfs:comments
                while(commentNodes.hasNext()){
                    Node commentNode = graphDb.createNode();
                    commentNode.setProperty("value",commentNodes.next().toString());
                    Relationship tmpRel = insNode.createRelationshipTo(commentNode,RelTypes.RDFS_COMMENT);
                    tmpRel.setProperty("uri",RDFS.comment.getURI());
                    tmpRel.setProperty("preLabel",UriUtil.getPreLabel(RDFS.comment.getURI()));
                }
                //处理该实例的所有数据类型属性
                StmtIterator stmtIter = individual.listProperties();
                while(stmtIter.hasNext()){
                    Statement statement = stmtIter.nextStatement();
                    Property prop = statement.getPredicate();    //得到当前属性
                    if(prop.hasProperty(RDF.type, OWL.DatatypeProperty)){   //当前属性是数据类型属性,所以处理(对象属性作为关系处理)
                        DatatypeProperty oDp = individual.getOntModel().getDatatypeProperty(prop.getURI());
                        //提取并设置第三元
                        StmtIterator dpValIter = individual.listProperties(oDp);
                        List<String> dpVals = new ArrayList<>();    //记录当前数据类型属性的取值集合
                        while(dpValIter.hasNext()){
                            dpVals.add(dpValIter.nextStatement().getLiteral().getString());
                        }
                        if(dpVals.size() == 0)  //当前数据类型属性没有取值,跳过
                            continue;
                        Node dpValNode = graphDb.createNode();
                        dpValNode.addLabel(Label.label("DP_VALUE"));
                        dpValNode.setProperty("value",listToArray(dpVals));
                        Relationship tmpRel = insNode.createRelationshipTo(dpValNode, new RelationshipType() {
                            @Override
                            public String name() {
                                return UriUtil.getPreLabel(oDp.getURI());
                            }
                        });
                        tmpRel.setProperty("uri",oDp.getURI());
                        tmpRel.setProperty("preLabel",UriUtil.getPreLabel(oDp.getURI()));
                        List<String> dpLabels = new ArrayList<>();  //当前数据类型属性的rdfs:label取值集合
                        Iterator<RDFNode> dpLabelIter = oDp.listLabels(null);
                        while(dpLabelIter.hasNext()){
                            dpLabels.add(dpLabelIter.next().toString());
                        }
                        if(dpLabels.size() > 0){  //如果该数据类型属性有rdfs:label集合
                            tmpRel.setProperty("`rdfs:label`",listToArray(dpLabels));
                        }
                    }
                }
                //处理实例所属的类
                ExtendedIterator<OntClass> clses = individual.listOntClasses(true);
                while(clses.hasNext()){
                    OntClass tCls = null;
                    try{
                        tCls = clses.next();
                    }catch (ConversionException e){
                        continue;
                    }
                    Node clsNode = graphDb.findNode(Label.label("OWL_CLASS"),"preLabel",UriUtil.getPreLabel(tCls.getURI()));
                    Relationship tmpRel = insNode.createRelationshipTo(clsNode,RelTypes.RDF_TYPE);
                    tmpRel.setProperty("uri",RDF.type.getURI());
                    tmpRel.setProperty("preLabel",UriUtil.getPreLabel(RDF.type.getURI()));
                }
                tx.success();
            }
            InstanceCache.addIndividual(preLabel); //写实例缓存
        }
        return true;
    }

    /**
     * 判断知识库中是否存在某个指定的关系
     * 必须先确定两个实例都已存在于知识库中并且关系缓存中有了该关系的两方,才能调用该方法
     * TODO:该方法目前只在loadInsRelIn方法中调用
     * @param fPre
     * @param lPre
     * @param rel
     * @return
     */
    public static boolean relExisted(String fPre,String lPre,String rel){
        relLock.readLock().lock();  //获得读锁
        try{
            return insRels.get(fPre).get(lPre).contains(rel);
        }finally {
            relLock.readLock().unlock();  //释放读锁
        }
    }

    /**
     * 将两个实例之间的对象属性关系写入知识库和缓存
     * 多线程写实例间关系,由于方法中存在“先检查-后执行”竞态条件,因此必须要保证每个线程所写入的实例关系集间互不相交才可保证不重复写
     * @param ins1
     * @param ins2
     * @param property
     * @return
     * @throws Exception
     */
    @Override
    public boolean loadInsRelIn(Individual ins1, Individual ins2, ObjectProperty property) throws Exception {
        String pre1 = UriUtil.getPreLabel(ins1.getURI());
        String pre2 = UriUtil.getPreLabel(ins2.getURI());
        String rel = UriUtil.getPreLabel(property.getURI());
        if(!InstanceCache.insContained(pre1) || !InstanceCache.insContained(pre2)){
            return false;
        }
        relLock.writeLock().lock();    //获得写锁
        try {
            if(!insRels.containsKey(pre1)){
                insRels.put(pre1,new HashMap<>());
            }
            if(!insRels.get(pre1).containsKey(pre2)){
                insRels.get(pre1).put(pre2,new HashSet<>());
            }
        }finally {
            relLock.writeLock().unlock();  //释放写锁
        }
        if(relExisted(pre1,pre2,rel))
            return true;
        //TODO:关系写库
        return writeRelIn(pre1,pre2,null,property);
    }

    /**
     * 将两个实例之间的语义关系写入知识库和缓存
     * 多线程写实例间关系,由于方法中存在“先检查-后执行”竞态条件,因此必须要保证每个线程所写入的实例关系集间互不相交才可保证不重复写
     * @param ins1
     * @param ins2
     * @param rel
     * @return
     * @throws Exception
     */
    @Override
    public boolean loadInsRelIn(Individual ins1, Individual ins2, Words rel) throws Exception {
        String pre1 = UriUtil.getPreLabel(ins1.getURI());
        String pre2 = UriUtil.getPreLabel(ins2.getURI());
        String uriRel = rel == Words.OWL_SAME_AS ? UriUtil.getPreLabel(OWL.sameAs.getURI()) : UriUtil.getPreLabel(OWL.differentFrom.getURI());
        if(!InstanceCache.insContained(pre1) || !InstanceCache.insContained(pre2)){
            return false;
        }
        relLock.writeLock().lock();
        try{
            if(!insRels.containsKey(pre1)){
                insRels.put(pre1,new HashMap<>());
            }
            if(!insRels.get(pre1).containsKey(pre2)){
                insRels.get(pre1).put(pre2,new HashSet<>());
            }
        }finally {
            relLock.writeLock().unlock();
        }
        if(relExisted(pre1,pre2,uriRel)){
            return true;
        }
        //TODO:关系写库
        return writeRelIn(pre1,pre2,rel,null);
    }


    /**
     * 多线程执行,将实例间关系写入知识库和缓存
     * @param pre1
     * @param pre2
     * @param rel
     * @return
     */
    private boolean writeRelIn(String pre1,String pre2,Words rel,ObjectProperty property){
        String relPreLabel = null;
        try(Transaction tx = graphDb.beginTx()) {
            Node insNode1 = graphDb.findNode(Label.label("OWL_NAMEDINDIVIDUAL"),"preLabel",pre1);
            Node insNode2 = graphDb.findNode(Label.label("OWL_NAMEDINDIVIDUAL"),"preLabel",pre2);
            Relationship relationship = null;
            if(property == null){
                switch (rel){
                    case OWL_SAME_AS:{
                        relationship = insNode1.createRelationshipTo(insNode2, RelTypes.OWL_SAME_AS);
                        relationship.setProperty("uri",OWL.sameAs.getURI());
                        relationship.setProperty("preLabel",UriUtil.getPreLabel(OWL.sameAs.getURI()));
                        relPreLabel = UriUtil.getPreLabel(OWL.sameAs.getURI());
                    }break;
                    case OWL_DFINS:{
                        relationship = insNode1.createRelationshipTo(insNode2, RelTypes.OWL_DFINS);
                        relationship.setProperty("uri",OWL.differentFrom.getURI());
                        relationship.setProperty("preLabel",UriUtil.getPreLabel(OWL.differentFrom.getURI()));
                        relPreLabel = UriUtil.getPreLabel(OWL.differentFrom.getURI());
                    }break;
                }
            }else{
                relationship = insNode1.createRelationshipTo(insNode2, new RelationshipType() {
                    @Override
                    public String name() {
                        return UriUtil.getPreLabel(property.getURI());
                    }
                });
                relationship.setProperty("uri",property.getURI());
                relationship.setProperty("preLabel",UriUtil.getPreLabel(property.getURI()));
                Iterator<RDFNode> opLabelIter = property.listLabels(null);  //列出该对象属性所有的可读名
                List<String> opLabels = new ArrayList<>();
                while(opLabelIter.hasNext()){
                    opLabels.add(opLabelIter.next().toString());
                }
                if(opLabels.size() > 0){
                    relationship.setProperty("`rdfs:label`",listToArray(opLabels));
                }
                relPreLabel = UriUtil.getPreLabel(property.getURI());
            }
            tx.success();
        }
        relLock.writeLock().lock();
        try{
            insRels.get(pre1).get(pre2).add(relPreLabel);
        }finally {
            relLock.writeLock().unlock();
        }
        return true;
    }

    private String[] listToArray(List<String> list){
        if(list == null)
            return null;
        String[] strs = new String[list.size()];
        int i = 0;
        for (String str : list) {
            strs[i++] = str;
        }
        return strs;
    }

}

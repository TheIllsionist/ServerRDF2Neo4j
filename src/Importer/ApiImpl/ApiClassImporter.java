package Importer.ApiImpl;

import Cache.ClassCache;
import Importer.ClassImporter;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.neo4j.graphdb.*;
import util.UriUtil;
import util.Words;
import java.util.Iterator;

/**
 * Created by The Illsionist on 2018/10/20.
 */
public class ApiClassImporter implements ClassImporter {

    private GraphDatabaseService graphDb = null;  //注意一个数据库同时只能存在一个实例

    public static enum RelTypes implements RelationshipType
    {
        RDF_TYPE,
        RDFS_LABEL,
        RDFS_COMMENT,
        RDFS_SUBCLASSOF,
        OWL_EQCLASS,
        OWL_DJCLASS
    }

    public ApiClassImporter(GraphDatabaseService graphDb){
        this.graphDb = graphDb;
    }

    /**
     * 将本体类导入Neo4j数据库
     * 多线程将类写入数据库和缓存,由于方法中存在“先检查-后执行”竞态条件,因此必须要保证每个线程所写入的类集间互不相交才可以保证不重复写
     * @param ontClass
     * @return
     * @throws Exception
     */
    @Override
    public boolean loadClassIn(OntClass ontClass) throws Exception {
        String preLabel = UriUtil.getPreLabel(ontClass.getURI());
        if(!ClassCache.classContained(preLabel)){   //该类还未导入数据库,注意存在竞态条件
            try(Transaction tx = graphDb.beginTx())
            {
                Node clsWord = graphDb.findNode(Label.label("OWL_WORD"),"preLabel","owl:Class");  //查询词汇定义Node
                Node clsNode = graphDb.createNode();    //创建类Node
                clsNode.addLabel(Label.label("OWL_CLASS"));
                clsNode.setProperty("uri",ontClass.getURI());
                clsNode.setProperty("preLabel",preLabel);
                Relationship rel = clsNode.createRelationshipTo(clsWord,RelTypes.RDF_TYPE); //定义该Node是类Node
                rel.setProperty("uri", RDF.type.getURI());
                rel.setProperty("preLabel", UriUtil.getPreLabel(RDF.type.getURI()));
                Iterator<RDFNode> labelNodes = ontClass.listLabels(null);  //列出该类的所有rdfs:label
                while(labelNodes.hasNext()){
                    Node labelNode = graphDb.createNode();
                    labelNode.setProperty("value",labelNodes.next().toString());
                    Relationship tmpRel = clsNode.createRelationshipTo(labelNode,RelTypes.RDFS_LABEL);
                    tmpRel.setProperty("uri", RDFS.label.getURI());
                    tmpRel.setProperty("preLabel",UriUtil.getPreLabel(RDFS.label.getURI()));
                }
                Iterator<RDFNode> commentNodes = ontClass.listLabels(null);  //列出该类的所有rdfs:comment
                while(commentNodes.hasNext()){
                    Node commentNode = graphDb.createNode();
                    commentNode.setProperty("value",commentNodes.next().toString());
                    Relationship tmpRel = clsNode.createRelationshipTo(commentNode,RelTypes.RDFS_COMMENT);
                    tmpRel.setProperty("uri",RDFS.comment.getURI());
                    tmpRel.setProperty("preLabel",UriUtil.getPreLabel(RDFS.comment.getURI()));
                }
                tx.success();  //提交事务
            }
            ClassCache.addClass(preLabel);
        }
        return true;
    }

    /**
     * 将两个本体类之间的关系导入Neo4j数据库
     * 多线程将关系写知识库和写缓存,由于方法中存在“先检查-后执行”竞态条件,因此必须要保证每个线程所写入的类关系集间互不相交才可保证不重复写
     * @param class1
     * @param class2
     * @param rel
     * @return
     * @throws Exception
     */
    @Override
    public boolean loadClassRelIn(OntClass class1, OntClass class2, Words rel) throws Exception {
        String fPre = UriUtil.getPreLabel(class1.getURI());
        String lPre = UriUtil.getPreLabel(class2.getURI());
        //写关系的两个类必须要先存在与知识库中
        if(!ClassCache.classContained(fPre) || !ClassCache.classContained(lPre))
            return false;
        //类已存在,但关系不存在,则先写数据库再写缓存
        if(!ClassCache.relExisted(fPre,lPre)){
            int tag = -1;
            try(Transaction tx = graphDb.beginTx()){
                Node clsNode1 = graphDb.findNode(Label.label("OWL_CLASS"),"preLabel",fPre); //查找类Node1
                Node clsNode2 = graphDb.findNode(Label.label("OWL_CLASS"),"preLabel",lPre); //查找类Node2
                Relationship relship = null;
                switch (rel){
                    case RDFS_SUBCLASSOF:{
                        relship = clsNode1.createRelationshipTo(clsNode2,RelTypes.RDFS_SUBCLASSOF);
                        relship.setProperty("uri",RDFS.subClassOf.getURI());
                        relship.setProperty("preLabel",UriUtil.getPreLabel(RDFS.subClassOf.getURI()));
                        tag = 1;
                    }break;
                    case OWL_EQCLASS:{
                        relship = clsNode1.createRelationshipTo(clsNode2,RelTypes.OWL_EQCLASS);
                        relship.setProperty("uri", OWL.equivalentClass.getURI());
                        relship.setProperty("preLabel", UriUtil.getPreLabel(OWL.equivalentClass.getURI()));
                        tag = 2;
                    }break;
                    case OWL_DJCLASS:{
                        relship = clsNode1.createRelationshipTo(clsNode2,RelTypes.OWL_DJCLASS);
                        relship.setProperty("uri",OWL.disjointWith.getURI());
                        relship.setProperty("preLabel",UriUtil.getPreLabel(OWL.disjointWith.getURI()));
                        tag = 3;
                    }break;
                }
                tx.success();  //提交事务
            }
            ClassCache.addRelation(fPre,lPre,tag);
        }
        return true;
    }

}

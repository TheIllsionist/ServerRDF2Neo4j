package Importer.ApiImpl;

import Cache.PropertyCache;
import Importer.PropertyImporter;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.neo4j.graphdb.*;
import util.UriUtil;
import util.Words;
import java.util.Iterator;

public class ApiPropertyImporter implements PropertyImporter {

    private GraphDatabaseService graphDb = null;  //注意一个数据库同时只能存在一个实例

    public static enum RelTypes implements RelationshipType
    {
        RDF_TYPE,
        RDFS_LABEL,
        RDFS_COMMENT,
        RDFS_SUBPROPERTYOF,
        RDFS_DOMAIN,
        RDFS_RANGE,
        OWL_EQPROPERTY,
        OWL_DJPROPERTY,
        OWL_IVPROPERTY
    }

    public ApiPropertyImporter(GraphDatabaseService graphDb){
        this.graphDb = graphDb;
    }

    /**
     * 将本体属性导入Neo4j数据库
     * 多线程读/写知识库和属性缓存,由于方法中存在“先检查-后执行”竞态条件,因此必须要保证每个线程所写入的属性集间互不相交才可保证不重复写
     * @param property
     * @return
     * @throws Exception
     */
    @Override
    public boolean loadPropertyIn(OntProperty property) throws Exception {
        String preLabel = UriUtil.getPreLabel(property.getURI());
        boolean isObj = property.isObjectProperty();
        if(!PropertyCache.isPropertyContained(preLabel)){
            try(Transaction tx = graphDb.beginTx()){
                Node propWord = isObj ? graphDb.findNode(Label.label("OWL_WORD"),"preLabel","owl:ObjectProperty")
                        : graphDb.findNode(Label.label("OWL_WORD"),"preLabel","owl:DatatypeProperty");
                Node propNode = graphDb.createNode();
                if(isObj) propNode.addLabel(Label.label("OWL_OBJECTPROPERTY"));
                else propNode.addLabel(Label.label("OWL_DATATYPEPROPERTY"));
                propNode.setProperty("uri",property.getURI());
                propNode.setProperty("preLabel",UriUtil.getPreLabel(property.getURI()));
                Relationship rel = propNode.createRelationshipTo(propWord,RelTypes.RDF_TYPE);
                rel.setProperty("uri", RDF.type.getURI());
                rel.setProperty("preLabel",UriUtil.getPreLabel(RDF.type.getURI()));
                Iterator<RDFNode> labels = property.listLabels(null);  //列出该属性的所有rdfs:label
                while(labels.hasNext()){
                    Node labelNode = graphDb.createNode();
                    labelNode.setProperty("value",labels.next().toString());
                    Relationship tmpRel = propNode.createRelationshipTo(labelNode,RelTypes.RDFS_LABEL);
                    tmpRel.setProperty("uri", RDFS.label.getURI());
                    tmpRel.setProperty("preLabel",UriUtil.getPreLabel(RDFS.label.getURI()));
                }
                Iterator<RDFNode> comments = property.listLabels(null);  //列出该属性的所有rdfs:comment
                while(comments.hasNext()){
                    Node commentNode = graphDb.createNode();
                    commentNode.setProperty("value",comments.next().toString());
                    Relationship tmpRel = propNode.createRelationshipTo(commentNode,RelTypes.RDFS_COMMENT);
                    tmpRel.setProperty("uri",RDFS.comment.getURI());
                    tmpRel.setProperty("preLabel",UriUtil.getPreLabel(RDFS.comment.getURI()));
                }
                ExtendedIterator<OntClass> domains = (ExtendedIterator<OntClass>) property.listDomain();//列出该属性的所有Domain
                while(domains.hasNext()){
                    OntClass tDom = domains.next();
                    Node dNode = graphDb.findNode(Label.label("OWL_CLASS"),"preLabel",UriUtil.getPreLabel(tDom.getURI()));
                    Relationship tmpRel = propNode.createRelationshipTo(dNode,RelTypes.RDFS_DOMAIN);
                    tmpRel.setProperty("uri",RDFS.domain.getURI());
                    tmpRel.setProperty("preLabel",UriUtil.getPreLabel(RDFS.domain.getURI()));
                }
                if(isObj){
                    ExtendedIterator<OntClass> ranges = (ExtendedIterator<OntClass>) property.listRange();//列出该属性的所有Range
                    while(ranges.hasNext()){
                        OntClass tRge = ranges.next();
                        Node rNode = graphDb.findNode(Label.label("OWL_CLASS"),"preLabel",UriUtil.getPreLabel(tRge.getURI()));
                        Relationship tmpRel = propNode.createRelationshipTo(rNode,RelTypes.RDFS_RANGE);
                        tmpRel.setProperty("uri",RDFS.range.getURI());
                        tmpRel.setProperty("preLabel",UriUtil.getPreLabel(RDFS.range.getURI()));
                    }
                }
                tx.success();  //提交事务
            }
            PropertyCache.addProperty(preLabel);
        }
        return true;
    }

    @Override
    public boolean loadPropertyRelIn(OntProperty prop1, OntProperty prop2, Words rel) throws Exception {
        String fPre = UriUtil.getPreLabel(prop1.getURI());
        String lPre = UriUtil.getPreLabel(prop2.getURI());
        boolean isObj = prop1.isObjectProperty();
        //写关系的两个属性必须要先存在于知识库中
        if(!PropertyCache.isPropertyContained(fPre) || !PropertyCache.isPropertyContained(lPre))
            return false;
        //如果关系不存在,则写知识库然后写缓存
        if(!PropertyCache.isRelExisted(fPre,lPre)){
            int tag = -1;
            try(Transaction tx = graphDb.beginTx()){
                Node propNode1 = graphDb.findNode(isObj?Label.label("OWL_OBJECTPROPERTY"):Label.label("OWL_DATATYPEPROPERTY"),"preLabel",UriUtil.getPreLabel(prop1.getURI()));
                Node propNode2 = graphDb.findNode(isObj?Label.label("OWL_OBJECTPROPERTY"):Label.label("OWL_DATATYPEPROPERTY"),"preLabel",UriUtil.getPreLabel(prop2.getURI()));
                Relationship relation = null;
                switch (rel){
                    case RDFS_SUBPROPERTYOF:{
                        relation = propNode1.createRelationshipTo(propNode2,RelTypes.RDFS_SUBPROPERTYOF);
                        relation.setProperty("uri",RDFS.subPropertyOf.getURI());
                        relation.setProperty("preLabel",UriUtil.getPreLabel(RDFS.subPropertyOf.getURI()));
                        tag = 1;
                    }break;
                    case OWL_EQPROPERTY:{
                        relation = propNode1.createRelationshipTo(propNode2,RelTypes.OWL_EQPROPERTY);
                        relation.setProperty("uri", OWL.equivalentProperty.getURI());
                        relation.setProperty("preLabel",UriUtil.getPreLabel(OWL.equivalentProperty.getURI()));
                        tag = 2;
                    }break;
                    case OWL_DJPROPERTY:{
                        relation = propNode1.createRelationshipTo(propNode2,RelTypes.OWL_DJPROPERTY);
                        relation.setProperty("uri",OWL.disjointWith.getURI());
                        relation.setProperty("preLabel",UriUtil.getPreLabel(OWL.disjointWith.getURI()));
                        tag = 3;
                    }break;
                    case OWL_IVPROPERTY:{
                        relation = propNode1.createRelationshipTo(propNode2,RelTypes.OWL_IVPROPERTY);
                        relation.setProperty("uri",OWL.inverseOf.getURI());
                        relation.setProperty("preLabel",UriUtil.getPreLabel(OWL.inverseOf.getURI()));
                        tag = 4;
                    }break;
                }
                tx.success();  //提交事务
            }
            PropertyCache.addRelation(fPre,lPre,tag); //写缓存
        }
        //无论是已经存在还是已经写入,返回true
        return true;
    }
}

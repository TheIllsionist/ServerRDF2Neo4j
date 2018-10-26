package test;

import GraphInstance.SingleGraphInstance;
import Importer.ApiImpl.ApiClassImporter;
import Importer.ApiImpl.ApiInsImporter;
import Importer.ApiImpl.ApiPropertyImporter;
import Importer.ClassImporter;
import Importer.InsImporter;
import Importer.PropertyImporter;
import Model.Relation;
import TaskThread.*;
import datasource.RdfProvider;
import datasource.impl.FileRdfProvider;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntProperty;
import util.Words;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by The Illsionist on 2018/10/23.
 */
public class FullTest {
    public static void main(String args[]){
        RdfProvider rdfProvider = new FileRdfProvider("/home/awakedreaming/Documents/");
        int tCount = 1;
        ClassImporter clsIpt = new ApiClassImporter(SingleGraphInstance.getInstance());
        Queue<OntClass> classes = rdfProvider.allOntClasses();
        ImportClassThread clsIn = new ImportClassThread(classes,clsIpt);
        new Thread(clsIn).run();   //要先把所有类导入才可以接着导入其他资源和关系
        //导入类间关系
        Queue<Relation<OntClass, Words>> clsRels = rdfProvider.allClassRels();
        ImportClassRelThread clsRelIn = new ImportClassRelThread(clsRels,clsIpt);
        new Thread(clsRelIn).start();
        tCount++;
        System.out.println("启动线程 " + tCount + " , 导入所有类间关系");
        //导入属性及属性间关系
        PropertyImporter propIpt = new ApiPropertyImporter(SingleGraphInstance.getInstance());
        Queue<OntProperty> props = rdfProvider.allOntProperties();
        Queue<Relation<OntProperty,Words>> propRels = rdfProvider.allPropertyRels();
        ImportPropThread propIn = new ImportPropThread(props,propIpt);
        ImportPropRelThread propRelIn = new ImportPropRelThread(propRels,propIpt);
        new Thread(propIn).start();
        tCount++;
        System.out.println("启动线程 " + tCount + " , 导入所有的属性");
        new Thread(propRelIn).start();
        tCount++;
        System.out.println("启动线程 " + tCount + " , 导入所有的属性关系");
        //导入实例及实例间关系
        InsImporter insIpt = new ApiInsImporter();
        Queue<Individual> inses = rdfProvider.allIndividuals();
        int insCount = 300;
        while(true){
            Queue<Individual> tmp = new LinkedList<>();
            while(!inses.isEmpty() && insCount >= 0){
                tmp.offer(inses.poll());
                insCount--;
            }
            new Thread(new ImportInsThread(tmp,insIpt)).start();
        }
        tCount++;
        System.out.println("启动线程 " + );
    }
}

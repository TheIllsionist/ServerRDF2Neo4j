package test;


import Importer.ApiImpl.ApiInsImporter;
import Importer.InsImporter;
import Model.Relation;
import TaskThread.ImportInsRelThread;
import TaskThread.ImportInsThread;
import datasource.RdfProvider;
import datasource.impl.FileRdfProvider;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.ObjectProperty;
import util.Words;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by The Illsionist on 2018/10/16.
 */
public class InsImportTest {
    public static void main(String args[]){
        RdfProvider rdfProvider = new FileRdfProvider("/home/awakedreaming/Documents/");
        InsImporter importer = new ApiInsImporter();
        Queue<Individual> inses = rdfProvider.allIndividuals();
        Queue<Relation<Individual,ObjectProperty>> insObjRels = rdfProvider.relsBetweenIndividuals();
        Queue<Relation<Individual, Words>> insWordRels = rdfProvider.relsBetweenIndividuals(Words.OWL_SAME_AS);
        insWordRels.addAll(rdfProvider.relsBetweenIndividuals(Words.OWL_DFINS));
        int groupCount = 300;
        int tCount = 0;
        while(true){
            Queue<Individual> tmp = new LinkedList<>();
            while(!inses.isEmpty() && groupCount >= 0){
                tmp.offer(inses.poll());
                groupCount--;
            }
            new Thread(new ImportInsThread(tmp,importer)).start();  //启动一个线程
            System.out.println("线程数: " + tCount++);
            if(inses.isEmpty())
                break;
            groupCount = 300;
        }
        new Thread(new ImportInsRelThread(insObjRels,importer)).start();  //导入实例间关系
    }
}

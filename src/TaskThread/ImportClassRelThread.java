package TaskThread;

import Importer.ClassImporter;
import Model.Relation;
import org.apache.jena.ontology.OntClass;
import util.Words;

import java.util.Queue;

/**
 * Created by The Illsionist on 2018/8/16.
 */
public class ImportClassRelThread implements Runnable {

    private final Queue<Relation<OntClass, Words>> rels;  //一批类关系导入任务
    private final ClassImporter importer;  //导入器

    public ImportClassRelThread(Queue<Relation<OntClass, Words>> rels, ClassImporter importer){
        this.rels = rels;
        this.importer = importer;
    }

    @Override
    public void run() {
        try{
            Relation<OntClass, Words> rel = null;
            while(!rels.isEmpty()){
                rel = rels.poll();
                if(!importer.loadClassRelIn(rel.getFirst(),rel.getSecond(),rel.getRel())){
                    rels.offer(rel);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            System.exit(-1);
        }
    }

}

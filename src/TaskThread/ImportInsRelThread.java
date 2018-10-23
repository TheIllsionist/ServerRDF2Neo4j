package TaskThread;

import Importer.InsImporter;
import Model.Relation;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.ObjectProperty;
import java.util.Queue;

/**
 * Created by The Illsionist on 2018/8/17.
 */
public class ImportInsRelThread implements Runnable{

    private final Queue<Relation<Individual, ObjectProperty>> rels;  //一批实例关系导入任务
    private final InsImporter importer;  //导入器

    public ImportInsRelThread(Queue<Relation<Individual, ObjectProperty>> rels, InsImporter importer){
        this.rels = rels;
        this.importer = importer;
    }

    @Override
    public void run() {
        try{
            Relation<Individual, ObjectProperty> rel = null;
            while(!rels.isEmpty()){
                rel = rels.poll();
                if(!importer.loadInsRelIn(rel.getFirst(),rel.getSecond(),rel.getRel())){
                    rels.offer(rel);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            System.exit(-1);
        }
    }

}

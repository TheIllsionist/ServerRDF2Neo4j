package TaskThread;

import Importer.PropertyImporter;
import Model.Relation;
import org.apache.jena.ontology.OntProperty;
import util.Words;
import java.util.Queue;

/**
 * Created by The Illsionist on 2018/8/17.
 */
public class ImportPropRelThread implements Runnable{

    private final Queue<Relation<OntProperty, Words>> rels;
    private final PropertyImporter importer;

    public ImportPropRelThread(Queue<Relation<OntProperty, Words>> rels, PropertyImporter importer){
        this.rels = rels;
        this.importer = importer;
    }

    @Override
    public void run() {
        try{
            Relation<OntProperty, Words> rel = null;
            while(!rels.isEmpty()){
                rel = rels.poll();
                if(!importer.loadPropertyRelIn(rel.getFirst(),rel.getSecond(),rel.getRel())){
                    rels.offer(rel);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            System.exit(-1);
        }
    }

}

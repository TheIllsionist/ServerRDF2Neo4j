package TaskThread;

import Importer.InsImporter;
import org.apache.jena.ontology.Individual;
import java.util.Queue;

/**
 * Created by The Illsionist on 2018/8/17.
 */
public class ImportInsThread implements Runnable{

    private final Queue<Individual> individuals;  //一批实例导入任务
    private final InsImporter importer;  //导入器

    public ImportInsThread(Queue<Individual> individuals, InsImporter importer){
        this.individuals = individuals;
        this.importer = importer;
    }

    @Override
    public void run() {
        try{
            while(!individuals.isEmpty()){  //不断从队列中取出导入直到队空
                importer.loadInsIn(individuals.poll());
            }
        }catch (Exception e){
            e.printStackTrace();
            System.exit(-1);  //先退出,后续这里可能要做保持知识库和缓存一致性的处理
        }
    }
}

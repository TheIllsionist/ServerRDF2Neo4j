package TaskThread;

import Importer.PropertyImporter;
import org.apache.jena.ontology.OntProperty;
import java.util.Queue;

/**
 * Created by The Illsionist on 2018/8/17.
 */
public class ImportPropThread implements Runnable{

    private final Queue<OntProperty> props;  //一批属性导入任务
    private final PropertyImporter importer;  //导入器

    public ImportPropThread(Queue<OntProperty> props, PropertyImporter importer){
        this.props = props;
        this.importer = importer;
    }

    @Override
    public void run() {
        try{
            while(!props.isEmpty()){  //不断从队列中取出导入直到队空
                importer.loadPropertyIn(props.poll());
            }
        }catch (Exception e){
            e.printStackTrace();
            System.exit(-1);  //先退出,后续这里可能要做保持知识库和缓存一致性的处理
        }
    }
}

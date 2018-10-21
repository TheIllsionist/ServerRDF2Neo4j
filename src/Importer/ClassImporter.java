package Importer;

import org.apache.jena.ontology.OntClass;
import util.Words;

/**
 * Created by The Illsionist on 2018/10/20.
 */
public interface ClassImporter {
    /**
     * 将类导入数据库
     * @param ontClass
     * @return
     * @throws Exception
     */
    boolean loadClassIn(OntClass ontClass) throws Exception;

    /**
     * 将两个类之间的关系导入数据库
     * @param class1
     * @param class2
     * @param rel
     * @return
     * @throws Exception
     */
    boolean loadClassRelIn(OntClass class1, OntClass class2, Words rel) throws Exception;

}

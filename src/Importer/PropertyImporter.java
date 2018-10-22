package Importer;

import org.apache.jena.ontology.OntProperty;
import util.Words;

public interface PropertyImporter {

    /**
     * 将属性导入数据库
     * @param property
     * @return
     * @throws Exception
     */
    public boolean loadPropertyIn(OntProperty property) throws Exception;

    /**
     * 将两个属性之间的关系导入数据库
     * @param prop1
     * @param prop2
     * @param rel
     * @return
     * @throws Exception
     */
    public boolean loadPropertyRelIn(OntProperty prop1, OntProperty prop2, Words rel) throws Exception;

}

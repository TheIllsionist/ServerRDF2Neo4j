package Importer;

import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.ObjectProperty;
import util.Words;

/**
 * Created by The Illsionist on 2018/10/22.
 */
public interface InsImporter {
    /**
     * 将实例导入数据库
     * @param individual
     * @return
     * @throws Exception
     */
    public boolean loadInsIn(Individual individual) throws Exception;

    /**
     * 将两个实例之间的领域内关系导入数据库
     * @param ins1
     * @param ins2
     * @param property
     * @return
     * @throws Exception
     */
    public boolean loadInsRelIn(Individual ins1, Individual ins2, ObjectProperty property) throws Exception;

    /**
     * 将两个实例之间的语义关系导入数据库
     * @param ins1
     * @param ins2
     * @param rel
     * @return
     * @throws Exception
     */
    public boolean loadInsRelIn(Individual ins1, Individual ins2, Words rel) throws Exception;

}

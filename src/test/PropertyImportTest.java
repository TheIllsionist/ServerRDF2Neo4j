package test;

import GraphInstance.SingleGraphInstance;
import Importer.ApiImpl.ApiPropertyImporter;
import Importer.PropertyImporter;
import Model.Relation;
import TaskThread.ImportPropRelThread;
import TaskThread.ImportPropThread;
import datasource.RdfProvider;
import datasource.impl.FileRdfProvider;
import org.apache.jena.ontology.OntProperty;
import util.Words;
import java.util.Queue;

/**
 * Created by The Illsionist on 2018/8/15.
 */
public class PropertyImportTest {
    public static void main(String args[]) throws Exception {
        RdfProvider rdfProvider = new FileRdfProvider("G:\\");
        Queue<OntProperty> props = rdfProvider.allOntProperties();
        Queue<Relation<OntProperty,Words>> propRels = rdfProvider.allPropertyRels();
        PropertyImporter importer = new ApiPropertyImporter(SingleGraphInstance.getInstance());
        ImportPropThread propIn = new ImportPropThread(props,importer);
        ImportPropRelThread propRelIn = new ImportPropRelThread(propRels,importer);
        new Thread(propIn).start();
        new Thread(propRelIn).start();
    }
}

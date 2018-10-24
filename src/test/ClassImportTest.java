package test;

import GraphInstance.SingleGraphInstance;
import Importer.ApiImpl.ApiClassImporter;
import Importer.ClassImporter;
import Model.Relation;
import TaskThread.ImportClassRelThread;
import TaskThread.ImportClassThread;
import datasource.RdfProvider;
import datasource.impl.FileRdfProvider;
import org.apache.jena.ontology.OntClass;
import util.Words;
import java.util.Queue;

/**
 * Created by The Illsionist on 2018/7/1.
 */
public class ClassImportTest {
    public static void main(String args[]) throws Exception {
        RdfProvider rdfProvider = new FileRdfProvider("G:\\");
        ClassImporter importer = new ApiClassImporter(SingleGraphInstance.getInstance());
        Queue<OntClass> classes = rdfProvider.allOntClasses();
        ImportClassThread clsIn = new ImportClassThread(classes,importer);
        Queue<Relation<OntClass, Words>> clsRels = rdfProvider.allClassRels();
        ImportClassRelThread clsRelIn = new ImportClassRelThread(clsRels,importer);
        new Thread(clsIn).run();
        new Thread(clsRelIn).run();
    }
}

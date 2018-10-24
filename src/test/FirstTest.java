package test;

import GraphInstance.SingleGraphInstance;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

/**
 * Created by The Illsionist on 2018/10/23.
 */
public class FirstTest {
    public static void main(String args[]){
        GraphDatabaseService graphDb = SingleGraphInstance.getInstance();
        try(Transaction tx = graphDb.beginTx()) {
            graphDb.execute("create(clsWord:OWL_WORD {uri:\"http://www.w3.org/2002/07/owl#Class\",preLabel:\"owl:Class\"})," +
                    "  (dpWord:OWL_WORD {uri:\"http://www.w3.org/2002/07/owl#DatatypeProperty\",preLabel:\"owl:DatatypeProperty\"})," +
                    "  (opWord:OWL_WORD {uri:\"http://www.w3.org/2002/07/owl#ObjectProperty\",preLabel:\"owl:ObjectProperty\"})," +
                    "  (insWord:OWL_WORD {uri:\"http://www.w3.org/2002/07/owl#NamedIndividual\",preLabel:\"owl:NamedIndividual\"})");
            tx.success();
        }
    }
}

import Conceptual.Vertex;
import javafx.util.Pair;
import org.apache.commons.csv.CSVRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class RDFXMLGenerator {
    Map<String, Integer> headers;
    List<CSVRecord> csv;
    ArrayList<Vertex> classes;

    RDFXMLGenerator(Map<String, Integer> headers, List<CSVRecord> csv, ArrayList<Vertex> classes){
        this.headers = headers;
        this.csv = csv;
        this.classes = classes;
    }

    String generate() {
        return null;
    }

    /**
     * Find either classes or csv headers that do not directly correlate (namely, are equal.
     * @return the uncorrelated headers and classes as a Pair.
     */
    Pair<Map<String, Integer>, ArrayList<Vertex>> getUncorrelatedHeaders(){
        ArrayList<Vertex> uncorrelatedClasses = classes;
        Map<String, Integer> uncorrelatedHeaders = headers;

        for (Map.Entry<String, Integer> header : headers.entrySet()){
            for (Vertex klass : classes){
                if (header.getKey().equals(klass.getName())){
                    uncorrelatedHeaders.remove(header.getKey());
                    uncorrelatedClasses.remove(klass);
                    break;
                }
            }
        }

        if (uncorrelatedClasses.size() == 0 && uncorrelatedHeaders.size() == 0) return null;
        else return new Pair<>(uncorrelatedHeaders, uncorrelatedClasses);
    }
}

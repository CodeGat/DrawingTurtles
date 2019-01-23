package model.conversion.rdfxml;

import model.conceptual.Vertex;
import javafx.util.Pair;
import org.apache.commons.csv.CSVRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RDFXMLGenerator {
    private Map<String, Integer> headers;
    private List<CSVRecord> csv;
    private ArrayList<Vertex> classes;
    private Correlations csvTtlCorrelations;

    public RDFXMLGenerator(Map<String, Integer> headers, List<CSVRecord> csv, ArrayList<Vertex> classes){
        this.headers = headers;
        this.csv = csv;
        this.classes = classes;
    }

    public String generate() {
        return null;
    }

    /**
     * Find either classes or csv headers that do not directly correlate (namely, are equal.
     * @return the uncorrelated headers and classes as a Pair.
     */
    public Pair<ArrayList<String>, ArrayList<Vertex>> getUncorrelatedHeaders(){
        ArrayList<Vertex> uncorrelatedClasses = classes;
        Map<String, Integer> uncorrelatedHeaders = headers;

        for (Map.Entry<String, Integer> header : headers.entrySet()){
            for (Vertex klass : classes){
                boolean isExactMatch = header.getKey().equals(klass.getName());
                boolean isCloseMatch = header.getKey().contains(":")
                        && header.getKey().equals(klass.getName().split(":", 2)[1]);

                if (isExactMatch || isCloseMatch){
                    csvTtlCorrelations.addCorrelation(header.getValue(), header.getKey(), klass);
                    uncorrelatedHeaders.remove(header.getKey());
                    uncorrelatedClasses.remove(klass);
                    break;
                }
            }
        }

        if (uncorrelatedClasses.size() == 0 && uncorrelatedHeaders.size() == 0) return null;
        else {
            ArrayList<String> uncorrelatedHeadersList =
                    uncorrelatedHeaders
                            .entrySet()
                            .stream()
                            .map(e -> e.getValue() + " "  + e.getKey())
                            .collect(Collectors.toCollection(ArrayList::new));
            return new Pair<>(uncorrelatedHeadersList, uncorrelatedClasses);
        }
    }
}

package model.conversion.rdfxml;

import model.conceptual.Vertex;
import javafx.util.Pair;
import org.apache.commons.csv.CSVRecord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RDFXMLGenerator {
    private Map<String, Integer> headers;
    private List<CSVRecord> csv;
    private ArrayList<Vertex> classes;
    private ArrayList<Correlation> csvTtlCorrelations = new ArrayList<>();
    private Pair<ArrayList<String>, ArrayList<Vertex>> csvTtlUncorrelated;

    public RDFXMLGenerator(Map<String, Integer> headers, List<CSVRecord> csv, ArrayList<Vertex> classes){
        this.headers = headers;
        this.csv = csv;
        this.classes = classes;
    }

    public String generate() {
        String rdfxml = "";

        //do magic

        return rdfxml;
    }

    /**
     * Find either classes or csv headers that do not directly correlate (namely, are equal.
     */
    public void attemptCorrelationOfHeaders(){
        ArrayList<Vertex> uncorrelatedClasses = new ArrayList<>(classes);
        Map<String, Integer> uncorrelatedHeaders = new HashMap<>(headers);

        for (Map.Entry<String, Integer> header : headers.entrySet()){
            for (Vertex klass : classes){
                boolean isExactMatch = header.getKey().equals(klass.getName());
                boolean isCloseMatch = !klass.isIri()
                        && klass.getType() != Vertex.GraphElemType.LITERAL
                        && header.getKey().equalsIgnoreCase(klass.getName().split(":", 2)[1]);

                if (isExactMatch || isCloseMatch){
                    csvTtlCorrelations.add(new Correlation(header.getValue(), header.getKey(), klass));
                    uncorrelatedHeaders.remove(header.getKey());
                    uncorrelatedClasses.remove(klass);
                    break;
                }
            }
        }

        if (uncorrelatedHeaders.size() != 0 || uncorrelatedClasses.size() != 0) {
            ArrayList<String> uncorrelatedHeadersList = uncorrelatedHeaders
                    .entrySet()
                    .stream()
                    .map(e -> e.getValue() + " " + e.getKey())
                    .collect(Collectors.toCollection(ArrayList::new));
            csvTtlUncorrelated = new Pair<>(uncorrelatedHeadersList, uncorrelatedClasses);
        }
    }

    public ArrayList<Correlation> getCorrelations() { return csvTtlCorrelations; }

    public Pair<ArrayList<String>, ArrayList<Vertex>> getUncorrelated() { return csvTtlUncorrelated; }

    public void setCorrelations(ArrayList<Correlation> correlations) {
        this.csvTtlCorrelations = correlations;
    }

    public void setUncorrelated(Pair<ArrayList<String>, ArrayList<Vertex>> uncorrelated) {
        this.csvTtlUncorrelated = uncorrelated;
    }

    public String uncorrelatedClassesToString(){
        StringBuilder result = new StringBuilder();
        csvTtlUncorrelated.getValue().forEach(cls -> result.append(cls.getName()).append(" "));
        return result.toString();
    }

    public String uncorrelatedToString(){
        StringBuilder result = new StringBuilder("[");
        csvTtlUncorrelated.getKey().forEach(header -> result.append(header).append(" "));
        result.append("] and [");
        csvTtlUncorrelated.getValue().forEach(cls -> result.append(cls.getName()).append(" "));
        result.append("]");
        return result.toString();
    }
}

package model.conversion.rdfxml;

import model.conceptual.Edge;
import model.conceptual.Vertex;
import javafx.util.Pair;
import org.apache.commons.csv.CSVRecord;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class RDFXMLGenerator {
    public class PrefixMissingException extends Exception {
        PrefixMissingException(String msg){ super(msg); }
    }

    private Map<String, Integer> headers;
    private List<CSVRecord> csv;
    private ArrayList<Vertex> classes;
    private ArrayList<Edge> predicates;
    private Map<String, String> prefixes;
    private ArrayList<Correlation> csvTtlCorrelations = new ArrayList<>();
    private Pair<ArrayList<String>, ArrayList<Vertex>> csvTtlUncorrelated;

    public RDFXMLGenerator(
            Map<String, Integer> headers,
            List<CSVRecord> csv,
            ArrayList<Vertex> classes,
            ArrayList<Edge> predicates,
            Map<String, String> prefixes){
        this.headers = headers;
        this.csv = csv;
        this.classes = classes;
        this.prefixes = prefixes;
        this.predicates = predicates;
    }

    public String generate() {
        String rdfxml = "";
        List<Vertex> ttlClasses =
                classes
                .stream()
                .filter(c -> c.getType() == Vertex.GraphElemType.CLASS)
                .collect(Collectors.toList());

        //do magic
        for (Vertex klass : ttlClasses){
            String subject = "";
            try {
                subject = generateLongformURI(klass.getName());
            } catch (PrefixMissingException e) {
                e.printStackTrace();
            }
            System.out.println(subject);
        }

        return rdfxml;
    }

    private String generateLongformURI(String name) throws PrefixMissingException {
        String[] nameParts = name.split(":");
        String   namePrefix = nameParts[0];
        String   nameURI = nameParts[1];

        Entry<String, String> matchingPrefix =
                prefixes.entrySet()
                        .stream()
                        .filter(p -> p.getKey().equals(namePrefix))
                        .findFirst()
                        .orElse(null);

        if (matchingPrefix == null)
            throw new PrefixMissingException("Failed to find matching prefix '" + namePrefix + "' in prefixes.");
        else return "<" + matchingPrefix.getValue() + nameURI + ">";
    }

    /**
     * Find either classes or csv headers that do not directly correlate (namely, are equal.
     */
    public void attemptCorrelationOfHeaders(){
        ArrayList<Vertex> uncorrelatedClasses = new ArrayList<>(classes);
        Map<String, Integer> uncorrelatedHeaders = new HashMap<>(headers);

        for (Entry<String, Integer> header : headers.entrySet()){
            for (Vertex klass : classes){
                boolean isExactMatch = header.getKey().equals(klass.getName());
                boolean isCloseMatch = !klass.isIri()
                        && klass.getType() == Vertex.GraphElemType.CLASS
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

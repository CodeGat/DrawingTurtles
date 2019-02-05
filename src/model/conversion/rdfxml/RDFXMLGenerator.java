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
    private Map<String, String> prefixes;
    private ArrayList<Correlation> csvTtlCorrelations = new ArrayList<>();
    private Pair<ArrayList<String>, ArrayList<Vertex>> csvTtlUncorrelated;

    public RDFXMLGenerator(
            Map<String, Integer> headers,
            List<CSVRecord> csv,
            ArrayList<Vertex> classes,
            Map<String, String> prefixes){
        this.headers = headers;
        this.csv = csv;
        this.classes = classes;
        this.prefixes = prefixes;
    }

    public String generate() {
        StringBuilder rdfxml = new StringBuilder();
        csv.forEach(record -> rdfxml.append(generateRdfxmlOf(record)));

        return rdfxml.toString();
    }

    private String generateRdfxmlOf(CSVRecord record) {
        StringBuilder rdfxmlRecord = new StringBuilder();
        List<Vertex> ttlClasses =
                classes
                .stream()
                .filter(c -> c.getType() == Vertex.GraphElemType.CLASS)
                .collect(Collectors.toList());

        for (Vertex klass : ttlClasses){
            StringBuilder rdfxmlTriples = new StringBuilder();
            String rdfxmlTriple;
            final String subject = generateLongformURI(klass, record);

            for (Edge edge : klass.getOutgoingEdges()){
                String predicate = generateLongformURI(edge);
                String object    = generateLongformURI(edge.getObject(), record);

                rdfxmlTriple = subject + " " + predicate + " " + object + "\n";
                rdfxmlTriples.append(rdfxmlTriple);
            }

            rdfxmlRecord.append(rdfxmlTriples.toString());
            System.out.println(rdfxmlTriples.toString());
        }

        return rdfxmlRecord.toString();
    }

    private String generateLongformURI(Vertex klass, CSVRecord record) {
        if (klass.getType() == Vertex.GraphElemType.GLOBAL_LITERAL)
            return klass.getName();
        else if (klass.getType() == Vertex.GraphElemType.CLASS) {
            String   name = klass.getName();
            String[] nameParts = name.split(":");
            String   prefixAcronym = nameParts[0];
            String   nameURI = nameParts[1];

            String longformPrefix;
            try {
                longformPrefix = generateLongformPrefix(prefixAcronym);
            } catch (PrefixMissingException e) {
                e.printStackTrace();
                return null;
            }

            Correlation matchedCorrelation = generateLongformClass(klass);
            String longformURI;
            if (matchedCorrelation != null) longformURI = record.get(matchedCorrelation.getIndex());
            else longformURI = nameURI;

            return "<" + longformPrefix + longformURI + ">";
        } else if (klass.getType() == Vertex.GraphElemType.INSTANCE_LITERAL){
            Correlation matchedCorrelation = generateLongformClass(klass);
            if (matchedCorrelation != null)
                return "\"" + record.get(matchedCorrelation.getIndex()) + "\"";
        }
        return null;
    }

    private Correlation generateLongformClass(Vertex klass) {
        return csvTtlCorrelations
                .stream()
                .filter(c -> c.getTtlClass().getName().equals(klass.getName()))
                .findFirst()
                .orElse(null);
    }

    private String generateLongformURI(Edge edge) {
        String name = edge.getName();
        String[] nameParts = name.split(":");
        String prefixAcronym = nameParts[0];
        String nameURI = nameParts[1];

        try {
            return "<" + generateLongformPrefix(prefixAcronym) + nameURI + ">";
        } catch (PrefixMissingException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String generateLongformPrefix(String acronym) throws PrefixMissingException {
        Entry<String, String> matchingPrefix =
                prefixes.entrySet()
                        .stream()
                        .filter(p -> p.getKey().equals(acronym))
                        .findFirst()
                        .orElse(null);

        if (matchingPrefix == null)
            throw new PrefixMissingException("Failed to find matching prefix '" + acronym + "' in prefixes.");
        else return matchingPrefix.getValue();
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

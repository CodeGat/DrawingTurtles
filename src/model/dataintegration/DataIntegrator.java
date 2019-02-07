package model.dataintegration;

import model.conceptual.Edge;
import model.conceptual.Vertex;
import javafx.util.Pair;
import org.apache.commons.csv.CSVRecord;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static model.conceptual.Vertex.GraphElemType.*;

/**
 * Responsible for the generation of instance data, as well as correlating .csv headers and their .ttl counterparts.
 */
public class DataIntegrator {
    public class PrefixMissingException extends Exception {
        private String missing;

        PrefixMissingException(String msg, String missing){
            super(msg);
            this.missing = missing;
        }

        public String getMissing(){ return missing; }
    }

    private Map<String, Integer> headers;
    private List<CSVRecord> csv;
    private ArrayList<Vertex> classes;
    private Map<String, String> prefixes;
    private ArrayList<Correlation> csvTtlCorrelations = new ArrayList<>();
    private Pair<ArrayList<String>, ArrayList<Vertex>> csvTtlUncorrelated;
    private List<Vertex> ttlClasses;

    private static int blankNodePermutation = 0;

    public DataIntegrator(
            Map<String, Integer> headers,
            List<CSVRecord> csv,
            ArrayList<Vertex> classes,
            Map<String, String> prefixes){
        this.headers = headers;
        this.csv = csv;
        this.classes = classes;
        this.prefixes = prefixes;
    }

    /**
     * Accessible method for generating instance-level data given a .csv and the graph. Recordwise generation.
     * @return String representation of the instance-level data.
     */
    public String generate() throws PrefixMissingException {
        StringBuilder instanceData = new StringBuilder();
        ttlClasses = classes.stream().filter(c -> c.getElementType() == CLASS).collect(Collectors.toList());

        for (CSVRecord record : csv)
            instanceData.append(generateInstanceDataOf(record));

        return instanceData.toString();
    }

    /**
     * Constructs the instance-level data of the particular record.
     * @param record the record used to populate the resulting instance-level data.
     * @return the generated instance-level data as a String.
     */
    private String generateInstanceDataOf(CSVRecord record) throws PrefixMissingException {
        StringBuilder instanceData = new StringBuilder();

        blankNodePermutation += 1;

        for (Vertex klass : ttlClasses){
            StringBuilder instanceTriples = new StringBuilder();
            String instanceTriple;
            final String subject = generateLongformURI(klass, record);
            if (klass.getRdfsLabel() != null){
                instanceTriple =
                        subject + " <http://www.w3.org/2000/01/rdf-schema#label> \"" + klass.getRdfsLabel() + "\"\n";
                instanceTriples.append(instanceTriple);
            }
            if (klass.getRdfsComment() != null){
                instanceTriple =
                        subject + " <http://www.w3.org/2000/01/rdf-schema#comment> \"" + klass.getRdfsComment() + "\"\n";
                instanceTriples.append(instanceTriple);
            }

            for (Edge edge : klass.getOutgoingEdges()){
                String predicate = generateLongformURI(edge);
                String object    = generateLongformURI(edge.getObject(), record);

                instanceTriple = subject + " " + predicate + " " + object + "\n";
                instanceTriples.append(instanceTriple);
            }

            instanceData.append(instanceTriples.toString());
        }

        return instanceData.toString();
    }

    /**
     * Create the expansion of the graph node into a well-formed URI or Literal.
     * @param klass the Vertex to be expanded.
     * @param record the data to populate the Vertices instance-level fields.
     * @return the instance data of the given Vertex as a String.
     */
    private String generateLongformURI(Vertex klass, CSVRecord record) throws PrefixMissingException {
        if (klass.getElementType() == GLOBAL_LITERAL || klass.isIri())
            return klass.getName();
        else if (klass.isBlank())
            return klass.getName() + blankNodePermutation;
        else if (klass.getElementType() == CLASS) {
            String   name = klass.getName();
            String[] nameParts = name.split(":");
            String   prefixAcronym = nameParts[0];
            String   nameURI = nameParts[1];

            String longformPrefix = generateLongformPrefix(prefixAcronym);
            String instanceData = getInstanceLevelData(klass, record);

            if (instanceData != null)
                return "<" + longformPrefix + instanceData + ">";
            else return "<" + longformPrefix + nameURI + ">";
        } else if (klass.getElementType() == INSTANCE_LITERAL){
            String dataType = klass.getDataType() != null ? klass.getDataType() : "";

            switch (dataType){
                case "xsd:String":
                case "":
                    return "\"" + getInstanceLevelData(klass, record) + "\"";
                case "xsd:Integer":
                case "xsd:Decimal":
                case "xsd:Double":
                case "xsd:Boolean":
                    return getInstanceLevelData(klass, record);
                default:
                    return "\"" + getInstanceLevelData(klass, record) + "\"^^" + dataType;
            }
        }
        return null;
    }

    /**
     * Generate the instance-level data of a given Vertex by finding the correlation between the Vertex and the
     *    record headers.
     * @param klass the Vertex we are populating.
     * @param record the .csv record we are using to populate instance-level data.
     * @return the String representation of the instance data.
     */
    private String getInstanceLevelData(Vertex klass, CSVRecord record) {
        Correlation matchedCorrelation = csvTtlCorrelations
                .stream()
                .filter(c -> c.getTtlClass().getName().equals(klass.getName()))
                .findFirst()
                .orElse(null);

        return matchedCorrelation != null ? record.get(matchedCorrelation.getIndex()) : null;
    }

    /**
     * Create the expansion of a graph property into a well-formed URI.
     * @param edge the Edge we are expanding.
     * @return the String form of the expanded Edge.
     */
    private String generateLongformURI(Edge edge) throws PrefixMissingException {
        if (edge.isIri()){
            return edge.getName();
        } else {
            String name = edge.getName();
            String[] nameParts = name.split(":");
            String prefixAcronym = nameParts[0];
            String nameURI = nameParts[1];

            return "<" + generateLongformPrefix(prefixAcronym) + nameURI + ">";
        }
    }

    /**
     * Creates the expanded prefix URI.
     * @param acronym the short form of the prefix.
     * @return the expanded version of the associated acronym.
     * @throws PrefixMissingException if there was no such acronym defined in the 'Prefixes Menu.'
     */
    private String generateLongformPrefix(String acronym) throws PrefixMissingException {
        Entry<String, String> matchingPrefix =
                prefixes.entrySet()
                        .stream()
                        .filter(p -> p.getKey().equals(acronym))
                        .findFirst()
                        .orElse(null);

        if (matchingPrefix == null)
            throw new PrefixMissingException("Failed to find matching prefix '" + acronym + "' in prefixes.", acronym);
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
                        && klass.getElementType() == CLASS
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

    /**
     * Accessors and toStrings
     */
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

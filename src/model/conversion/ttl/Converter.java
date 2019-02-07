package model.conversion.ttl;

import model.conceptual.Edge;
import model.conceptual.Vertex;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class that is responsible for the conversion of a visual model.graph into a .ttl string.
 */
public class Converter {
    private static Map<String, String> prefixes;
    private static ArrayList<Vertex>   classes;
    private static ArrayList<Edge>     properties;
    private static ArrayList<Boolean>  config;

    private static boolean isOntology;

    private static String tabs = "\t";

    /**
     * The overarching method for conversion of a graph into a string.
     * @param prefixes the Arraylist of known prefixes.
     * @param classes the Arraylist of visual Classes and Literals.
     * @param properties the Arraylist of visual Properties.
     * @param config the options specified by the user.
     * @return a String representation of the graph as Turtle RDF syntax.
     */
    public static String convertGraphToTtlString(
            Map<String, String> prefixes,
            ArrayList<Vertex> classes,
            ArrayList<Edge> properties,
            ArrayList<Boolean> config) {
        Converter.prefixes   = prefixes;
        Converter.classes    = classes;
        Converter.properties = properties;
        Converter.config     = config;

        isOntology = config.get(2);

        String fixesNeeded = getFixes();
        String stringPrefixes = convertPrefixes();
        String stringProperties = convertGProperties();
        String stringClasses  = convertGClasses();


        return fixesNeeded + stringPrefixes + stringClasses + stringProperties;
    }

    /**
     * Get potential problems that the user may want to rectify.
     * Checks include: possible renaming of blank node names, and notifying the user of instance-level literal
     *    placeholders.
     * @return the list of fixes.
     */
    private static String getFixes() {
        StringBuilder fixString = new StringBuilder("# Potential issues found: \n");
        final int fixStringInitLength = fixString.length();

        // checking for blank node names, reminding the user to rename them from basic characters.
        if (Vertex.getBlankNodeNames().size() > 0){
            fixString.append("# Don't forget to rename generic blank node names, namely: \n# ");
            Vertex.getBlankNodeNames().forEach(n -> fixString.append(n).append(", "));
            fixString.delete(fixString.length() - 2, fixString.length());
            fixString.append(".\n");
        }

        // reminding the user that instance literals will be replaced by their corresponding instance level data when
        //    converted to RDFXML.
        if (classes.stream().anyMatch(c -> c.getElementType() == Vertex.GraphElemType.INSTANCE_LITERAL)){
            fixString.append("# The following Literals are placeholders for instance-level data that will be populate" +
                    "d during RDFXML creation: \n# ");
            classes.stream()
                    .filter(c -> c.getElementType() == Vertex.GraphElemType.INSTANCE_LITERAL)
                    .forEach(c -> fixString.append(c.getName()).append(", "));
            fixString.delete(fixString.length() - 2, fixString.length());
            fixString.append(".\n");
        }

        //
        Stream<String> ttlClassPrefixesStream = classes.stream()
                .filter(c -> c.getElementType() == Vertex.GraphElemType.CLASS && !c.isIri())
                .map(c -> c.getName().split(":")[0]);
        Stream<String> ttlPropPrefixesStream = properties.stream()
                .filter(p -> !p.isIri())
                .map(p -> p.getName().split(":")[0]);
        Set<String> ttlPrefixSet = Stream
                .concat(ttlClassPrefixesStream, ttlPropPrefixesStream)
                .collect(Collectors.toCollection(HashSet::new));
        Set<String> addedPrefixesSet = prefixes.keySet();

        if (!addedPrefixesSet.equals(ttlPrefixSet)){
            Set<String> ttlPrefixSetTmp = new HashSet<>(ttlPrefixSet);
            Set<String> addedPrefixesSetTmp = new HashSet<>(addedPrefixesSet);
            ttlPrefixSetTmp.removeAll(addedPrefixesSet);
            addedPrefixesSetTmp.removeAll(ttlPrefixSet);

            if (ttlPrefixSetTmp.size() > 0) {
                fixString.append("# The following prefixes are defined in the graph but not in the prefixes menu (RDF" +
                        "XML creation will not work):\n#   ");
                ttlPrefixSetTmp.forEach(p -> fixString.append(p).append(", "));
                fixString.delete(fixString.length() - 2, fixString.length());
                fixString.append(".\n");
            }
            if (addedPrefixesSetTmp.size() > 0){
                fixString.append("# The following prefixes are defined in the prefixes menu but remain unused in the " +
                        "graph:\n#   ");
                addedPrefixesSetTmp.forEach(p -> fixString.append(p).append(", "));
                fixString.delete(fixString.length() - 2, fixString.length());
                fixString.append(".\n");
            }
        }

        if (classes.stream().anyMatch(c -> c.getElementType() == Vertex.GraphElemType.INSTANCE_LITERAL)){
            fixString.append("# The following Literals are placeholders for instance-level data that will be populate" +
                    "d during RDFXML creation: \n# ");
            classes.stream()
                    .filter(c -> c.getElementType() == Vertex.GraphElemType.INSTANCE_LITERAL)
                    .forEach(c -> fixString.append(c.getName()).append(", "));
            fixString.delete(fixString.length() - 2, fixString.length());
            fixString.append(".\n\n");
        }

        return fixString.length() > fixStringInitLength ? fixString.toString() : "";
    }

    /**
     * Conversion of prefixes into .ttl prefixes.
     * Helper of {@link #convertGraphToTtlString(Map, ArrayList, ArrayList, ArrayList)}.
     * @return the converted prefixes.
     */
    private static String convertPrefixes() {
        StringBuilder prefixStrs = new StringBuilder();

        for (Map.Entry<String, String> prefix : prefixes.entrySet()){
            String   prefixStr = "@prefix " + prefix.getKey() + " : <" + prefix.getValue() + "> .\n";
            prefixStrs.append(prefixStr);
        }

        return prefixStrs.toString();
    }

    /**
     * Conversion of visual properties into .ttl representation.
     * Helper of {@link #convertGraphToTtlString(Map, ArrayList, ArrayList, ArrayList)}.
     * @return the properties as a valid .tll string.
     */
    private static String convertGProperties() {
        if (!isOntology) return "";

        StringBuilder propStrs = new StringBuilder(
                "\n##################################################\n" +
                "#####          Ontology Properties           #####\n" +
                "##################################################\n\n"
        );

        for (Edge property : properties){
            String propStr;
            String prop = property.getName();
            String obj = property.getObject().getName();
            String sub = property.getSubject().getName();

            String subType = null;
            String objType = null;
            String ints = "[+\\-]?\\d";

            if      (obj.matches("\".*\"")) objType = "xsd:string";
            else if (obj.matches("true|false")) objType = "xsd:boolean";
            else if (obj.matches(ints+"+")) objType = "xsd:integer";
            else if (obj.matches(ints+"*\\.\\d+")) objType = "xsd:decimal";
            else if (obj.matches("("+ints+"+\\.\\d+|[+\\-]?\\.\\d+|"+ints+")E"+ints+"+")) objType = "xsd:double";
            else if (obj.matches(".*\\^\\^.*")) objType = obj.split("\\^\\^")[1];

            if      (sub.matches("\".*\"")) subType = "xsd:string";
            else if (sub.matches("true|false")) subType = "xsd:boolean";
            else if (sub.matches(ints+"+")) subType = "xsd:integer";
            else if (sub.matches(ints+"*\\.\\d+")) subType = "xsd:decimal";
            else if (sub.matches("("+ints+"+\\.\\d+|[+\\-]?\\.\\d+|"+ints+")E"+ints+"+")) subType = "xsd:double";
            else if (sub.matches(".*\\^\\^.*")) subType = sub.split("\\^\\^")[1];

            propStr = prop + " rdf:type owl:ObjectProperty ;\n\t" +
                    "rdfs:domain " + (subType == null ? sub : subType) + " ;\n\t" +
                    "rdfs:range " + (objType == null ? obj : objType) + " .\n";
            propStrs.append(propStr);
        }
        return propStrs.toString();
    }

    /**
     * Comversion of visual Classes and Literals into their .ttl string equivalent.
     * @return the converted Classes and Literals into thier .ttl equivalent.
     */
    private static String convertGClasses() {
        StringBuilder classStrs = new StringBuilder(
                "##################################################\n" +
                "#####            Ontology Classes            #####\n" +
                "##################################################\n\n"
        );
        boolean isBlanknode;

        for (Vertex graphClass : classes) {
            isBlanknode = config.get(1) && graphClass.isBlank();

            if (graphClass.getElementType() == Vertex.GraphElemType.CLASS && !isBlanknode)
                classStrs.append(convertTriple(graphClass));
        }

        return classStrs.toString();
    }

    /**
     * Converts a triple to a String. Equivalent to https://www.w3.org/TR/turtle/#grammar-production-triples.
     * @param subject the subject of the triple.
     * @return the triple in String form.
     */
    private static String convertTriple(Vertex subject){
        String subjectString = convertSubject(subject);
        String predicateObjectString = convertPredicateObjectList(subject);

        if (predicateObjectString.length() == 0 && !isOntology && subject.getTypeDefinition() == null) {
            return "";
        } else if (predicateObjectString.length() == 0)
            subjectString = subjectString.substring(0, subjectString.length() - 4);

        return subjectString + predicateObjectString + " .\n\n";
    }

    /**
     * Creates the predicate-object list of the given subject.
     * Equivalent to https://www.w3.org/TR/turtle/#grammar-production-predicateObjectList
     * @param subject the Vertex of the predicate-object list.
     * @return the predicate-object list in String form.
     */
    private static String convertPredicateObjectList(Vertex subject) {
        StringBuilder predicateObjectListSB = new StringBuilder();
        boolean first = true;

        HashMap<String, ArrayList<Vertex>> commonObjects = new HashMap<>();
        for (Edge edge : subject.getOutgoingEdges()){
            Vertex obj = edge.getObject();
            String edgeName = edge.getName();

            if (commonObjects.containsKey(edgeName)) commonObjects.get(edgeName).add(obj);
            else commonObjects.put(edgeName, new ArrayList<>(Collections.singletonList(obj)));
        }

        if (isOntology) {
            int initPredObjListSBLength = predicateObjectListSB.length();
            predicateObjectListSB.append(getRdfsProperties(subject));
            if (predicateObjectListSB.length() > initPredObjListSBLength) first = false;
        }

        for (Map.Entry<String, ArrayList<Vertex>> e : commonObjects.entrySet()){
            String predicateObjectListStr = "";
            String propName = e.getKey();
            String objectListStr;
            ArrayList<Vertex> objectList = e.getValue();

            objectListStr = convertObjectList(objectList);

            if (first) first = false;
            else predicateObjectListStr = " ;\n" + tabs;

            predicateObjectListStr += propName + " " + objectListStr;
            predicateObjectListSB.append(predicateObjectListStr);
        }

        return predicateObjectListSB.toString();
    }

    private static String getRdfsProperties(Vertex subject) {
        String result = "";
        String rdfsLabel = subject.getRdfsLabel();
        String rdfsComment = subject.getRdfsComment();

        if (rdfsLabel != null && rdfsLabel.length() != 0 && rdfsLabel.contains("\n"))
            result += "rdfs:label \"\"\"" + rdfsLabel + "\"\"\" ;\n" + tabs;
        else if (rdfsLabel != null && rdfsLabel.length() != 0)
            result += "rdfs:label \"" + rdfsLabel + "\" ;\n" + tabs;
        if (rdfsComment != null && rdfsComment.length() != 0 && rdfsComment.contains("\n"))
            result += "rdfs:comment \"\"\"" + rdfsComment + "\"\"\" ;\n" + tabs;
        else if (rdfsComment != null && rdfsComment.length() != 0)
            result += "rdfs:comment \"" + rdfsComment + "\" ;\n" + tabs;

        return result.length() != 0 ? result.substring(0, result.length() - 4) : result;
    }

    /**
     * Creates the object list of a predicate.
     * Equivalent to https://www.w3.org/TR/turtle/#grammar-production-objectList
     * @param objectList list of Vertices to be converted.
     * @return the object list in String form.
     */
    private static String convertObjectList(ArrayList<Vertex> objectList) {
        StringBuilder objectListSB = new StringBuilder();
        boolean asCollection = config.get(0);
        boolean first = true;

        if (asCollection && objectList.size() > 1) objectListSB.append("(");
        for (Vertex object : objectList){
            String objectListStr;

            if (objectList.size() == 1 || (first && asCollection)){
                objectListSB.append(convertObject(object));
                first = false;
            } else if (asCollection){
                objectListStr = " " + convertObject(object);
                objectListSB.append(objectListStr);
            } else {
                indentTab();
                objectListStr = (!first ? " ," : "") + "\n" + tabs +  convertObject(object);
                objectListSB.append(objectListStr);
                dedentTab();
                first = false;
            }

        }
        if (asCollection && objectList.size() > 1) objectListSB.append(")");
        return objectListSB.toString();
    }

    /**
     * Creates the object, turning it into a string representation of the Vertex.
     * Closely models https://www.w3.org/TR/turtle/#grammar-production-object
     * @param object the Vertex to be converted.
     * @return the Object as a string.
     */
    private static String convertObject(Vertex object) {
        String objectStr = object.getName();

        if (config.get(1) && object.isBlank()) {
            String predicateObjectList = convertPredicateObjectList(object);

            if (predicateObjectList.contains(";") || predicateObjectList.contains(",")) {
                indentTab();
                objectStr = "[\n" + tabs + convertPredicateObjectList(object) + "\n";
                dedentTab();
                objectStr += tabs + "]";
            } else objectStr = "[" + predicateObjectList + "]";
        }
        else if (object.getElementType() == Vertex.GraphElemType.INSTANCE_LITERAL) objectStr = "\"" + objectStr + "\"";
        else objectStr = objectStr.matches("http:.*|mailto:.*") ? "<"+objectStr+">" : objectStr;

        return objectStr;
    }

    /**
     * Converts the subject node into the base class definition.
     * Closely models https://www.w3.org/TR/turtle/#grammar-production-subject
     * @param klass the subject to be converted.
     * @return a string representation of the subjects class.
     */
    private static String convertSubject(Vertex klass){
        String subname = klass.getName();
        subname = subname.matches("http:.*|mailto:.*") ? "<" + subname + ">" : subname;
        String typeDef;

        if (isOntology)
            typeDef = " a " + (klass.getTypeDefinition() != null ? klass.getTypeDefinition() + " ;" : "owl:Class ;");
        else typeDef = klass.getTypeDefinition() != null ? " a " + klass.getTypeDefinition() + " ;" : "    ";

        return subname + typeDef + "\n" + tabs;
    }

    private static void indentTab() { tabs += "\t"; }

    private static void dedentTab() { tabs = tabs.substring(0, tabs.length() - 1); }
}

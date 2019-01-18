import Conceptual.Edge;
import Conceptual.Vertex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Class that is responsible for the conversion of a visual graph into a .ttl string.
 */
class Converter {
    private static ArrayList<String>  prefixes;
    private static ArrayList<Vertex>  classes;
    private static ArrayList<Edge>    properties;
    private static ArrayList<Boolean> config;

    private static String subjectClassRedefinition;

    /**
     * The overarching method for conversion of a graph into a string.
     * @param prefixes the Arraylist of known prefixes.
     * @param classes the Arraylist of visual Classes and Literals.
     * @param properties the Arraylist of visual Properties.
     * @return a String representation of the graph as Turtle RDF syntax.
     */
    static String convertGraphToTtlString(
            ArrayList<String> prefixes,
            ArrayList<Vertex> classes,
            ArrayList<Edge> properties,
            ArrayList<Boolean> config) {
        Converter.prefixes   = prefixes;
        Converter.classes    = classes;
        Converter.properties = properties;
        Converter.config     = config;

        String fixesNeeded = getFixes();
        String stringPrefixes = convertPrefixes();
        String stringProperties = convertGProperties();
        String stringClasses  = convertGClasses();


        return fixesNeeded + stringPrefixes + stringClasses + stringProperties;
    }

    /**
     * Get potential problems that the user may want to rectify, for example blank node names.
     * @return the list of fixes.
     */
    private static String getFixes() {
        StringBuilder fixString = new StringBuilder("# Potential issues found: \n");
        final int fixStringInitLength = fixString.length();

        if (Vertex.getBlankNodeNames().size() > 0){
            fixString.append("# Don't forget to rename generic blank node names, namely: ");
            Vertex.getBlankNodeNames().forEach(n -> fixString.append(n).append(", "));
            fixString.delete(fixString.length() - 2, fixString.length());
            fixString.append(".\n\n");
        }

        return fixString.length() > fixStringInitLength ? fixString.toString() : "";
    }

    /**
     * Conversion of prefixes into .ttl prefixes.
     * Helper of {@link #convertGraphToTtlString(ArrayList, ArrayList, ArrayList, ArrayList)}.
     * @return the converted prefixes.
     */
    private static String convertPrefixes() {
        StringBuilder prefixStrs = new StringBuilder();
        prefixStrs.append("@prefix rdf : <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n");
        prefixStrs.append("@prefix rdfs : <http://www.w3.org/2000/01/rdf-schema#> .\n");
        prefixStrs.append("@prefix owl : <http://www.w3.org/2002/07/owl#> .\n");

        for (String prefix : prefixes){
            String[] splitPrefix = prefix.split(" : ", 2);
            String   prefixStr = "@prefix " + splitPrefix[0] + " : <" + splitPrefix[1] + "> .\n";

            prefixStrs.append(prefixStr);
        }

        return prefixStrs.toString();
    }

    /**
     * Conversion of visual properties into .ttl representation.
     * Helper of {@link #convertGraphToTtlString(ArrayList, ArrayList, ArrayList, ArrayList)}.
     * @return the properties as a valid .tll string.
     */
    private static String convertGProperties() {
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

            prop = prop.contains("http:") ? "<"+prop+">" : prop;
            obj = obj.matches("http:.*|mailto:.*") ? "<"+obj+">" : obj;
            sub = sub.matches("http:.*|mailto:.*") ? "<"+sub+">" : sub;

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
     * Helper of {@link #convertGraphToTtlString(ArrayList, ArrayList, ArrayList, ArrayList)}.
     * @return the converted Classes and Literals into thier .ttl equivalent.
     */
    private static String convertGClasses() {
        StringBuilder classStrs = new StringBuilder(
                "##################################################\n" +
                "#####            Ontology Classes            #####\n" +
                "##################################################\n\n"
        );

        for (Vertex graphClass : classes)
            if (graphClass.getType() == Vertex.GraphElemType.CLASS)
                classStrs.append(convertTriple(graphClass));

        return classStrs.toString();
    }

    private static String convertTriple(Vertex klass) {
        return convertTriple(klass, false);
    }

    /**
     * Converts a Class into it's .ttl representation, with append
     * @param klass the Vertex and it's outgoing Edges that will be converted.
     */
    // TODO: 18/01/2019 split class along lines of CFG
    private static String convertTriple(Vertex klass, boolean isBlankSubject) {
        String tripleString = "";

        if (!isBlankSubject) tripleString = createSubject(klass);

        HashMap<String, ArrayList<Vertex>> commonObjects = new HashMap<>();
        for (Edge edge : klass.getOutgoingEdges()){
            Vertex obj = edge.getObject();
            String edgeName = edge.getName();

            if (commonObjects.containsKey(edgeName)) commonObjects.get(edgeName).add(obj);
            else commonObjects.put(edgeName, new ArrayList<>(Collections.singletonList(obj)));
        }

        if (isBlankSubject) {
            int removeBlankIx = tripleString.length();
            tripleString += createPredicateObjectList(commonObjects);
            tripleString = tripleString.substring(0, removeBlankIx) + tripleString.substring(removeBlankIx + 4);
        } else tripleString += createPredicateObjectList(commonObjects);

        if (subjectClassRedefinition != null) {
            tripleString = tripleString.replaceFirst("owl:Class", subjectClassRedefinition);
            subjectClassRedefinition = null;
        }

        return isBlankSubject ? tripleString.substring(0, tripleString.length() - 3) : tripleString;
    }

    /**
     * Get all predicates and their associated objects of a certain subject.
     * @param commonObjects a map of predicates and their associated objects of a subject.
     * @return a .ttl representation of the predicates and objects of a subject.
     */
    private static String createPredicateObjectList(HashMap<String, ArrayList<Vertex>> commonObjects){
        StringBuilder predicateObjectSB = new StringBuilder();

        for (Map.Entry<String, ArrayList<Vertex>> e : commonObjects.entrySet()){
            String propName = e.getKey();
            String predicateObjectStr;
            ArrayList<Vertex> objs = e.getValue();

            //check if the property assigns a rdf:type to the subject.
            if (propName.matches("a|https://www.w3.org/1999/02/22-rdf-syntax-ns#type|rdf:type")){
                subjectClassRedefinition = objs.get(0).getName();
                continue;
            }

            propName = propName.matches("http:.*|mailto:.*") ? "<" + propName + ">" : propName;
            predicateObjectStr = " ;\n\t" + propName + " " + convertObjectList(objs);
            predicateObjectSB.append(predicateObjectStr);
        }

        return predicateObjectSB.toString() + " .\n\n";
    }

    private static String convertObjectList(ArrayList<Vertex> objects){
        StringBuilder objectListSB = new StringBuilder();
        boolean asContainer = config.get(0);
        boolean asBlankNodeList = config.get(1);

        if (objects.size() == 1){
            Vertex object = objects.get(0);
            return object.isBlank() && asBlankNodeList ? "["+ convertTriple(object, true)+"]" : objects.get(0).getName();
        }

        objectListSB.append(asContainer ? "(" : "\n\t\t");
        for (Vertex object : objects){
            String objectListStr;
            String objectName = object.getName();

            if (object.isBlank() && asBlankNodeList) objectListStr = "[" + convertTriple(object, true) + "]";
            else {
                objectName  = objectName.matches("http:.*|mailto:.*") ? "<"+objectName+">" : objectName;
                objectListStr = objectName + (asContainer ? " " : " ,\n\t\t");
            }
            objectListSB.append(objectListStr);
        }

        if (asContainer)
            return objectListSB.substring(0, objectListSB.length() - 1) + ")";
        else
            return objectListSB.substring(0, objectListSB.length() - 5);
    }

    /**
     * Converts the subject node into the base class definition.
     * @param klass the subject to be converted.
     * @return a string representation of the subjects class.
     */
    private static String createSubject(Vertex klass){
        String subname = klass.getName();

        subname = subname.matches("http:.*|mailto:.*") ? "<"+subname+">" : subname;
        return subname + " a owl:Class";
    }
}

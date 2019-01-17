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
    private static ArrayList<String> prefixes;
    private static ArrayList<Vertex> classes;
    private static ArrayList<Edge>   properties;

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
            ArrayList<Edge> properties) {
        Converter.prefixes   = prefixes;
        Converter.classes    = classes;
        Converter.properties = properties;

        // sort such that classes are parsed before literals, as literals are often appended to the class in .ttl.
        Converter.classes.sort((o1, o2) -> {
            boolean o1c = o1.getType() == Vertex.GraphElemType.CLASS;
            boolean o1l = o1.getType() == Vertex.GraphElemType.LITERAL;
            boolean o2c = o2.getType() == Vertex.GraphElemType.CLASS;
            boolean o2l = o2.getType() == Vertex.GraphElemType.LITERAL;

            if      (o1c && o2l) return -1;
            else if (o1l && o2c) return 1;
            else return 0;
        });

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
     * Helper of {@link #convertGraphToTtlString(ArrayList, ArrayList, ArrayList)}.
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
     * Helper of {@link #convertGraphToTtlString(ArrayList, ArrayList, ArrayList)}.
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
     * Helper of {@link #convertGraphToTtlString(ArrayList, ArrayList, ArrayList)}.
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
                classStrs.append(convertClass(graphClass));

        return classStrs.toString();
    }

    /**
     * Converts a Class into it's .ttl representation, with append
     * @param klass the GraphClass (Class) that will be converted.
     */
    private static String convertClass(Vertex klass) {
        //first create the base subject text.
        String subname = klass.getName();
        String classString;

        subname = subname.matches("http:.*") ? "<"+subname+">" : subname;
        classString = subname + " a owl:Class .\n\n";

        //then group properties with their potentially multiple objects
        HashMap<String, ArrayList<String>> commonObjects = new HashMap<>();
        for (Edge edge : klass.getOutgoingEdges()){
            String edgeName = edge.getName();
            String objName = edge.getObject().getName();

            if (commonObjects.containsKey(edgeName)) commonObjects.get(edgeName).add(objName);
            else commonObjects.put(edgeName, new ArrayList<>(Collections.singletonList(objName)));
        }

        //finally add properties and objects to the overall classString.
        for (Map.Entry<String, ArrayList<String>> e : commonObjects.entrySet()){
            String propName = e.getKey();
            ArrayList<String> objectNames = e.getValue();

            //check if the property assigns a rdf:type to the subject.
            if (propName.matches("a|https://www.w3.org/1999/02/22-rdf-syntax-ns#type|rdf:type")){
                classString = classString.replaceFirst("owl:Class", objectNames.get(0));
                continue;
            } else {
                propName = propName.matches("http:.*|mailto:.*") ? "<"+propName+">" : propName;
                classString = classString.substring(0, classString.length() - 3);
            }

            //check if the property has only one object, which has a simpler representation than the multi-object one.
            if (objectNames.size() == 1){
                classString += ";\n\t" + propName  + " " + objectNames.get(0) + " .\n\n";
            } else {
                classString += ";\n\t" + propName  + "\n\t\t";

                StringBuilder multiObjString = new StringBuilder();
                for (String objName : objectNames){
                    objName  = objName.matches("http:.*|mailto:.*") ? "<"+objName+">" : objName;
                    multiObjString.append(objName).append(" ,\n\t\t");
                }
                classString += multiObjString.toString();
                classString = classString.substring(0, classString.length() - 4);
                classString += ".\n\n";
            }
        }

        return classString;
    }
}

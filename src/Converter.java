import Conceptual.Edge;
import Conceptual.Node;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Class that is responsible for the conversion of a visual graph into a .ttl string.
 */
class Converter {
    private static ArrayList<String> prefixes;
    private static ArrayList<Node>   classes;
    private static ArrayList<Edge>   properties;
    private static HashMap<String, String> classStrings;

    /**
     * The overarching method for conversion of a graph into a string.
     * @param prefixes the Arraylist of known prefixes.
     * @param classes the Arraylist of visual Classes and Literals.
     * @param properties the Arraylist of visual Properties.
     * @return a String representation of the graph as Turtle RDF syntax.
     */
    static String convertGraphToTtlString(
            ArrayList<String> prefixes,
            ArrayList<Node> classes,
            ArrayList<Edge> properties) {
        Converter.prefixes     = prefixes;
        Converter.classes      = classes;
        Converter.properties   = properties;
        Converter.classStrings = new HashMap<>();

        // sort such that classes are parsed before literals, as literals are often appended to the class in .ttl.
        Converter.classes.sort((o1, o2) -> {
            boolean o1c = o1.getType() == Node.GraphElemType.CLASS;
            boolean o1l = o1.getType() == Node.GraphElemType.LITERAL;
            boolean o2c = o2.getType() == Node.GraphElemType.CLASS;
            boolean o2l = o2.getType() == Node.GraphElemType.LITERAL;

            if      (o1c && o2l) return -1;
            else if (o1l && o2c) return 1;
            else return 0;
        });

        String stringPrefixes = convertPrefixes();
        String stringProperties = convertGProperties();
        String stringClasses  = convertGClasses();


        return stringPrefixes + stringClasses + stringProperties;
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
            String name = property.getName();
            String propStr;

            if (name.contains(":")) {
                propStr = name + " rdf:type owl:ObjectProperty ;\n" +
                        "\trdfs:domain " + property.getSubject().getName() + " ;\n" +
                        "\trdfs:range " + property.getObject().getName() + " .\n";
                propStrs.append(propStr);
            } else {
                propStr = "<" + name + "> rdf:type owl:ObjectProperty ;\n" +
                        "\trdfs:domain " + property.getSubject().getName() + " ;\n" +
                        "\trdfs:range " + property.getObject().getName() + " .\n";
                propStrs.append(propStr);
            }
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

        for (Node graphClass : classes){
            if      (graphClass.getType() == Node.GraphElemType.CLASS)   convertClass(graphClass);
            else if (graphClass.getType() == Node.GraphElemType.LITERAL) convertLiteral(graphClass);
        }

        classStrings.values().forEach(classStrs::append);

        return classStrs.toString();
    }

    /**
     * Converts a Class into it's .ttl representation, and adds it to an extendable HashMap in case Literals need to be
     *    appended.
     * @param klass the GraphClass (Class) that will be converted.
     */
    private static void convertClass(Node klass) {
        String klassName = klass.getName();

        if (klassName.contains(":")) classStrings.put(klassName, klassName + " a owl:Class .\n\n");
        else classStrings.put(klassName, "<" + klassName + "> a owl:Class .\n\n");
    }

    /**
     * Converts a Literal to it's .ttl representation and appends it to the Class it is connected to.
     * @param klass the GraphClass (Literal) that will be converted and appended.
     */
    private static void convertLiteral(Node klass) {
        ArrayList<Edge> markRemovable = new ArrayList<>();
        String className = klass.getName();

        for (Edge property : properties){
            String trimClassString, key;
            String subjectName = property.getSubject().getName();
            String objectName  = property.getObject().getName();

            if (objectName.equals(className)){
                key = subjectName;
                String classString = classStrings.get(key);
                trimClassString = classString.substring(0, classString.length() - 3);
                markRemovable.add(property);
            } else if (subjectName.equals(className)){
                key = objectName;
                String classString = classStrings.get(key);
                trimClassString = classString.substring(0, classString.length() - 3);
                markRemovable.add(property);
            } else continue;

            String propName = property.getName();
            trimClassString += ";\n\t" + (propName.contains(":") ? propName : "<" + propName + ">") + " " +
                    (className.contains(":") || className.contains("\"") ? className : "<" + className + ">") +
                    " .\n\n";

            classStrings.put(key, trimClassString);
        }

        properties.removeAll(markRemovable);
    }
}

import Graph.GraphClass;
import Graph.GraphProperty;

import java.util.ArrayList;
import java.util.HashMap;

class Converter {
    private static ArrayList<String>        prefixes;
    private static ArrayList<GraphClass>    classes;
    private static ArrayList<GraphProperty> properties;
    private static HashMap<String, String>  classStrings;


    static String convertGraphToTtlString(ArrayList<String> prefixes, ArrayList<GraphClass> classes, ArrayList<GraphProperty> properties) {
        Converter.prefixes     = prefixes;
        Converter.classes      = classes;
        Converter.properties   = properties;
        Converter.classStrings = new HashMap<>();

        Converter.classes.sort((o1, o2) -> {
            boolean o1c = o1.getType() == GraphClass.GraphElemType.CLASS;
            boolean o1l = o1.getType() == GraphClass.GraphElemType.LITERAL;
            boolean o2c = o2.getType() == GraphClass.GraphElemType.CLASS;
            boolean o2l = o2.getType() == GraphClass.GraphElemType.LITERAL;

            if      (o1c && o2l) return -1;
            else if (o1l && o2c) return 1;
            else return 0;
        });

        return convertPrefixes() + convertGClasses() + convertGProperties();
    }

    private static String convertPrefixes() {
        StringBuilder prefixStrs = new StringBuilder();

        for (String prefix : prefixes){
            String[] splitPrefix = prefix.split("\\s:\\s");
            String   prefixStr = "@prefix " + splitPrefix[0] + " : <" + splitPrefix[1] + "> .\n";

            prefixStrs.append(prefixStr);
        }

        return prefixStrs.toString();
    }

    private static String convertGProperties() {
        // TODO: 13/12/2018 have both connections for property
        // TODO: 16/12/2018 stackpane conversion breaks when clicking on label or shape...
        StringBuilder propStrs = new StringBuilder("\n##################################################\n" +
                "#####          Ontology Properties           #####\n" +
                "##################################################\n\n");

        for (GraphProperty property : properties){
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

    // TODO: 13/12/2018 search for connections to it, add as property of connected
    private static String convertGClasses() {
        StringBuilder classStrs = new StringBuilder("\n##################################################\n" +
                "#####            Ontology Classes            #####\n" +
                "##################################################\n\n");

        for (GraphClass graphClass : classes){
            String name = graphClass.getName();

            if (graphClass.getType() == GraphClass.GraphElemType.CLASS){
                classStrings.put(name, convertClass(name));
                classStrs.append(convertClass(name));
            } else if (graphClass.getType() == GraphClass.GraphElemType.LITERAL) {
                System.out.println("Skipping literal: " + graphClass.getName());
            }
        }

        classStrings.forEach((k, v) -> System.out.println(k + " : " + v));

        return classStrs.toString();
    }

    private static String convertClass(String name) {
        if (name.contains(":")) return name + " a owl:Class .\n";
        else return "<" + name + "> a owl:Class .\n";
    }

    private static String convertLiteral(GraphClass graphClass) {
        return null;
    }
}

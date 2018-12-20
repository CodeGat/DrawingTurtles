import ConceptualElement.GraphClass;
import ConceptualElement.GraphProperty;

import java.util.ArrayList;
import java.util.HashMap;

class Converter {
    private static ArrayList<String>        prefixes;
    private static ArrayList<GraphClass>    classes;
    private static ArrayList<GraphProperty> properties;
    private static HashMap<String, String>  classStrings;

    static String convertGraphToTtlString(
            ArrayList<String> prefixes,
            ArrayList<GraphClass> classes,
            ArrayList<GraphProperty> properties) {
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
        prefixStrs.append("@prefix rdf : <http://www.w3.org/1999/02/22-rdf-syntax-ns#>.\n");
        prefixStrs.append("@prefix rdfs : <http://www.w3.org/2000/01/rdf-schema#>.\n");
        prefixStrs.append("@prefix owl : <>.\n");

        for (String prefix : prefixes){
            String[] splitPrefix = prefix.split(":", 2);
            String   prefixStr = "@prefix " + splitPrefix[0] + " : <" + splitPrefix[1] + "> .\n";

            prefixStrs.append(prefixStr);
        }

        return prefixStrs.toString();
    }

    private static String convertGProperties() {
        StringBuilder propStrs = new StringBuilder(
                "\n##################################################\n" +
                "#####          Ontology Properties           #####\n" +
                "##################################################\n\n"
        );

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

    private static String convertGClasses() {
        StringBuilder classStrs = new StringBuilder(
                "##################################################\n" +
                "#####            Ontology Classes            #####\n" +
                "##################################################\n\n"
        );

        for (GraphClass graphClass : classes){
            String name = graphClass.getName();

            if      (graphClass.getType() == GraphClass.GraphElemType.CLASS)   convertClass(name);
            else if (graphClass.getType() == GraphClass.GraphElemType.LITERAL) convertLiteral(graphClass);
        }

        classStrings.values().forEach(classStrs::append);

        return classStrs.toString();
    }

    private static void convertClass(String name) {
        if (name.contains(":")) classStrings.put(name, name + " a owl:Class .\n\n");
        else classStrings.put(name, "<" + name + "> a owl:Class .\n\n");
    }

    private static void convertLiteral(GraphClass graphClass) {
        ArrayList<GraphProperty> markRemovable = new ArrayList<>();
        for (GraphProperty property : properties){
            String trimClassString;
            String key;

            if (property.getObject().getName().equals(graphClass.getName())){
                key = property.getSubject().getName();
                String classString = classStrings.get(key);
                trimClassString = classString.substring(0, classString.length() - 3);
                markRemovable.add(property);
            } else if (property.getSubject().getName().equals(graphClass.getName())){
                key = property.getObject().getName();
                String classString = classStrings.get(key);
                trimClassString = classString.substring(0, classString.length() - 3);
                markRemovable.add(property);
            } else continue;

            String pname = property.getName();
            String gname = graphClass.getName();
            trimClassString += ";\n\t" + (pname.contains(":") ? pname : "<" + pname + ">") + " " +
                    (gname.contains(":") || gname.contains("\"") ? gname : "<" + gname + ">") + " .\n\n";

            classStrings.put(key, trimClassString);
        }

        properties.removeAll(markRemovable);
    }
}

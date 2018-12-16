import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;

class Converter {
    private static ArrayList<String>    prefixes;
    private static ArrayList<StackPane> elements;


    static String convertGraphToTtlString(ArrayList<String> prefixes, ArrayList<StackPane> elements) {
        Converter.prefixes = prefixes;
        Converter.elements = elements;

        return convertPrefixes() + convertElements();
    }

    private static String convertPrefixes() {
        StringBuilder prefixStr = new StringBuilder();

        for (String prefix : prefixes){
            String[] splitPrefix = prefix.split("\\s:\\s");
            prefixStr.append("@prefix ")
                    .append(splitPrefix[0])
                    .append(" : ")
                    .append(" <")
                    .append(splitPrefix[1])
                    .append("> .\n");
        }

        return prefixStr.toString();
    }

    private static String convertElements() {
        StringBuilder classStrs = new StringBuilder("\n##################################################\n" +
                "#####            Ontology Classes            #####\n" +
                "##################################################\n\n");
        StringBuilder propStrs = new StringBuilder("\n##################################################\n" +
                "#####          Ontology Properties           #####\n" +
                "##################################################\n\n");
        StringBuilder literalStrs = new StringBuilder("\n##################################################\n" +
                "#####            Ontology Literals           #####\n" +
                "##################################################\n\n");

        for (StackPane element : elements) {
            Node[] elementProp = element.getChildren().toArray(new Node[0]);

            if      (elementProp[0] instanceof Rectangle) literalStrs.append(convertElementLiteral(elementProp));
            else if (elementProp[0] instanceof Ellipse)   classStrs.append(convertElementClass(elementProp));
            else if (elementProp[0] instanceof Line)      propStrs.append(convertElementProperty(elementProp));
        }

        return classStrs + propStrs.toString() + literalStrs;
    }

    private static String convertElementProperty(Node[] nodes) {
        // TODO: 13/12/2018 have both connections for property
        // TODO: 16/12/2018 stackpane conversion breaks when clicking on label or shape... 
        String propertyName = ((Label) nodes[1]).getText();
        if (propertyName.contains(":")) {
            return propertyName + " rdf:type owl:ObjectProperty ;\n" +
                    "\trdfs:domain " + ((StackPane) Controller.propSrcNode).getChildren().get(1) + " ;\n" +
                    "\trdfs:range " + ((StackPane) Controller.propDestNode).getChildren().get(1) + " .";
        } else {
            return "<" + propertyName + "> rdf:type owl:ObjectProperty ;\n" +
                    "\trdfs:domain " + ((StackPane) Controller.propSrcNode).getChildren().get(1) + " ;\n" +
                    "\trdfs:range " + ((StackPane) Controller.propDestNode).getChildren().get(1) + " .\n";
        }
    }

    private static String convertElementLiteral(Node[] nodes) {
        // TODO: 13/12/2018 search for connections to it, add as property of connected
        return null;
    }

    private static String convertElementClass(Node[] nodes) {
        String className = ((Label) nodes[1]).getText();
        if (className.contains(":")) return className + " a owl:Class .\n";
        else return "<" + className + "> a owl:Class .";
    }
}

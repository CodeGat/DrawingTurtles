import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

class Converter {
    private ArrayList<String>    prefixes;
    private ArrayList<StackPane> elements;

    Converter(ArrayList<String> prefixes, ArrayList<StackPane> elements) {
        this.prefixes = prefixes;
        this.elements = elements;
    }


    void convertGraph() {
        String fileString = convertPrefixes() + convertElements();
        Path file = Paths.get("ontology.ttl");
        try {
            Files.write(file, fileString.getBytes("UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String convertPrefixes() {
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

    private String convertElements() {
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

    private String convertElementProperty(Node[] nodes) {
        // TODO: 13/12/2018 have both connections for property
        return null;
    }

    private String convertElementLiteral(Node[] nodes) {
        // TODO: 13/12/2018 search for connections to it, add as property of connected
        return null;
    }

    private String convertElementClass(Node[] nodes) {
        String className = ((Label) nodes[1]).getText();
        if (className.contains(":")) return className + " a owl:Class .\n";
        else return "<" + className + "> a owl:Class .";
    }
}

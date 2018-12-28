package Conceptual;

import javafx.scene.control.Label;

/**
 * A java-friendly representation of the Graphs properties as an Edge.
 */
public class Edge {
    private final String name;
    private final Node subject;
    private final Node object;

    /**
     * A simple constructor for the conceptual property.
     * @param name the name of the property.
     * @param subject the tail of the property arrow.
     * @param object the head of the property arrow.
     */
    public Edge(Label name, Node subject, Node object){
        this.name = name.getText();
        this.subject = subject;
        this.object = object;
    }

    public String getName() { return name; }

    public Node getObject() { return object; }

    public Node getSubject() { return subject; }
}


package Conceptual;

import javafx.scene.control.Label;

/**
 * A java-friendly representation of the Graphs properties as an Edge.
 */
public class Edge {
    private final String name;
    private final Vertex subject;
    private final Vertex object;

    /**
     * A simple constructor for the conceptual property.
     * @param name the name of the property.
     * @param subject the tail of the property arrow.
     * @param object the head of the property arrow.
     */
    public Edge(Label name, Vertex subject, Vertex object){
        this.name = name.getText();
        this.subject = subject;
        this.object = object;
    }

    public String getName() { return name; }

    public Vertex getObject() { return object; }

    public Vertex getSubject() { return subject; }
}

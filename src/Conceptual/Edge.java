package Conceptual;

import javafx.geometry.Bounds;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

/**
 * A java-friendly representation of the Graphs properties as an Edge.
 */
public class Edge {
    private final Bounds nameBounds;
    private final String name;
    private final StackPane container;
    private final Vertex subject;
    private final Vertex object;

    /**
     * A simple constructor for the conceptual property.
     * @param name the name of the property.
     * @param subject the tail of the property arrow.
     * @param object the head of the property arrow.
     */
    public Edge(StackPane container, Label name, Vertex subject, Vertex object){
        this.container = container;
        this.name = name.getText();
        this.nameBounds = name.getLayoutBounds();
        this.subject = subject;
        this.object = object;
    }

    public String getName() { return name; }

    public Vertex getObject() { return object; }

    public Vertex getSubject() { return subject; }

    public Bounds getBounds() { return nameBounds; }

    public StackPane getContainer() { return container; }
}


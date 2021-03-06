package model.conceptual;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

/**
 * A java-friendly representation of the Graphs properties as an Edge.
 */
public class Edge {
    private String name;
    private final StackPane container;
    private final Vertex subject;
    private final Vertex object;
    private boolean isIri;

    // used to specify the x-location of the whole container, not just the arrow. .
    private double layoutX;
    private double layoutY;

    /**
     * A simple constructor for the conceptual property.
     * @param container the container of the arrow and the property name.
     * @param name the name of the property.
     * @param subject the tail of the property arrow.
     * @param object the head of the property arrow.
     */
    public Edge(StackPane container, Label name, Vertex subject, Vertex object){
        this.container = container;
        this.name = name.getText();
        this.subject = subject;
        this.object = object;
        this.layoutX = container.getBoundsInParent().getMinX();
        this.layoutY = container.getBoundsInParent().getMinY();
        this.isIri = this.name.matches("https?:.*|mailto:.*");
    }

    /**
     * The bounds of the name of the arrow in the graph are given by the top-left coordinate of the container, plus
     *    the top-left coordinate of the name, as you can't simply get the coord of the name from the grandparents
     *    perspective...
     * @return the bounds of the name.
     */
    public Bounds getBounds() {
        Label name = (Label) container.getChildrenUnmodifiable().get(1);

        return new BoundingBox(
                container.getLayoutX() + name.getLayoutX(),
                container.getLayoutY() + name.getLayoutY(),
                name.getWidth(),
                name.getHeight()
        );
    }

    /**
     * Accessors
     */
    public boolean isIri(){ return isIri; }
    public boolean isSelfReferential(){ return subject == object; }
    public double getLayoutX() { return layoutX; }
    public double getLayoutY() { return layoutY; }
    public String getName() { return name; }
    public Vertex getObject() { return object; }
    public Vertex getSubject() { return subject; }
    public StackPane getContainer() { return container; }
}


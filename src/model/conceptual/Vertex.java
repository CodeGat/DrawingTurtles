package model.conceptual;

import javafx.event.EventTarget;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Ellipse;
import javafx.scene.text.Text;

import java.util.ArrayList;

/**
 * A java-friendly conceptual representation of the graphs Literals and Classes.
 */
public abstract class Vertex {

    public enum GraphElemType {
        GLOBAL_CLASS, INSTANCE_CLASS, GLOBAL_LITERAL, INSTANCE_LITERAL
    }

    /**
     * Exception when the users click is outside the bounds of this Vertex.
     */
    public class OutsideElementException extends Exception {
        OutsideElementException(){ super(); }
    }

    /**
     * Exception when the name of the Vertex does not match any of the regex for Classes or Global/Instance Literals.
     */
    public class UndefinedElementTypeException extends Exception {
        UndefinedElementTypeException() { super(); }
    }

    GraphElemType elementType;
    String name;
    StackPane container;
    double x;
    double y;
    private ArrayList<Edge> incomingEdges, outgoingEdges;

    /**
     * Constructor for the creation of a new Vertex that doesn't yet exist.
     * @param element the enclosing container for the shape and text.
     */
    public Vertex(EventTarget element) throws OutsideElementException {
        try {
            container = (StackPane) element;
        } catch (ClassCastException e) {
            throw new OutsideElementException();
        }

        this.name = ((Text) container.getChildren().get(1)).getText();

        incomingEdges = new ArrayList<>();
        outgoingEdges = new ArrayList<>();
    }

    /**
     * Snap the users property arrow as close to the edge of the shape as possible. This is fairly straightforward for
     *    a Literal, but is much more involved for a Class.
     * @param subX the x value of the subject of the property arrow - needed to find the intercept of the line and the
     *             ellipse.
     * @param subY the y value of the subject of the property arrow - needed to find the intercept of the line and the
     *             ellipse.
     * @param x the x value of the users click.
     * @param y the y value of the users click.
     */
    public abstract void setSnapTo(double subX, double subY, double x, double y);

    /**
     * Places the arrow's origin in the center of the shape, so it looks more natural when moving it around.
     */
    public void snapToCenter() {
        double minX = container.getBoundsInParent().getMinX();
        double minY = container.getBoundsInParent().getMinY();
        double maxX = container.getBoundsInParent().getMaxX();
        double maxY = container.getBoundsInParent().getMaxY();

        this.x = minX + (maxX - minX) / 2;
        this.y = minY + (maxY - minY) / 2;
    }

    /**
     * For literals, the bounds of the shape are close enough to the edges of the shape. For classes, the bounding box
     * is malformed and needs to be adjusted.
     * @return the tightest bounds for the GraphClass.
     */
    public Bounds getBounds(){
        if (this instanceof Literal){
            return container.getBoundsInParent();
        } else {
            Ellipse e = (Ellipse) container.getChildrenUnmodifiable().get(0);

            return new BoundingBox(
                    e.getCenterX(),
                    e.getCenterY(),
                    e.getRadiusX() * 2 + 2,
                    e.getRadiusY() * 2 + 2
            );
        }
    }

    /**
     * Accessor methods.
     */
    public void addIncomingEdge(Edge e){
        incomingEdges.add(e);
    }

    public void addOutgoingEdge(Edge e){ outgoingEdges.add(e); }

    public ArrayList<Edge> getIncomingEdges() { return incomingEdges; }

    public ArrayList<Edge> getOutgoingEdges() { return outgoingEdges; }

    public String getName() { return name; }

    public GraphElemType getElementType() { return elementType; }

    public StackPane getContainer() { return container; }

    public double getX() { return x; }

    public double getY() { return y; }
}

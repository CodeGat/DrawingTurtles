package Conceptual;

import javafx.event.EventTarget;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Ellipse;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * A java-friendly conceptual representation of the graphs Literals and Classes.
 */
public class Vertex {

    public enum GraphElemType {
        CLASS, LITERAL
    }

    public class OutsideElementException extends Exception {
        OutsideElementException(){
            super();
        }
    }


    private GraphElemType type;
    private String name;
    private StackPane container;
    private double x, y;
    private ArrayList<Edge> incomingEdges, outgoingEdges;


    /**
     * Constructor for the creation of a new GraphClass that doesn't yet exist.
     * Allows the property arrow to start or end at the closest edge, making it look more natural.
     * @param element the enclosing container for the shape and text
     * @param x the x coordinate the mouse was at when clicked.
     * @param y the y coordinate the mouse was at when clicked.
     */
    public Vertex(EventTarget element, double x, double y) throws OutsideElementException {
        try {
            container = (StackPane) element;
        } catch (ClassCastException e) {
            throw new OutsideElementException();
        }

        this.name = ((Text) container.getChildren().get(1)).getText();
        this.type = (container.getChildren().get(0) instanceof Ellipse ? GraphElemType.CLASS : GraphElemType.LITERAL);
        incomingEdges = new ArrayList<>();
        outgoingEdges = new ArrayList<>();

        // we get the shortest distance between the mouse click and the edge of the vertex, giving us a neat
        //    snap-to feature.
        Bounds bounds = container.getBoundsInParent();
        double distMinX = Math.abs(bounds.getMinX() - x);
        double distMaxX = Math.abs(bounds.getMaxX() - x);
        double distMinY = Math.abs(bounds.getMinY() - y);
        double distMaxY = Math.abs(bounds.getMaxY() - y);
        double[] distArray = {distMinX, distMaxX, distMinY, distMaxY};
        Arrays.sort(distArray);
        double   minDist = distArray[0];

        if (minDist == distMinX) {
            this.x = bounds.getMinX();
            this.y = y;
        } else if (minDist == distMaxX) {
            this.x = bounds.getMaxX();
            this.y = y;
        } else if (minDist == distMinY){
            this.x = x;
            this.y = bounds.getMinY();
        } else {
            this.x = x;
            this.y = bounds.getMaxY();
        }
    }

    /**
     * For literals, the bounds of the shape are close enough to the edges of the shape. For classes, the bounding box
     * is malformed and needs to be adjusted.
     * @return the tightest bounds for the GraphClass.
     */
    public Bounds getBounds(){
        if (type == GraphElemType.LITERAL){
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

    public void addIncomingEdge(Edge e){
        incomingEdges.add(e);
    }

    public void addOutgoingEdge(Edge e){
        outgoingEdges.add(e);
    }

    public ArrayList<Edge> getIncomingEdges() { return incomingEdges; }

    public ArrayList<Edge> getOutgoingEdges() { return outgoingEdges; }

    public String getName() { return name; }

    public GraphElemType getType() { return type; }

    public StackPane getContainer() { return container; }

    public double getX() { return x; }

    public double getY() { return y; }
}

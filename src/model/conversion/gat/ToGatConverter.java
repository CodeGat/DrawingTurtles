package model.conversion.gat;

import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Rectangle;
import model.conceptual.Edge;
import model.conceptual.Vertex;
import model.graph.Arrow;

import java.util.ArrayList;

/**
 * Class responsible for converting a graph into a .gat file.
 */
public class ToGatConverter {
    private double w, h;
    private ArrayList<Vertex> classes;
    private ArrayList<Edge> properties;
    /**
     * Constructor of the ElementConverter.
     * @param w width of the canvas.
     * @param h height of the canvas.
     * @param classes the Vertices we are converting to a .gat file.
     * @param properties the Edges we are converting to a .gat file.
     */
    public ToGatConverter(double w, double h, ArrayList<Vertex> classes, ArrayList<Edge> properties) {
        this.w = w;
        this.h = h;
        this.classes = classes;
        this.properties = properties;
    }

    /**
     * Traverses the graph through the children of the canvas (the drawPane), in order of creation, and gives the
     *    canvas size.
     * @return a bespoke string serialization of the children of the canvas (the elements of the graph).
     */
    public String traverseCanvas() {
        String propertyStr = traverseProperties();
        String classStr = traverseClasses();

        return "G" + w + "x" + h + classStr + propertyStr;
    }

    /**
     * Converts properties to the .gat structure.
     * @return the String .gat representation of the properties.
     */
    private String traverseProperties() {
        StringBuilder result = new StringBuilder();

        for (Edge e : properties) {
            result.append("[");
            Arrow a = (Arrow) e.getContainer().getChildren().get(0);
            String shapeInfo = "A" + a.getStartX() + "|" + a.getStartY() + "|" + a.getEndX() + "|" + a.getEndY();
            String shapeName = "=" + e.getName();
            result.append(shapeInfo).append(shapeName);
            result.append("]");
        }

        return result.toString();
    }

    /**
     * Converts classes to the .gat structure.
     * @return the String .gat representation of the properties.
     */
    private String traverseClasses() {
        StringBuilder result = new StringBuilder();

        for (Vertex v : classes) {
            result.append("[");
            if (v.getType() == Vertex.GraphElemType.CLASS){
                Ellipse e = (Ellipse) v.getContainer().getChildren().get(0);
                String shapeInfo = "C"+ e.getCenterX() + "|" + e.getCenterY() + "|" + e.getRadiusX() + "|" +
                        e.getRadiusY() + "|" + e.getFill().toString();
                String shapeName = "=" + v.getName();
                String rdfsLabel = v.getRdfsLabel() != null ? "\\|" + v.getRdfsLabel() : "\\|";
                String rdfsComment = v.getRdfsComment() != null ? "\\|" + v.getRdfsComment() : "\\|";
                result.append(shapeInfo).append(shapeName).append(rdfsLabel).append(rdfsComment);
            } else {
                Rectangle r = (Rectangle) v.getContainer().getChildren().get(0);
                String shapeInfo = "L" + r.getParent().getLayoutX() + "|" + r.getParent().getLayoutY() + "|" +
                        r.getWidth() + "|" + r.getHeight() + "|" + r.getFill().toString();
                String literalType = "|" + (r.getStrokeDashArray().size() != 0 ? "i" : "g");
                String shapeName = "=" + v.getName();
                String rdfsLabel = v.getRdfsLabel() != null ? "\\|" + v.getRdfsLabel() : "\\|";
                String rdfsComment = v.getRdfsComment() != null ? "\\|" + v.getRdfsComment() : "\\|";
                result.append(shapeInfo).append(literalType).append(shapeName).append(rdfsLabel).append(rdfsComment);
            }
            result.append("]");
        }

        return result.toString();
    }
}

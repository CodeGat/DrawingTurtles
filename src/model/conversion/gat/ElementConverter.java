package model.conversion.gat;

import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Rectangle;
import model.conceptual.Edge;
import model.conceptual.Vertex;
import model.graph.Arrow;

import java.util.ArrayList;

public class ElementConverter {
    /**
     * Traverses the graph through the children of the canvas (the drawPane), in order of creation, and gives the
     *    canvas size.
     * @return a bespoke string serialization of the children of the canvas (the elements of the graph).
     */
    public static String traverseCanvas(double w, double h, ArrayList<Vertex> classes, ArrayList<Edge> properties) {
        return "G" + w + "x" + h +
                traverseClasses(classes) +
                traverseProperties(properties);
    }

    private static String traverseProperties(ArrayList<Edge> properties) {
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

    private static String traverseClasses(ArrayList<Vertex> classes) {
        StringBuilder result = new StringBuilder();

        for (Vertex v : classes) {
            result.append("[");
            if (v.getType() == Vertex.GraphElemType.CLASS){
                Ellipse e = (Ellipse) v.getContainer().getChildren().get(0);
                String shapeInfo = "E"+ e.getCenterX() + "|" + e.getCenterY() + "|" + e.getRadiusX() + "|" +
                        e.getRadiusY() + "|" + e.getFill().toString();
                String shapeName = "=" + v.getName();
                result.append(shapeInfo).append(shapeName);
            } else if (v.getType() == Vertex.GraphElemType.LITERAL){
                Rectangle r = (Rectangle) v.getContainer().getChildren().get(0);
                String shapeInfo = "R" + r.getParent().getLayoutX() + "|" + r.getParent().getLayoutY() + "|" +
                        r.getWidth() + "|" + r.getHeight() + "|" + r.getFill().toString();
                String shapeName = "=" + v.getName();
                result.append(shapeInfo).append(shapeName);
            }
            result.append("]");
        }

        return result.toString();
    }
}

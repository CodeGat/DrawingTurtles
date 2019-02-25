package model.conversion.gat;

import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Rectangle;
import model.conceptual.Edge;
import model.conceptual.Vertex;
import model.graph.Arrow;
import model.graph.SelfReferentialArrow;

import java.util.ArrayList;

import static model.conceptual.Vertex.GraphElemType.*;

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
     * Of form: [Asx\|sy\|ex\|ey\|layX\|name]
     * @return the String .gat representation of the properties.
     */
    private String traverseProperties() {
        StringBuilder result = new StringBuilder();

        for (Edge e : properties) {
            result.append(e.isSelfReferential() ? traverseSelfReferentialProperty(e) : traverseNormalProperty(e));
        }

        return result.toString();
    }

    /**
     * Converts a self-referential Edge to the .gat structure.
     * Of form: [RcenterX\|centerY\|radiusX\|radiusY\|layX\|layY\|name]
     * @param edge the Edge we are converting
     * @return the String .gat representation of the normal property.
     */
    private String traverseSelfReferentialProperty(Edge edge) {
        SelfReferentialArrow a = (SelfReferentialArrow) edge.getContainer().getChildren().get(0);
        String shapeInfo = "R"+ a.getCenterX() + "\\|" + a.getCenterY() + "\\|" + a.getRadiusX() + "\\|" +
                a.getRadiusY() + "\\|" + edge.getLayoutX() + "\\|" + edge.getLayoutY();
        String shapeName = "\\|" + edge.getName();
        return "[" + shapeInfo + shapeName + "]";
    }

    /**
     * Converts a normal Edge (namely, one that connects between two different Vertices) to the .gat structure.
     * Of form: [Asx\|sy\|ex\|ey\|layX\|name]
     * @param edge the Edge we are converting.
     * @return the String .gat representation of the normal property.
     */
    private String traverseNormalProperty(Edge edge) {
        Arrow a = (Arrow) edge.getContainer().getChildren().get(0);
        String shapeInfo = "A" + a.getStartX() + "\\|" + a.getStartY() + "\\|" + a.getEndX() + "\\|" +
                a.getEndY() + "\\|" + edge.getLayoutX();
        String shapeName = "\\|" + edge.getName();

        return "[" + shapeInfo + shapeName + "]";
    }

    /**
     * Converts classes to the .gat structure.
     * For classes, of form:  [CcenterX\|centerY\|radiusX\|radiusY\|fill\|instanceorglobal\|name\|label\|comment]
     * For literals, of form: [LlayoutX\|layoutY\|width\|height\|fill\|instanceorglobal\|name\|datatype]
     * @return the String .gat representation of the properties.
     */
    private String traverseClasses() {
        StringBuilder result = new StringBuilder();

        for (Vertex v : classes) {
            result.append("[");
            if (v.getElementType() == GLOBAL_CLASS || v.getElementType() == INSTANCE_CLASS){
                Ellipse e = (Ellipse) v.getContainer().getChildren().get(0);
                String shapeInfo = "C" + e.getCenterX() + "\\|" + e.getCenterY() + "\\|" + e.getRadiusX() + "\\|" +
                        e.getRadiusY() + "\\|" + e.getFill().toString();
                String elemType = "\\|" + (e.getStrokeDashArray().size() != 0 ? "i" : "g");
                String shapeName = "\\|" + v.getName();
                String rdfsLabel = "\\|" + (v.getRdfsLabel() != null ? v.getRdfsLabel() : "");
                String rdfsComment = "\\|" + (v.getRdfsComment() != null ? v.getRdfsComment() : "");
                result.append(shapeInfo).append(elemType).append(shapeName).append(rdfsLabel).append(rdfsComment);
            } else {
                Rectangle r = (Rectangle) v.getContainer().getChildren().get(0);
                String shapeInfo = "L" + r.getParent().getLayoutX() + "\\|" + r.getParent().getLayoutY() + "\\|" +
                        r.getWidth() + "\\|" + r.getHeight() + "\\|" + r.getFill().toString();
                String elemType = "\\|" + (r.getStrokeDashArray().size() != 0 ? "i" : "g");
                String name = "\\|" + v.getName();
                String dataType = "\\|" + (v.getDataType() != null ? v.getDataType() : "");
                result.append(shapeInfo).append(elemType).append(name).append(dataType);
            }
            result.append("]");
        }

        return result.toString();
    }
}

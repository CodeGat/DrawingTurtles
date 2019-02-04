package model.conversion.gat;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import model.conceptual.Edge;
import model.conceptual.Vertex;
import model.graph.Arrow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class for turning a loaded .gat file into a graph.
 */
public class FromGatConverter {
    private static final Logger LOGGER = Logger.getLogger(FromGatConverter.class.getName());

    private class PropertyElemMissingException extends Exception {
        PropertyElemMissingException(String msg) { super(msg); }
    }

    private ArrayList<Vertex> classes = new ArrayList<>();
    private ArrayList<Edge>   properties = new ArrayList<>();
    private ArrayList<StackPane> compiledElements = new ArrayList<>();
    private ArrayList<StackPane> compiledProperties = new ArrayList<>();
    private double canvasWidth, canvasHeight;
    private String gat;

    public FromGatConverter(String gat){
        this.gat = gat;
    }

    /**
     * Splits the output of the .gat file into it's respective elements and attempts to bind them.
     */
    public void bindGraph() {
        String[] elements = Arrays.stream(gat.split("]\\[|\\[|]"))
                .filter(s -> !s.equals(""))
                .toArray(String[]::new);

        try {
            for (String element : elements) {
                if (element.charAt(0) == 'L') bindLiteral(element);
                else if (element.charAt(0) == 'C') bindClass(element);
                else if (element.charAt(0) == 'A') bindProperty(element);
                else if (element.charAt(0) == 'G') bindCanvas(element);
            }
        } catch (PropertyElemMissingException e){
            LOGGER.log(Level.WARNING, "Property missing: ", e);
        }
    }

    /**
     * Determine the initial size of the canvas.
     * @param size the String representation of the size of the canvas.
     */
    private void bindCanvas(String size) {
        String[] canvasSize = size.split("[Gx]");
        double width = Double.valueOf(canvasSize[1]);
        double height = Double.valueOf(canvasSize[2]);

        canvasWidth = width;
        canvasHeight = height;
    }

    /**
     * Binds a literal into both a human-friendly visual element of the graph, and a java-friendly Literal Vertex.
     * @param lit the .gat String serialization of a Literal.
     */
    private void bindLiteral(String lit) {
        String[] litElements = lit.split("=");
        String[] litInfo = litElements[0].substring(1).split("\\|");
        String   litName = litElements[1].split("\\\\\\|")[0];
        double   x = Double.valueOf(litInfo[0]);
        double   y = Double.valueOf(litInfo[1]);
        double   w = Double.valueOf(litInfo[2]);
        double   h = Double.valueOf(litInfo[3]);
        Color    col = Color.web(litInfo[4]);
        String   litType = litInfo[5];

        resizeEdgeOfCanvas(x, y);

        StackPane compiledLit = new StackPane();
        compiledLit.setLayoutX(x);
        compiledLit.setLayoutY(y);

        Rectangle rect = new Rectangle(w, h, col);
        rect.setStroke(Color.BLACK);

        if (litType.equals("i")) rect.getStrokeDashArray().addAll(10d, 10d);

        Text name = new Text(litName);

        compiledLit.getChildren().addAll(rect, name);
        compiledElements.add(compiledLit);
        try {
            classes.add(new Vertex(compiledLit));
        } catch (Vertex.OutsideElementException e) {
            e.printStackTrace();
        }
    }

    /**
     * Binds a class into both a human-friendly visual element of the graph, and a java-friendly Vertex.
     * @param cls the .gat String serialization of a Class.
     */
    private void bindClass(String cls) {
        String[] clsElements = cls.split("=");
        String[] clsShape    = clsElements[0].substring(1).split("\\|");
        String[] clsInfo     = clsElements[1].split("\\\\\\|", -1);
        String   clsName     = clsInfo[0];
        String   rdfsLabel   = clsInfo[1];
        String   rdfsComment = clsInfo[2];
        double   x = Double.valueOf(clsShape[0]);
        double   y = Double.valueOf(clsShape[1]);
        double   rx = Double.valueOf(clsShape[2]);
        double   ry = Double.valueOf(clsShape[3]);
        Color    col = Color.web(clsShape[4]);

        resizeEdgeOfCanvas(x, y);

        StackPane compiledCls = new StackPane();
        compiledCls.setLayoutX(x);
        compiledCls.setLayoutY(y);

        Ellipse ellipse = new Ellipse(x, y, rx, ry);
        ellipse.setFill(col);
        ellipse.setStroke(Color.BLACK);

        Text name = new Text(clsName);

        compiledCls.getChildren().addAll(ellipse, name);
        compiledElements.add(compiledCls);

        try {
            if (!rdfsLabel.equals("") || !rdfsComment.equals(""))
                classes.add(new Vertex(compiledCls, rdfsLabel, rdfsComment));
            else classes.add(new Vertex(compiledCls));
        } catch (Vertex.OutsideElementException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a human-friendly graph property arrow, and binds a java-friendly Edge.
     * @param prop the .gat String serialization of a Property.
     */
    private void bindProperty(String prop) throws PropertyElemMissingException {
        String[] propElements = prop.split("=");
        String[] propInfo     = propElements[0].substring(1).split("\\|");
        String   propName     = propElements[1];
        double sx = Double.valueOf(propInfo[0]);
        double sy = Double.valueOf(propInfo[1]);
        double ex = Double.valueOf(propInfo[2]);
        double ey = Double.valueOf(propInfo[3]);

        StackPane compiledProp = new StackPane();
        compiledProp.setLayoutX(sx < ex ? sx : ex);
        compiledProp.setLayoutY(sy < ey ? sy : ey);

        Arrow arrow = new Arrow();
        arrow.setStartX(sx);
        arrow.setStartY(sy);
        arrow.setEndX(ex);
        arrow.setEndY(ey);

        Label name = new Label(propName);
        name.setBackground(new Background(new BackgroundFill(Color.web("f4f4f4"), CornerRadii.EMPTY, Insets.EMPTY)));

        compiledProp.getChildren().addAll(arrow, name);
        compiledProperties.add(compiledProp);
        compiledProp.toBack();

        Vertex sub = findClassUnder(sx, sy);
        Vertex obj = findClassUnder(ex, ey);
        if (sub != null && obj != null) {
            Edge edge = new Edge(compiledProp, name, sub, obj);
            sub.addOutgoingEdge(edge);
            obj.addIncomingEdge(edge);
            properties.add(edge);
        } else throw new PropertyElemMissingException((sub == null ? "sub" : " obj") + " missing from property " + name.getText());
    }

    /**
     * Ties a coordinate to the class or literal below it, used in finding the subject and object of a property given
     *    the general start and end coordinates of the arrow. Also finds the class below a users click.
     * @param x a x coordinate, given some leeway in the Bounds.
     * @param y a y coordinate, given some leeway in the Bounds.
     * @return the class or literal under the (x, y) coordinate, or null otherwise.
     */
    private Vertex findClassUnder(double x, double y) {
        for (Vertex klass : classes) {
            Bounds classBounds = klass.getBounds();
            Bounds pointBounds = new BoundingBox(x-1, y-1, 2, 2);

            if (classBounds.intersects(pointBounds)) return klass;
        }
        return null;
    }

    /**
     * Extend the canvas width and height if any new element gets to close to the bounds of the drawPane canvas.
     * @param x x coordinate to check if we need to extend the width of the canvas.
     * @param y y coordinate to check if we need to extend the height of the canvas.
     */
    private void resizeEdgeOfCanvas(double x, double y) {

        if (x > canvasWidth - 150 && y > canvasHeight - 150) {
            canvasWidth  = x + 300;
            canvasHeight = y + 300;
        } else if (x > canvasWidth - 150) {
            canvasWidth = x + 300;
        } else if (y > canvasHeight - 150) {
            canvasHeight = y + 300;
        }
    }

    /**
     * Accessor methods.
     */
    public double getCanvasHeight() { return canvasHeight; }
    public double getCanvasWidth() { return canvasWidth; }
    public ArrayList<Vertex> getClasses() { return classes; }
    public ArrayList<Edge>   getProperties() { return properties; }
    public ArrayList<StackPane> getCompiledElements() { return compiledElements; }
    public ArrayList<StackPane> getCompiledProperties() { return compiledProperties; }
}

package model.conversion.gat;

import controller.Controller;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
import model.conceptual.Vertex.OutsideElementException;
import model.conceptual.Vertex.UndefinedElementTypeException;
import model.graph.Arrow;
import model.graph.SelfReferentialArrow;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Class for turning a .gat file into a graph.
 */
public class FromGatConverter {

    /**
     * Exception if a subject/object cannot be reached from either end of the property arrow.
     */
    public class PropertyElemMissingException extends Exception {
        private String elementMissing, propertyName;

        PropertyElemMissingException(String elementMissing, String propertyName) {
            super(elementMissing + " missing from " + propertyName);
            this.elementMissing = elementMissing;
            this.propertyName = propertyName;
        }

        public String getMissingElement() { return elementMissing; }
        public String getPropertyName() { return propertyName; }
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
     * @throws PropertyElemMissingException passed from {@link #bindNormalProperty(String)}
     * @throws OutsideElementException passed from {@link #bindClass(String)}
     * @throws UndefinedElementTypeException passed from {@link #bindClass(String)}
     */
    public void bindGraph()
            throws PropertyElemMissingException, OutsideElementException, UndefinedElementTypeException {
        String[] elements = Arrays.stream(gat.split("]\\[|\\[|]"))
                .filter(s -> !s.equals(""))
                .toArray(String[]::new);

        for (String element : elements) {
            if (element.charAt(0) == 'L') bindLiteral(element);
            else if (element.charAt(0) == 'C') bindClass(element);
            else if (element.charAt(0) == 'A') bindNormalProperty(element);
            else if (element.charAt(0) == 'R') bindSelfReferentialProperty(element);
            else if (element.charAt(0) == 'G') bindCanvas(element);
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
     * @throws OutsideElementException if the Vertex is outside the canvas.
     * @throws UndefinedElementTypeException if the name of the Vertex does not match up with Turtle syntax.
     */
    private void bindLiteral(String lit) throws OutsideElementException, UndefinedElementTypeException {
        String[] litElements = lit.split("\\\\\\|", -1);
        double x = Double.valueOf(litElements[0].substring(1));
        double y = Double.valueOf(litElements[1]);
        double w = Double.valueOf(litElements[2]);
        double h = Double.valueOf(litElements[3]);
        Color  c = Color.web(litElements[4]);
        String etype = litElements[5];
        Text   name  = new Text(litElements[6]);
        String dtype = litElements[7];

        resizeEdgeOfCanvas(x, y);

        StackPane compiledLit = new StackPane();
        compiledLit.setLayoutX(x);
        compiledLit.setLayoutY(y);

        Rectangle rect = new Rectangle(w, h, c);
        rect.setStroke(Color.BLACK);

        // if the literal is an instance literal, give it a dashed rectangle.
        if (etype.equals("i")) rect.getStrokeDashArray().addAll(10d, 10d);

        compiledLit.getChildren().addAll(rect, name);
        compiledElements.add(compiledLit);

        if (!dtype.equals("")) classes.add(new Vertex(compiledLit, dtype));
        else classes.add(new Vertex(compiledLit));
    }

    /**
     * Binds a class into both a human-friendly visual element of the graph, and a java-friendly Vertex.
     * @param cls the .gat String serialization of a Class.
     * @throws OutsideElementException if the Vertex is outside the canvas.
     * @throws UndefinedElementTypeException if the name of the Vertex does not match up with Turtle syntax.
     */
    private void bindClass(String cls) throws OutsideElementException, UndefinedElementTypeException {
        String[] clsElements = cls.split("\\\\\\|", -1);
        double x = Double.valueOf(clsElements[0].substring(1));
        double y = Double.valueOf(clsElements[1]);
        double rx = Double.valueOf(clsElements[2]);
        double ry = Double.valueOf(clsElements[3]);
        Color  c  = Color.web(clsElements[4]);
        String etype = clsElements[5];
        Text   name = new Text(clsElements[6]);
        String label = clsElements[7];
        String comment = clsElements[8];

        resizeEdgeOfCanvas(x, y);

        StackPane compiledCls = new StackPane();
        compiledCls.setLayoutX(x);
        compiledCls.setLayoutY(y);

        Ellipse ellipse = new Ellipse(x, y, rx, ry);
        ellipse.setFill(c);
        ellipse.setStroke(Color.BLACK);

        // if the class is an instance class, give it a dashed ellipse.
        if (etype.equals("i")) ellipse.getStrokeDashArray().addAll(10d, 10d);

        compiledCls.getChildren().addAll(ellipse, name);
        compiledElements.add(compiledCls);

        if (!label.equals("") || !comment.equals(""))
            classes.add(new Vertex(compiledCls, label, comment, etype.equals("i")));
        else classes.add(new Vertex(compiledCls));
    }

    /**
     * Creates a human-friendly graph property arrow, and binds a java-friendly Edge.
     * @param prop the .gat String serialization of a Property.
     * @throws PropertyElemMissingException if the start and end of the Arrow do not reach a class/literal.
     */
    private void bindNormalProperty(String prop) throws PropertyElemMissingException {
        String[] propElements = prop.split("\\\\\\|");
        double sx = Double.valueOf(propElements[0].substring(1));
        double sy = Double.valueOf(propElements[1]);
        double ex = Double.valueOf(propElements[2]);
        double ey = Double.valueOf(propElements[3]);
        double lx = Double.valueOf(propElements[4]);
        String propName = propElements[5];

        StackPane compiledProp = new StackPane();
        compiledProp.setLayoutX(lx);
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
        } else throw new PropertyElemMissingException((sub == null ? "subject" : "object"), name.getText());
    }

    /**
     * Creates a human-friendly self-referential graph property arrow, and binds a java-friendly Edge.
     * @param refProp the .gat String serialization of a self-referential Property.
     * @throws PropertyElemMissingException if the self-referential Property is not associated with a class.
     */
    private void bindSelfReferentialProperty(String refProp) throws PropertyElemMissingException {
        String[] propElements = refProp.split("\\\\\\|", -1);
        double x = Double.valueOf(propElements[0].substring(1));
        double y = Double.valueOf(propElements[1]);
        double rx = Double.valueOf(propElements[2]);
        double ry = Double.valueOf(propElements[3]);
        double lx = Double.valueOf(propElements[4]);
        double ly = Double.valueOf(propElements[5]);
        String propName = propElements[6];

        resizeEdgeOfCanvas(x, y);

        StackPane compiledSelfRefProp = new StackPane();
        compiledSelfRefProp.setLayoutX(lx);
        compiledSelfRefProp.setLayoutY(ly);

        SelfReferentialArrow arrow = new SelfReferentialArrow();
        arrow.setCenterX(x);
        arrow.setCenterY(y);
        arrow.setRadiusX(rx);
        arrow.setRadiusY(ry);

        Label name = new Label(propName);
        name.setBackground(new Background(new BackgroundFill(Controller.JFX_DEFAULT_COLOUR, CornerRadii.EMPTY, Insets.EMPTY)));

        compiledSelfRefProp.getChildren().addAll(arrow, name);
        StackPane.setAlignment(name, Pos.BOTTOM_CENTER);

        compiledProperties.add(compiledSelfRefProp);
        compiledSelfRefProp.toBack();

        Vertex vertex = findClassUnder(x, y);
        if (vertex != null){
            Edge edge = new Edge(compiledSelfRefProp, name, vertex, vertex);
            vertex.addOutgoingEdge(edge);
            vertex.addIncomingEdge(edge);
            properties.add(edge);
        } else throw new PropertyElemMissingException("self-referential class", name.getText());
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
            Bounds pointBounds = new BoundingBox(x-2, y-2, 4, 4);

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

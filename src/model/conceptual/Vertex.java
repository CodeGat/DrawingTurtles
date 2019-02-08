package model.conceptual;

import javafx.event.EventTarget;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Ellipse;
import javafx.scene.text.Text;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * A java-friendly conceptual representation of the graphs Literals and Classes.
 */
public class Vertex {

    public enum GraphElemType {
        CLASS, GLOBAL_LITERAL, INSTANCE_LITERAL
    }

    public class OutsideElementException extends Exception {
        OutsideElementException(){
            super();
        }
    }

    public class UndefinedElementTypeException extends Exception {
        UndefinedElementTypeException() { super(); }
    }

    private static char nextBlankNodeName = (char) 96;
    private static final ArrayList<Character> blankNodeNames = new ArrayList<>();
    private static final String globalLiteralRegex = "\".*\"(\\^\\^.*|@.*)?" + // unspecified / String
            "|true|false" + // boolean
            "|[+\\-]?\\d+" + //integer
            "|[+\\-]?\\d*\\.\\d+" + // decimal
            "|([+\\-]?\\d+\\.\\d+|[+\\-]?\\.\\d+|[+\\-]?\\d+)[Ee][+\\-]\\d+"; // double;
    private static final String instanceLiteralRegex = "(?<!\")[^:]*(?!\")";

    private GraphElemType elementType;
    private String dataType;
    private String name;
    private StackPane container;
    private double x, y;
    private ArrayList<Edge> incomingEdges, outgoingEdges;
    private boolean isBlankNode;
    private boolean isIri;
    private String typeDefinition;

    private String rdfsLabel, rdfsComment;

    public Vertex(EventTarget element, String rdfsLabel, String rdfsComment)
            throws OutsideElementException, UndefinedElementTypeException {
        this(element);
        this.rdfsLabel = rdfsLabel;
        this.rdfsComment = rdfsComment;
    }

    public Vertex(EventTarget element, String dataType) throws OutsideElementException, UndefinedElementTypeException {
        this(element);
        this.dataType = dataType;
    }

    /**
     * Constructor for the creation of a new GraphClass that doesn't yet exist.
     * Allows the property arrow to start or end at the closest edge, making it look more natural.
     * @param element the enclosing container for the shape and text
     */
    public Vertex(EventTarget element) throws OutsideElementException, UndefinedElementTypeException {
        try {
            container = (StackPane) element;
        } catch (ClassCastException e) {
            throw new OutsideElementException();
        }

        this.name = ((Text) container.getChildren().get(1)).getText();

        if (this.name.charAt(0) == '_') {
            ((Text) container.getChildren().get(1)).setText("");
            isBlankNode = true;
            isIri = false;
        } else if (this.name.matches("http:.*|mailto:.*")){
            this.name = "<" + this.name + ">";
            isBlankNode = false;
            isIri = true;
        } else {
            isBlankNode = false;
            isIri = false;
        }

        if (container.getChildren().get(0) instanceof Ellipse) this.elementType = GraphElemType.CLASS;
        else if (this.name.matches(globalLiteralRegex)) this.elementType = GraphElemType.GLOBAL_LITERAL;
        else if (this.name.matches(instanceLiteralRegex)) this.elementType = GraphElemType.INSTANCE_LITERAL;
        else throw new UndefinedElementTypeException();

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
    public void setSnapTo(double subX, double subY, double x, double y){
        if (this.elementType == GraphElemType.GLOBAL_LITERAL || this.elementType == GraphElemType.INSTANCE_LITERAL) {
            Bounds bounds = container.getBoundsInParent();
            double distMinX = Math.abs(bounds.getMinX() - x);
            double distMaxX = Math.abs(bounds.getMaxX() - x);
            double distMinY = Math.abs(bounds.getMinY() - y);
            double distMaxY = Math.abs(bounds.getMaxY() - y);
            double[] distArray = {distMinX, distMaxX, distMinY, distMaxY};
            Arrays.sort(distArray);
            double minDist = distArray[0];

            if (minDist == distMinX) {
                this.x = bounds.getMinX();
                this.y = y;
            } else if (minDist == distMaxX) {
                this.x = bounds.getMaxX();
                this.y = y;
            } else if (minDist == distMinY) {
                this.x = x;
                this.y = bounds.getMinY();
            } else {
                this.x = x;
                this.y = bounds.getMaxY();
            }
        } else {
            this.snapToCenter();

            Bounds b = container.getBoundsInParent();
            Ellipse e = (Ellipse) container.getChildren().get(0);

            ArrayList<Pair<Double, Double>> coords = getIntersection(
                    subX, subY,
                    this.x, this.y,
                    b.getMinX() + e.getRadiusX(), b.getMinY() + e.getRadiusY(),
                    e.getRadiusX(), e.getRadiusY()
            );

            if (coords.size() > 1) System.out.println("Went across two shapes, exiting. ");

            this.x = coords.get(0).getKey();
            this.y = coords.get(0).getValue();
        }
    }

    /**
     * Find the intersection(s) of the property arrow and the Object ellipse. Should usually be a single intersection,
     *    multiple denote a pass across the shape rather than into it.
     * @param x1 subject point x-value.
     * @param y1 subject point y-value.
     * @param x2 object x-value.
     * @param y2 object y-value.
     * @param midX x-value of the midpoint of the ellipse.
     * @param midY y-value of the midpoint of the ellipse.
     * @param h the major axis of the ellipse.
     * @param v the minor axis of the ellipse.
     * @return list of coordinates in which the ellipse and the line intersect.
     */
    private static ArrayList<Pair<Double, Double>> getIntersection(
            double x1, double y1,
            double x2, double y2,
            double midX, double midY,
            double h, double v) {
        ArrayList<Pair<Double, Double>> points = new ArrayList<>();

        x1 -= midX;
        y1 -= midY;

        x2 -= midX;
        y2 -= midY;

        if (x1 == x2) {
            double y = (v/h)*Math.sqrt(h*h-x1*x1);
            if (Math.min(y1, y2) <= y && y <= Math.max(y1, y2)) points.add(new Pair<>(x1+midX, y+midY));
            if (Math.min(y1, y2) <= -y && -y <= Math.max(y1, y2)) points.add(new Pair<>(x1+midX, -y+midY));
        } else {
            double a = (y2 - y1) / (x2 - x1);
            double b = (y1 - a*x1);

            double r = a*a*h*h + v*v;
            double s = 2*a*b*h*h;
            double t = h*h*b*b - h*h*v*v;

            double d = s*s - 4*r*t;

            if (d > 0) {
                double xi1 = (-s+Math.sqrt(d))/(2*r);
                double xi2 = (-s-Math.sqrt(d))/(2*r);

                double yi1 = a*xi1+b;
                double yi2 = a*xi2+b;

                if (isPointInLine(x1, y1, x2, y2, xi1, yi1)) points.add(new Pair<>(xi1+midX, yi1+midY));
                if (isPointInLine(x1, y1, x2, y2, xi2, yi2)) points.add(new Pair<>(xi2+midX, yi2+midY));
            } else if (d == 0) {
                double xi = -s/(2*r);
                double yi = a*xi+b;

                if (isPointInLine(x1, y1, x2, y2, xi, yi)) points.add(new Pair<>(xi+midX, yi+midY));
            }
        }

        return points;
    }

    /**
     * Determines if a given point is within the the given line.
     * @param x1 subject x-value.
     * @param y1 subject y-value.
     * @param x2 object x-value.
     * @param y2 object y-value.
     * @param px given points x-value.
     * @param py given points y-value.
     * @return whether the point is within the line or not.
     */
    private static boolean isPointInLine(double x1, double y1, double x2, double y2, double px, double py) {
        double xMin = Math.min(x1, x2);
        double xMax = Math.max(x1, x2);

        double yMin = Math.min(y1, y2);
        double yMax = Math.max(y1, y2);

        return (xMin <= px && px <= xMax) && (yMin <= py && py <= yMax);
    }

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
        if (elementType == GraphElemType.GLOBAL_LITERAL || elementType == GraphElemType.INSTANCE_LITERAL){
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

    // TODO: 20/01/2019 determine the effect of removing the edge to the typedef.
    public void addOutgoingEdge(Edge e){
        if (e.getName().matches("a|rdf:type|http://www.w3.org/1999/02/22-rdf-syntax-ns#type")){
            typeDefinition = e.getObject().name;
        } else {
            outgoingEdges.add(e);
        }
    }

    public ArrayList<Edge> getIncomingEdges() { return incomingEdges; }

    public ArrayList<Edge> getOutgoingEdges() { return outgoingEdges; }

    public String getName() { return name; }

    public GraphElemType getElementType() { return elementType; }

    public StackPane getContainer() { return container; }

    public double getX() { return x; }

    public double getY() { return y; }

    public boolean isBlank() { return isBlankNode; }

    public boolean isIri() { return isIri; }

    public static char getNextBlankNodeName() {
        nextBlankNodeName += 1;
        blankNodeNames.add(nextBlankNodeName);
        return nextBlankNodeName;
    }

    public static ArrayList<Character> getBlankNodeNames(){ return blankNodeNames; }

    public String getTypeDefinition() { return typeDefinition; }

    public String getRdfsLabel() {
        return rdfsLabel;
    }

    public String getRdfsComment() {
        return rdfsComment;
    }

    public String getDataType() {
        if (dataType == null) return null;
        else if (dataType.matches("http(s)?:.*") && elementType != GraphElemType.CLASS)
            return "<" + dataType + ">";
        else if (this.elementType != GraphElemType.CLASS)
            return dataType;

        System.out.println("data type was not null or a class or an iri, must have been empty string. ");
        return null;
    }
}

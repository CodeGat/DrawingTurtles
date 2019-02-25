package model.conceptual;

import javafx.event.EventTarget;
import javafx.geometry.Bounds;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;
import javafx.util.Pair;

import java.util.ArrayList;

public class Class extends Vertex {
    private boolean isBlankNode;
    private boolean isIri;
    private String typeDefinition;
    private String rdfsLabel, rdfsComment;

    private static char nextBlankNodeName = (char) 96;
    private static final ArrayList<Character> blankNodeNames = new ArrayList<>();


    /**
     * Constructor for a Vertex with meta-information, namely it's human-readable label and comment (defined in RDFS).
     *
     * @param element     the container of the shape and name of the Vertex.
     * @param rdfsLabel   the human-readable label of the Vertex.
     * @param rdfsComment the comment regarding the Vertex.
     * @throws OutsideElementException       if the container is not castable to a stackpane (aka it is outside the canvas.
     */
    public Class(EventTarget element, String rdfsLabel, String rdfsComment) throws OutsideElementException {
        this(element);
        this.rdfsLabel = rdfsLabel;
        this.rdfsComment = rdfsComment;
    }

    public Class(EventTarget container) throws OutsideElementException {
        super(container);

        if (super.name.charAt(0) == '_') {
            ((Text) super.container.getChildren().get(1)).setText("");
            isBlankNode = true;
            isIri = false;
        } else if (this.name.matches("https?:.*|mailto:.*")){
            isBlankNode = false;
            isIri = true;
        } else {
            isBlankNode = false;
            isIri = false;
        }

        Shape shape = (Shape) super.container.getChildren().get(0);
        if (shape instanceof Ellipse && shape.getStrokeDashArray().size() == 0)
            super.elementType = GraphElemType.GLOBAL_CLASS;
        else if (shape instanceof Ellipse) super.elementType = GraphElemType.INSTANCE_CLASS;
    }

    /**
     * Snap the users property arrow as close to the edge of the shape as possible. This is fairly straightforward for
     * a Literal, but is much more involved for a Class.
     *
     * @param subX the x value of the subject of the property arrow - needed to find the intercept of the line and the
     *             ellipse.
     * @param subY the y value of the subject of the property arrow - needed to find the intercept of the line and the
     *             ellipse.
     * @param x    the x value of the users click.
     * @param y    the y value of the users click.
     */
    @Override
    public void setSnapTo(double subX, double subY, double x, double y) {
        this.snapToCenter();

        Bounds b = container.getBoundsInParent();
        Ellipse e = (Ellipse) container.getChildren().get(0);

        ArrayList<Pair<Double, Double>> coords = getIntersection(
                subX, subY,
                this.x, this.y,
                b.getMinX() + e.getRadiusX(), b.getMinY() + e.getRadiusY(),
                e.getRadiusX(), e.getRadiusY()
        );

        if (coords.size() == 1) {
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

    @Override
    public void addOutgoingEdge(Edge e) {
        if (e.getName().matches("a|rdf:type|http://www.w3.org/1999/02/22-rdf-syntax-ns#type"))
            typeDefinition = e.getObject().name;
        else
            super.addOutgoingEdge(e);
    }

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
}

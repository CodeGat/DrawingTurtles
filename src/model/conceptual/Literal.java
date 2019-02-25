package model.conceptual;

import javafx.event.EventTarget;
import javafx.geometry.Bounds;

import java.util.Arrays;

public class Literal extends Vertex {
    private static final String globalLiteralRegex = "\".*\"(\\^\\^.*|@.*)?" + // unspecified / String
            "|true|false" + // boolean
            "|[+\\-]?\\d+" + //integer
            "|[+\\-]?\\d*\\.\\d+" + // decimal
            "|([+\\-]?\\d+\\.\\d+|[+\\-]?\\.\\d+|[+\\-]?\\d+)[Ee][+\\-]\\d+"; // double;
    private static final String instanceLiteralRegex = "(?<!\")[^:]*(?!\")";

    private String dataType;

    /**
     * Constructor for a Vertex with meta-information regarding the datatype of the Literal.
     *
     * @param container  the container of the shape and the name.
     * @param dataType the data type of the given Vertex.
     * @throws OutsideElementException       if the container is not castable to a stackpane (aka it is outside the canvas.
     * @throws UndefinedElementTypeException if the name of the Vertex does not correspond to any of the GraphElemTypes.
     */
    public Literal(EventTarget container, String dataType)
            throws OutsideElementException, UndefinedElementTypeException {
        this(container);
        this.dataType = dataType;
    }

    public Literal(EventTarget container) throws OutsideElementException, UndefinedElementTypeException {
        super(container);

        if (this.name.matches(globalLiteralRegex)) this.elementType = GraphElemType.GLOBAL_LITERAL;
        else if (this.name.matches(instanceLiteralRegex)) this.elementType = GraphElemType.INSTANCE_LITERAL;
        else throw new UndefinedElementTypeException();
    }

    /**
     * @return the datatype of the Vertex, in angle-brackets if it is a fully-qualified IRI.
     */
    public String getDataType() {
        if (this.elementType == GraphElemType.GLOBAL_LITERAL){
            final String ints = "[+\\-]?\\d";

            if      (name.matches("\".*\"")) return "xsd:string";
            else if (name.matches("true|false")) return  "xsd:boolean";
            else if (name.matches(ints+"+")) return "xsd:integer";
            else if (name.matches(ints+"*\\.\\d+")) return "xsd:decimal";
            else if (name.matches("("+ints+"+\\.\\d+|[+\\-]?\\.\\d+|"+ints+")E"+ints+"+")) return "xsd:double";
            else if (name.matches(".*\\^\\^.*")) return name.split("\\^\\^")[1];
            else return null;
        }

        if (dataType == null)
            return null;
        else if (dataType.matches("http(s)?:.*") && elementType != GraphElemType.GLOBAL_CLASS)
            return "<" + dataType + ">";
        else if (this.elementType != GraphElemType.GLOBAL_CLASS)
            return dataType;
        else
            return null;
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
    }
}

package model.graph;

import javafx.beans.InvalidationListener;
import javafx.beans.property.DoubleProperty;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.QuadCurve;

public class SelfReferentialArrow extends Group {
    private final QuadCurve curve;
    private static final double arrowLength = 20;
    private static final double arrowWidth = 7;

    public SelfReferentialArrow() {
        this(new QuadCurve(), new Line(), new Line());
    }

    private SelfReferentialArrow(QuadCurve curve, Line arrow1, Line arrow2) {
        super(curve, arrow1, arrow2);
        this.curve = curve;
        this.curve.setStroke(Color.web("000000"));
        this.curve.setFill(Color.web("f4f4f4"));

        InvalidationListener updater = o -> {
            double ex = getEndX();
            double ey = getEndY();
            double cx = getControlX();
            double cy = getControlY();
            double sx = getStartX();
            double sy = getStartY();

            arrow1.setEndX(ex);
            arrow1.setEndY(ey);
            arrow2.setEndX(ex);
            arrow2.setEndY(ey);

            if (ex == sx && ey == sy) {
                // arrow parts of length 0
                arrow1.setStartX(ex);
                arrow1.setStartY(ey);
                arrow2.setStartX(ex);
                arrow2.setStartY(ey);
            } else {
                double factor = arrowLength / Math.hypot(sx-cx, sy-cy);
                double factorO = arrowWidth / Math.hypot(sx-cx, sy-cy);

                // part in direction of main line
                double dx = (sx - cx) * factor;
                double dy = (sy - cy) * factor;

                // part ortogonal to main line
                double ox = (sx - cx) * factorO;
                double oy = (sy - cy) * factorO;

                arrow1.setStartX(cx + dx - oy);
                arrow1.setStartY(cy + dy + ox);
                arrow2.setStartX(cx + dx + oy);
                arrow2.setStartY(cy + dy - ox);
            }
        };

        // add updater to properties
        startXProperty().addListener(updater);
        startYProperty().addListener(updater);
        controlXProperty().addListener(updater);
        controlYProperty().addListener(updater);
        endXProperty().addListener(updater);
        endYProperty().addListener(updater);
        updater.invalidated(null);
    }

    /**
     * Basic getter/setter/property methods.
     */
    public final void setStartX(double value) { curve.setStartX(value); }
    public final void setStartY(double value) { curve.setStartY(value); }
    public final void setEndX(double value)   { curve.setEndX(value); }
    public final void setEndY(double value)   { curve.setEndY(value); }
    public final void setControlX(double value) { curve.setControlX(value); }
    public final void setControlY(double value) { curve.setControlY(value); }

    public double getStartX() { return curve.getStartX(); }
    public double getStartY() { return curve.getStartY(); }
    public double getControlX() { return curve.getControlX(); }
    public double getControlY() { return curve.getControlY(); }
    public double getEndX()   { return curve.getEndX(); }
    public double getEndY()   { return curve.getEndY(); }
    public double getWidth() { return Math.abs(curve.getStartX() - curve.getEndX()); }

    private DoubleProperty startXProperty() { return curve.startXProperty(); }
    private DoubleProperty startYProperty() { return curve.startYProperty(); }
    private DoubleProperty controlXProperty() { return curve.controlXProperty(); }
    private DoubleProperty controlYProperty() { return curve.controlYProperty(); }
    private DoubleProperty endXProperty()   { return curve.endXProperty(); }
    private DoubleProperty endYProperty()   { return curve.endYProperty(); }
}

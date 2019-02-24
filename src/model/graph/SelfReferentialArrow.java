package model.graph;

import javafx.beans.InvalidationListener;
import javafx.beans.property.DoubleProperty;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;

public class SelfReferentialArrow extends Group {
    private final Ellipse ellipse;

    public SelfReferentialArrow() { this(new Ellipse(), new Line(), new Line()); }

    private SelfReferentialArrow(Ellipse ellipse, Line arrow1, Line arrow2){
        super(ellipse, arrow1, arrow2);
        this.ellipse = ellipse;
        this.ellipse.setFill(Color.TRANSPARENT);
        this.ellipse.setStroke(Color.BLACK);

        InvalidationListener updater = o -> {
            double cx = getCenterX();
            double cy = getCenterY();
            double rx = getRadiusX();
            double ry = getRadiusY();
            double head = (cx - rx / 2) - rx / 2 ;

            ellipse.setCenterX(cx);
            ellipse.setCenterY(cy);
            ellipse.setRadiusX(rx);
            ellipse.setRadiusY(ry);

            arrow1.setStartX(head);
            arrow1.setStartY(cy);
            arrow1.setEndX(head - 10);
            arrow1.setEndY(cy + 10);

            arrow2.setStartX(head);
            arrow2.setStartY(cy);
            arrow2.setEndX(head +  10);
            arrow2.setEndY(cy + 10);
        };

        centerXProperty().addListener(updater);
        centerYProperty().addListener(updater);
        radiusXProperty().addListener(updater);
        radiusYProperty().addListener(updater);
        updater.invalidated(null);
    }

    public void setCenterX(double value){ ellipse.setCenterX(value); }
    public void setCenterY(double value){ ellipse.setCenterY(value); }
    public void setRadiusX(double value){ ellipse.setRadiusX(value); }
    public void setRadiusY(double value){ ellipse.setRadiusY(value); }

    public double getCenterX() { return ellipse.getCenterX(); }
    public double getCenterY() { return ellipse.getCenterY(); }
    public double getRadiusX() { return ellipse.getRadiusX(); }
    public double getRadiusY() { return ellipse.getRadiusY(); }

    private DoubleProperty centerXProperty() { return ellipse.centerXProperty(); }
    private DoubleProperty centerYProperty() { return ellipse.centerYProperty(); }
    private DoubleProperty radiusXProperty() { return ellipse.radiusXProperty(); }
    private DoubleProperty radiusYProperty() { return ellipse.radiusYProperty(); }
}

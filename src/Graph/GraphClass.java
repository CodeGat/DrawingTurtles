package Graph;

import javafx.event.EventTarget;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Shape;

import java.util.Comparator;

public class GraphClass {

    public enum GraphElemType {
        CLASS, LITERAL
    }

    private GraphElemType type;
    private String name;
    private double x, y;

    public GraphClass(Shape type, Label name){
        this.name = name.getText();
        this.type = (type instanceof Ellipse ? GraphElemType.CLASS : GraphElemType.LITERAL);
    }

    public GraphClass(EventTarget element, double x, double y){
        StackPane parent = (StackPane) element;

        this.name = ((Label) parent.getChildren().get(0)).getText();
        this.type = (parent.getChildren().get(1) instanceof Ellipse ? GraphElemType.CLASS : GraphElemType.LITERAL);
        this.x = x;
        this.y = y;
    }

    public String getName() {
        return name;
    }

    public GraphElemType getType() {
        return type;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }
}

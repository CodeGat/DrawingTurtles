package Graph;

import javafx.event.EventTarget;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;

public class GraphClass {

    public enum GraphElemType {
        CLASS, LITERAL
    }

    private GraphElemType type;
    private String name;
    private double x, y;

    public GraphClass(Shape type, Text name){
        this.name = name.getText();
        this.type = (type instanceof Ellipse ? GraphElemType.CLASS : GraphElemType.LITERAL);
    }

    public GraphClass(EventTarget element, double x, double y){
        StackPane parent = (StackPane) element;

        this.name = ((Text) parent.getChildren().get(1)).getText();
        this.type = (parent.getChildren().get(0) instanceof Ellipse ? GraphElemType.CLASS : GraphElemType.LITERAL);
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

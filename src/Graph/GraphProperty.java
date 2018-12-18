package Graph;

import javafx.scene.control.Label;

public class GraphProperty {
    private String name;
    private GraphClass subject, object;

    public GraphProperty (Label name, GraphClass subject, GraphClass object){
        this.name = name.getText();
        this.subject = subject;
        this.object = object;
    }



    public String getName() {
        return name;
    }

    public GraphClass getObject() {
        return object;
    }

    public GraphClass getSubject() {
        return subject;
    }
}

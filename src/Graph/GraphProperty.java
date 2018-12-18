package Graph;

import javafx.scene.text.Text;

public class GraphProperty {
    private String name;
    private GraphClass subject, object;

    public GraphProperty (Text name, GraphClass subject, GraphClass object){
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

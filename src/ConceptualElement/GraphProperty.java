package ConceptualElement;

import javafx.scene.text.Text;

public class GraphProperty {
    private final String name;
    private final GraphClass subject;
    private final GraphClass object;

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

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.Optional;

public class Controller {
    enum Type {
        CLASS, PROPERTY, LITERAL
    }

    public BorderPane root;
    public Button classBtn;
    public Button propBtn;
    public Button literalBtn;
    public Button addPrefixBtn;
    public Button exportTllBtn;
    public Button exportPngBtn;
    public Label statusLbl;
    public Pane drawPane;
    public Label drawStatusLbl;

    private Type selectedType = Type.CLASS;
    private ArrayList<String>     prefixes = new ArrayList<>();
    private ArrayList<Class>      classes  = new ArrayList<>();
    private ArrayList<Class>      literals = new ArrayList<>();
    private ArrayList<Connection> props    = new ArrayList<>();

    @FXML protected void classSelectAction() {
        drawStatusLbl.setText("Class selected");
        selectedType = Type.CLASS;
    }

    @FXML protected void propSelectAction()  {
        drawStatusLbl.setText("Property selected");
        selectedType = Type.PROPERTY;
    }

    @FXML protected void literalSelectAction() {
        drawStatusLbl.setText("Literal selected");
        selectedType = Type.LITERAL;
    }

    @FXML protected void addPrefixAction() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Ontology Prefix");
        dialog.setHeaderText("Of the form: <prefix name> : <URI prefix>");

        Optional<String> dialogResult = dialog.showAndWait();
        dialogResult.ifPresent(prefix -> {
            if (prefix.matches("[a-z]* : .*")){
                prefixes.add(prefix);
                System.out.println("Added");
            } else {
                System.out.println("Not Added");
                showPrefixMalformedAlert();
            }
        });
    }

    @FXML protected void exportTtlAction() {}
    @FXML protected void exportPngAction() {}

    @FXML protected void addElementAction(MouseEvent mouseEvent) {
        if (selectedType == Type.CLASS){
            StackPane compiledElement = new StackPane();
            compiledElement.setLayoutX(mouseEvent.getX());
            compiledElement.setLayoutY(mouseEvent.getY());

            Ellipse   elementType     = new Ellipse();
            elementType.setCenterX(mouseEvent.getX());
            elementType.setCenterY(mouseEvent.getY());
            elementType.setRadiusX(100);
            elementType.setRadiusY(50);
            elementType.setFill(Color.TRANSPARENT);
            elementType.setStroke(Color.BLACK);
            Label elementName = showNameElementDialog();

            if (elementName != null) {
                compiledElement.getChildren().addAll(elementType, elementName);
                drawPane.getChildren().add(compiledElement);
            }

        } else if (selectedType == Type.LITERAL){ // TODO: 13/12/2018 Set dynamic height based on text, draggable, etc.
            StackPane compiledElement = new StackPane();
            compiledElement.setLayoutX(mouseEvent.getX());
            compiledElement.setLayoutY(mouseEvent.getY());

            Rectangle elementType = new Rectangle();
            elementType.setWidth(100);
            elementType.setHeight(70);
            elementType.setFill(Color.TRANSPARENT);
            elementType.setStroke(Color.BLACK);

            Label elementName = showNameElementDialog();

            if (elementName != null) {
                compiledElement.getChildren().addAll(elementType, elementName);
                drawPane.getChildren().add(compiledElement);
            }

        } else if (selectedType == Type.PROPERTY) {
            System.out.println("A Property with nothing.");
        }
    }

    private Label showNameElementDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setGraphic(null);
        dialog.setTitle("Add Ontology Element");
        dialog.setHeaderText("Can be set as defined in Turtle Syntax");

        Optional<String> optDialogResult = dialog.showAndWait();

        if (optDialogResult.isPresent()) {
            String diagResult = optDialogResult.get();
            classes.add(new Class(Class.Type.CLASS, diagResult));
            return new Label(diagResult);
        }
        return null;
    }

    private void showPrefixMalformedAlert() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText("Prefix was not of the form:\n" +
                "\"<prefix name> : <URI prefix>\"\n" +
                "The malformed prefix was discarded, try again.\n" +
                "Example: \"foaf : http://xmlns.com/foaf/0.1/\"");
        alert.showAndWait();
    }
}

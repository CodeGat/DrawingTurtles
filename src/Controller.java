import javafx.event.EventTarget;
import javafx.fxml.FXML;
import javafx.scene.Node;
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
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

public class Controller {
    enum Type { CLASS, PROPERTY, LITERAL }

    public BorderPane root;
    public Button classBtn;
    public Button propBtn;
    public Button literalBtn;
    public Button addPrefixBtn;
    public Button exportTllBtn;
    public Button exportPngBtn;
    public Label  statusLbl;
    public Pane   drawPane;
    public Label  drawStatusLbl;

    private Type selectedType = Type.CLASS;
    private ArrayList<String>    prefixes = new ArrayList<>();
    private ArrayList<StackPane> elements = new ArrayList<>();

    // Related to making a connection between things, can be classified
    private double propSrcX, propSrcY, propDestX, propDestY;
    public static EventTarget propSrcNode, propDestNode;
    private boolean srcClick = true;

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
            if (prefix.matches("[a-z]*\\s*:\\s*.*")) prefixes.add(prefix);
            else showPrefixMalformedAlert();
        });
    }

    @FXML protected void exportTtlAction() {
        /// TODO: 14/12/2018 make sure the elements array is nullified, or use a Set
        for (Node child : drawPane.getChildren()) {
            elements.add((StackPane) child);
        }

        File saveFile = showSaveFileDialog();
        if (saveFile != null){
            String ttl = Converter.convertGraphToTtlString(prefixes, elements);
            try {
                statusLbl.setText(saveFile.createNewFile() ? "File saved." : "File not saved.");
                FileWriter writer = new FileWriter(saveFile);
                writer.write(ttl);
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else statusLbl.setText("File save cancelled.");
    }

    private File showSaveFileDialog() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialFileName("ontology.ttl");
        fileChooser.setTitle("Save Turtle As");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        return fileChooser.showSaveDialog(root.getScene().getWindow());
    }

    @FXML protected void exportPngAction() {}

    @FXML protected void addElementAction(MouseEvent mouseEvent) {
        if (mouseEvent.isStillSincePress() && selectedType == Type.CLASS){
            addClassSubaction(mouseEvent);
        } else if (mouseEvent.isStillSincePress() && selectedType == Type.LITERAL){
            addLiteralSubaction(mouseEvent);
        } else if (selectedType == Type.PROPERTY) {
            addPropertySubaction(mouseEvent);
        }
    }

    private void addLiteralSubaction(MouseEvent mouseEvent){
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
    }

    private void addClassSubaction(MouseEvent mouseEvent){
        StackPane compiledElement = new StackPane();
        compiledElement.setLayoutX(mouseEvent.getX());
        compiledElement.setLayoutY(mouseEvent.getY());

        Ellipse elementType = new Ellipse();
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
    }

    // TODO: 15/12/2018 Check if eventTarget is the drawPane, show some feedback on first click
    private void addPropertySubaction(MouseEvent mouseEvent){
        if (srcClick){
            propSrcX = mouseEvent.getX();
            propSrcY = mouseEvent.getY();
            propSrcNode = ((Node)mouseEvent.getTarget()).getParent();
            srcClick = false;
            System.out.println("In first click: " + propSrcNode.toString());
        } else {
            propDestX = mouseEvent.getX();
            propDestY = mouseEvent.getY();

            StackPane compiledProperty = new StackPane();
            compiledProperty.setLayoutX(propSrcX < propDestX ? propSrcX : propDestX);
            compiledProperty.setLayoutY(propSrcY < propDestY ? propSrcY : propDestY);

            Line propertyLine = new Line();
            propertyLine.setStartX(propSrcX);
            propertyLine.setStartY(propSrcY);
            propertyLine.setEndX(mouseEvent.getX());
            propertyLine.setEndY(mouseEvent.getY());

            Label propertyName = showNameElementDialog();

            if (propertyName != null){
                compiledProperty.getChildren().addAll(propertyLine, propertyName);
                drawPane.getChildren().add(compiledProperty);
            }

            propDestNode = ((Node)mouseEvent.getTarget()).getParent();
            srcClick = true;
            System.out.println("In second click: " + propDestNode.toString());
        }
    }

    private Label showNameElementDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setGraphic(null);
        dialog.setTitle("Add Ontology Element");
        dialog.setHeaderText("Can be set as defined in Turtle Syntax");

        Optional<String> optDialogResult = dialog.showAndWait();
        return optDialogResult.map(Label::new).orElse(null);
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

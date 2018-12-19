import ConceptualElement.GraphClass;
import ConceptualElement.GraphProperty;
import Graph.Arrow;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventTarget;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

// TODO: 19/12/2018 Make ": URI" a valid prefix
public class Controller {
    enum Type { CLASS, PROPERTY, LITERAL }

    public BorderPane root;
    public Button classBtn;
    public Button propBtn;
    public Button literalBtn;
    public Button addPrefixBtn;
    public Button exportTllBtn;
    public Button exportPngBtn;
    public Button instrBtn;
    public Label  statusLbl;
    public Pane   drawPane;
    public Label  drawStatusLbl;

    private Type selectedType = Type.CLASS;
    private final ArrayList<String>    prefixes = new ArrayList<>();
    private final ArrayList<GraphProperty> properties = new ArrayList<>();
    private final ArrayList<GraphClass> classes = new ArrayList<>();

    private GraphClass sub;
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
        File saveFile = showSaveFileDialog(
                "ontology.ttl",
                "Save Turtle Ontology As",
                null
        );
        if (saveFile != null){
            String ttl = Converter.convertGraphToTtlString(prefixes, classes, properties);
            try {
                FileWriter writer = new FileWriter(saveFile);
                writer.write(ttl);
                writer.close();
                statusLbl.setText("File saved.");
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else statusLbl.setText("File save cancelled.");
    }

    @FXML protected void exportPngAction() {
        File saveFile = showSaveFileDialog(
                "ontology.png",
                "Save ConceptualElement Image As",
                new FileChooser.ExtensionFilter("png files (*.png)", "*.png")
        );
        if (saveFile != null){
            try {
                WritableImage writableImage = drawPane.snapshot(new SnapshotParameters(), null);
                RenderedImage renderedImage = SwingFXUtils.fromFXImage(writableImage, null);
                ImageIO.write(renderedImage, "png", saveFile);
                statusLbl.setText("File saved.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else statusLbl.setText("Image save cancelled.");
    }

    @FXML protected void addElementAction(MouseEvent mouseEvent) {
        if (mouseEvent.isStillSincePress() && selectedType == Type.CLASS){
            addClassSubaction(mouseEvent);
        } else if (mouseEvent.isStillSincePress() && selectedType == Type.LITERAL){
            addLiteralSubaction(mouseEvent);
        } else if (selectedType == Type.PROPERTY) {
            addPropertySubaction(mouseEvent);
        }
    }

    @FXML protected void showInstructionsAction() {
        showInstructionsAlert();
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

        Text elementName = showNameElementDialog();

        if (elementName != null) {
            compiledElement.getChildren().addAll(elementType, elementName);
            drawPane.getChildren().add(compiledElement);
            classes.add(new GraphClass(elementType, elementName));
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

        Text elementName = showNameElementDialog();

        if (elementName != null) {
            compiledElement.getChildren().addAll(elementType, elementName);
            drawPane.getChildren().add(compiledElement);
            classes.add(new GraphClass(elementType, elementName));
        }
    }

    private void addPropertySubaction(MouseEvent mouseEvent){
        EventTarget parent = ((Node) mouseEvent.getTarget()).getParent();
        boolean isInsideElement = !(parent instanceof BorderPane);

        if (srcClick && isInsideElement){
            sub = new GraphClass(parent, mouseEvent.getX(), mouseEvent.getY());
            srcClick = false;
            statusLbl.setText("Subject selected. Click another element for the Object.");

        } else if (isInsideElement) {
            GraphClass obj = new GraphClass(parent, mouseEvent.getX(), mouseEvent.getY());

            StackPane compiledProperty = new StackPane();
            compiledProperty.setLayoutX(sub.getX() < obj.getX() ? sub.getX() : obj.getX());
            compiledProperty.setLayoutY(sub.getY() < obj.getY() ? sub.getY() : obj.getY());

            Arrow propertyArrow = new Arrow();
            propertyArrow.setStartX(sub.getX());
            propertyArrow.setStartY(sub.getY());
            propertyArrow.setEndX(obj.getX());
            propertyArrow.setEndY(obj.getY());

            Text propertyName0 = showNameElementDialog();

            if (propertyName0 != null){
                Label propertyName = new Label(propertyName0.getText());
                propertyName.setBackground(new Background(new BackgroundFill(
                        Color.web("F4F4F4"),
                        CornerRadii.EMPTY,
                        Insets.EMPTY
                )));
                compiledProperty.getChildren().addAll(propertyArrow, propertyName);
                drawPane.getChildren().add(compiledProperty);
                properties.add(new GraphProperty(propertyName, sub, obj));
                statusLbl.setText("Property " + propertyName.getText() + " created. ");
            }
            srcClick = true;
        } else {
            srcClick = true;
            statusLbl.setText("Property: Did not select a Class or Literal. Try again.");
        }
    }

    private Text showNameElementDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setGraphic(null);
        dialog.setTitle("Add Ontology Element");
        dialog.setHeaderText("Can be set as defined in Turtle Syntax");

        Optional<String> optDialogResult = dialog.showAndWait();
        return optDialogResult.map(Text::new).orElse(null);
    }

    private File showSaveFileDialog(String fileName, String windowTitle, FileChooser.ExtensionFilter extensionFilter) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialFileName(fileName);
        fileChooser.setTitle(windowTitle);
        fileChooser.setSelectedExtensionFilter(extensionFilter);
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        return fileChooser.showSaveDialog(root.getScene().getWindow());
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

    private void showInstructionsAlert() {
        Alert instrDialog = new Alert(Alert.AlertType.INFORMATION);
        instrDialog.setTitle("Instructions on using Drawing Turtles");
        instrDialog.setHeaderText(null);
        instrDialog.setContentText(
                "How to use Drawing Turtles:\nClick once on the button corresponding to the graph element you want to" +
                        " add to the canvas, then click somewhere valid on the canvas. Add a name (even in .ttl synta" +
                        "x!) and the item will be created in that position. \nIn regards to the Property button, you " +
                        "must click on a valid (already existing) element in the graph as the subject, and then anoth" +
                        "er as the object. If you click on something that is not a Class or Literal, you will need to" +
                        " click the subject-object pair again. \n"
        );

        instrDialog.showAndWait();
    }
}

import ConceptualElement.GraphClass;
import ConceptualElement.GraphProperty;
import Graph.Arrow;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventTarget;
import javafx.fxml.FXML;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
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
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javax.imageio.ImageIO;
import java.awt.image.RenderedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Controller {
    enum Type { CLASS, PROPERTY, LITERAL }

    private static final Logger LOGGER = Logger.getLogger(Controller.class.getName());

    public BorderPane root;
    public Button classBtn;
    public Button propBtn;
    public Button literalBtn;
    public Button addPrefixBtn;
    public Button savePrefixBtn;
    public Button loadPrefixBtn;
    public Button showPrefixBtn;
    public Button clearPrefixBtn;
    public Button saveGraphBtn;
    public Button loadGraphBtn;
    public Button exportTllBtn;
    public Button exportPngBtn;
    public Button instrBtn;
    public Label  statusLbl;
    public Pane   drawPane;
    public Label  drawStatusLbl;

    private Type selectedType = Type.CLASS;
    private final ArrayList<String>        prefixes   = new ArrayList<>();
    private final ArrayList<GraphProperty> properties = new ArrayList<>();
    private final ArrayList<GraphClass>    classes    = new ArrayList<>();

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
        String prefixResult = showAddPrefixesDialog();

        if (prefixResult == null) return;
        String[] prefixList = prefixResult.split(", ");

        for (String prefix : prefixList){
            if (prefix.matches("[a-z]* : .*")) prefixes.add(prefix);
            else showPrefixMalformedAlert(prefix);
        }
    }

    @FXML protected void savePrefixAction(){
        File saveFile = showSaveFileDialog(
                "prefixes.txt",
                "Save Prefixes",
                new FileChooser.ExtensionFilter("Text Files (*.txt)", "*.txt")
        );
        if (saveFile != null){
            StringBuilder prefixesToSave = new StringBuilder();
            for (String prefix : prefixes)
                prefixesToSave.append(prefix).append("\n");
            prefixesToSave.deleteCharAt(prefixesToSave.length() - 1);

            try {
                FileWriter writer = new FileWriter(saveFile);
                writer.write(prefixesToSave.toString());
                writer.flush();
                writer.close();
                statusLbl.setText("Prefixes saved to file. ");
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "failed to save prefixes: ", e);
            }
        } else statusLbl.setText("Prefixes save cancelled. ");
    }

    @FXML protected void loadPrefixAction(){
        File loadFile = showLoadFileDialog("Load Prefixes");

        if (loadFile != null){
            try (FileReader reader = new FileReader(loadFile)){
                char[] rawPrefixes = new char[10000];
                if (reader.read(rawPrefixes) == 0) {
                    statusLbl.setText("Read failed: nothing in file.");
                    LOGGER.warning("Nothing in prefix file. ");
                }
                String[] strPrefixes = new String(rawPrefixes).trim().split("\\n");
                for (String strPrefix : strPrefixes) if (!prefixes.contains(strPrefix)) prefixes.add(strPrefix);
                statusLbl.setText("Prefixes loaded from file. ");
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Loading prefixes failed: ", e);
            }
        } else statusLbl.setText("Prefix Load cancelled.");

    }

    @FXML protected void showPrefixAction() {
        showPrefixesAlert();
    }

    @FXML protected void clearPrefixAction() {
        prefixes.clear();
        statusLbl.setText("Prefixes cleared. ");
    }

    @FXML protected void saveGraphAction() {
        File saveFile = showSaveFileDialog("graph.gat", "Save Graph As", null);
        if (saveFile != null){
            String filetext = traverseCanvas();
            try {
                FileWriter writer = new FileWriter(saveFile);
                writer.write(filetext);
                writer.flush();
                writer.close();
                statusLbl.setText("File saved.");
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "failed to save graph: ", e);
            }

        } else statusLbl.setText("File save cancelled.");
    }

    private String traverseCanvas() {
        StringBuilder result = new StringBuilder();

        for (Node compiledElement : drawPane.getChildren()){
            ObservableList<Node> subelements = ((StackPane) compiledElement).getChildrenUnmodifiable();
            result.append("[");
            if (subelements.get(0) instanceof Ellipse){
                Ellipse e = (Ellipse) subelements.get(0);
                String shapeInfo = "E"+ e.getCenterX() + "|" + e.getCenterY() + "|" + e.getRadiusX() + "|" +
                        e.getRadiusY() + "|" + e.getFill().toString();
                String shapeName = "=" + ((Text) subelements.get(1)).getText();
                result.append(shapeInfo).append(shapeName);
            } else if (subelements.get(0) instanceof Rectangle){
                Rectangle r = (Rectangle) subelements.get(0);

                String shapeInfo = "R" + r.getParent().getLayoutX() + "|" + r.getParent().getLayoutY() + "|" +
                        r.getWidth() + "|" + r.getHeight() + "|" + r.getFill().toString();
                String shapeName = "=" + ((Text) subelements.get(1)).getText();
                result.append(shapeInfo).append(shapeName);
            } else if (subelements.get(0) instanceof Arrow){
                Arrow a = (Arrow) subelements.get(0);
                Line  l = a.getLine();
                String shapeInfo = "A" + l.getStartX() + "|" + l.getStartY() + "|" + l.getEndX() + "|" + l.getEndY();
                String shapeName = "=" + ((Label) subelements.get(1)).getText();
                result.append(shapeInfo).append(shapeName);
            } else statusLbl.setText("TRAVERSAL FAILED. Berate the programmer for not generifying the traversal algorithm.");

            result.append("]");
        }

        return result.toString();
    }

    @FXML protected void loadGraphAction() {
        File loadFile = showLoadFileDialog("Load Graph File");
        if (loadFile != null){
            drawPane.getChildren().clear();
            prefixes.clear();
            classes.clear();
            properties.clear();

            try (FileReader reader = new FileReader(loadFile)){
                char[] rawGraph = new char[10000];
                if (reader.read(rawGraph) == 0 ) {
                    statusLbl.setText("Read failed: nothing in graph file.");
                    LOGGER.warning("Nothing in graph file.");
                }
                bindGraph(new String(rawGraph));
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Loading the graph failed: ", e);
            }
        } else statusLbl.setText("Graph load cancelled.");
    }

    private void bindGraph(String graph) {
        String[] elements = Arrays.stream(graph.split("]\\[|\\[|]"))
                .filter(s -> !s.equals(""))
                .toArray(String[]::new);

        for (String element : elements){
            if      (element.charAt(0) == 'R') bindLiteral(element);
            else if (element.charAt(0) == 'E') bindClass(element);
            else if (element.charAt(0) == 'A') bindProperty(element);
        }
    }

    private void bindLiteral(String lit) {
        String[] litElements = lit.split("=");
        String[] litInfo = litElements[0].substring(1).split("\\|");
        String   litName = litElements[1];

        StackPane compiledLit = new StackPane();
        compiledLit.setLayoutX(Double.valueOf(litInfo[0]));
        compiledLit.setLayoutY(Double.valueOf(litInfo[1]));

        Rectangle rect = new Rectangle();
        rect.setWidth(Double.valueOf(litInfo[2]));
        rect.setHeight(Double.valueOf(litInfo[3]));
        rect.setFill(Color.web(litInfo[4]));
        rect.setStroke(Color.BLACK);

        Text name = new Text(litName);

        compiledLit.getChildren().addAll(rect, name);
        drawPane.getChildren().add(compiledLit);
        classes.add(new GraphClass(rect, name));
    }

    private void bindClass(String cls) {
        String[] clsElements = cls.split("=");
        String[] clsInfo     = clsElements[0].substring(1).split("\\|");
        String   clsName     = clsElements[1];

        StackPane compiledCls = new StackPane();
        compiledCls.setLayoutX(Double.valueOf(clsInfo[0]));
        compiledCls.setLayoutY(Double.valueOf(clsInfo[1]));

        Ellipse ellipse = new Ellipse();
        ellipse.setCenterX(Double.valueOf(clsInfo[0]));
        ellipse.setCenterY(Double.valueOf(clsInfo[1]));
        ellipse.setRadiusX(Double.valueOf(clsInfo[2]));
        ellipse.setRadiusY(Double.valueOf(clsInfo[3]));
        ellipse.setFill(Color.web(clsInfo[4]));
        ellipse.setStroke(Color.BLACK);

        Text name = new Text(clsName);

        compiledCls.getChildren().addAll(ellipse, name);
        drawPane.getChildren().add(compiledCls);
        classes.add(new GraphClass(ellipse, name));
    }

    private void bindProperty(String prop) { //maybe just get parent xy?
        String[] propElements = prop.split("=");
        String[] propInfo     = propElements[0].substring(1).split("\\|");
        String   propName     = propElements[1];
        double sx = Double.valueOf(propInfo[0]);
        double sy = Double.valueOf(propInfo[1]);
        double ex = Double.valueOf(propInfo[2]);
        double ey = Double.valueOf(propInfo[3]);

        StackPane compiledProp = new StackPane();
        compiledProp.setLayoutX(sx < ex ? sx : ex);
        compiledProp.setLayoutY(sy < ey ? sy : ey);

        Arrow arrow = new Arrow();
        arrow.setStartX(sx);
        arrow.setStartY(sy);
        arrow.setEndX(ex);
        arrow.setEndY(ey);

        Label name = new Label(propName);
        name.setBackground(new Background(new BackgroundFill(Color.web("f4f4f4"), CornerRadii.EMPTY, Insets.EMPTY)));

        compiledProp.getChildren().addAll(arrow, name);
        drawPane.getChildren().add(compiledProp);
        properties.add(new GraphProperty(
                name,
                bindClassUnder(sx, sy),
                bindClassUnder(ex, ey))
        );
    }

    private GraphClass bindClassUnder(double x, double y) {
        for (GraphClass klass : classes) {
            Bounds classBounds = klass.getBounds();
            Bounds pointBounds = new BoundingBox(x-1, y-1, 2, 2);

            if (classBounds.intersects(pointBounds)) return klass;
        }
        LOGGER.log(Level.SEVERE, "no class was found within ("+x+", "+y+"), left unbound. ");
        return null;
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
                LOGGER.log(Level.SEVERE, "failed to export to .tll: ", e);
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
                LOGGER.log(Level.SEVERE, "failed to export to .png: ", e);
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

        Text elementName = showNameElementDialog();
        if (elementName == null || elementName.getText().equals("")) return;
        double textWidth = elementName.getBoundsInLocal().getWidth();

        Rectangle elementType = new Rectangle();
        elementType.setHeight(75);
        elementType.setWidth(textWidth > 125 ? textWidth + 15 : 125);
        elementType.setFill(Color.web("f4f4f4"));
        elementType.setStroke(Color.BLACK);

        compiledElement.getChildren().addAll(elementType, elementName);
        drawPane.getChildren().add(compiledElement);
        classes.add(new GraphClass(elementType, elementName));
    }

    private void addClassSubaction(MouseEvent mouseEvent){
        StackPane compiledElement = new StackPane();
        compiledElement.setLayoutX(mouseEvent.getX());
        compiledElement.setLayoutY(mouseEvent.getY());

        Text elementName = showNameElementDialog();
        if (elementName == null || elementName.getText().equals("")) return;
        double textWidth = elementName.getBoundsInLocal().getWidth();

        Ellipse elementType = new Ellipse();
        elementType.setCenterX(mouseEvent.getX());
        elementType.setCenterY(mouseEvent.getY());
        elementType.setRadiusX(textWidth / 2 > 62.5 ? textWidth / 2 + 10 : 62.5);
        elementType.setRadiusY(37.5);
        elementType.setFill(Color.web("f4f4f4"));
        elementType.setStroke(Color.BLACK);

        compiledElement.getChildren().addAll(elementType, elementName);
        drawPane.getChildren().add(compiledElement);
        classes.add(new GraphClass(elementType, elementName));
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
            if (propertyName0 == null){
                srcClick = true;
                return;
            }

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
            srcClick = true;
        } else {
            srcClick = true;
            statusLbl.setText("Property: Did not select a Class or Literal. Try again.");
        }
    }

    private String showAddPrefixesDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Ontology Prefixes");
        dialog.setHeaderText("Of the form: <prefix name> : <URI prefix>\nCan add multiple as comma-seperated values.");

        Optional<String> optPrefixResult = dialog.showAndWait();
        return optPrefixResult.map(String::new).orElse(null);
    }

    private Text showNameElementDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setGraphic(null);
        dialog.setTitle("Add Ontology Element");
        dialog.setHeaderText("Can be set as defined in Turtle Syntax");

        Optional<String> optDialogResult = dialog.showAndWait();
        return optDialogResult.map(Text::new).orElse(null);
    }

    private File showSaveFileDialog(String fileName, String windowTitle, FileChooser.ExtensionFilter extFilter) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialFileName(fileName);
        fileChooser.setTitle(windowTitle);
        fileChooser.setSelectedExtensionFilter(extFilter);
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        return fileChooser.showSaveDialog(root.getScene().getWindow());
    }

    private File showLoadFileDialog(String title){
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        return fileChooser.showOpenDialog(root.getScene().getWindow());
    }

    private void showPrefixMalformedAlert(String badPrefix) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText("Prefix: " + badPrefix + " was not of the form:\n" +
                "\"<prefix name> : <URI prefix>\"\n" +
                "This malformed prefix was discarded, try again.\n" +
                "Example: \"foaf : http://xmlns.com/foaf/0.1/\"");
        alert.showAndWait();
    }

    private void showInstructionsAlert() {
        Alert instrAlert = new Alert(Alert.AlertType.INFORMATION);
        instrAlert.setTitle("Instructions on using Drawing Turtles");
        instrAlert.setHeaderText(null);
        instrAlert.setContentText(
                "How to use Drawing Turtles:\nClick once on the button corresponding to the graph element you want to" +
                        " add to the canvas, then click somewhere valid on the canvas. Add a name (even in .ttl synta" +
                        "x!) and the item will be created in that position. \nIn regards to the Property button, you " +
                        "must click on a valid (already existing) element in the graph as the subject, and then anoth" +
                        "er as the object. If you click on something that is not a Class or Literal, you will need to" +
                        " click the subject-object pair again. \n"
        );

        instrAlert.showAndWait();
    }

    private void showPrefixesAlert() {
        StringBuilder prefixBuilder = new StringBuilder();
        prefixes.forEach(p -> prefixBuilder.append(p).append("\n"));
        String prefixList = prefixBuilder.toString();

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Currently added Prefixes");
        alert.setHeaderText(null);
        alert.setContentText("These are the prefixes that are currently in this project, apart from the basic owl, r" +
                "df, rdfs prefixes: \n" + (prefixList.length() == 0 ? "<none>" : prefixList));
        alert.showAndWait();
    }
}

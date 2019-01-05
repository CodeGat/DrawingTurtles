import Conceptual.Edge;
import Conceptual.OutsideElementException;
import Conceptual.Vertex;
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
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
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

/**
 * The Controller for application.fxml: takes care of actions from the application.
 */
public class Controller {

    /**
     * An enumeration of the types of graph elements.
     */
    enum Type {
        CLASS, PROPERTY, LITERAL
    }

    private static final Logger LOGGER = Logger.getLogger(Controller.class.getName());

    public BorderPane root;
    public Pane drawPane;
    public ScrollPane scrollPane;
    public Button classBtn, propBtn, literalBtn, addPrefixBtn, savePrefixBtn, loadPrefixBtn, showPrefixBtn,
            clearPrefixBtn, saveGraphBtn, loadGraphBtn, exportTllBtn, exportPngBtn, instrBtn;
    public Label  statusLbl, drawStatusLbl;

    private Type selectedType = Type.CLASS;
    private final ArrayList<String> prefixes   = new ArrayList<>();
    private final ArrayList<Edge>   properties = new ArrayList<>();
    private final ArrayList<Vertex> classes    = new ArrayList<>();

    private Arrow arrow;
    private Vertex sub;
    private boolean srcClick = true;

    /**
     * Method invoked on any key press in the main application.
     * @param keyEvent the key that invoked the method.
     */
    @FXML protected void keyPressedAction(KeyEvent keyEvent){
        KeyCode key = keyEvent.getCode();

        if (key == KeyCode.Q) {
            classSelectAction();
        } else if (key == KeyCode.W) {
            literalSelectAction();
        } else if (key == KeyCode.E) {
            propSelectAction();
        } else if (key == KeyCode.S && keyEvent.isControlDown()){
            savePrefixAction();
            saveGraphAction();
        } else if (key == KeyCode.S) {
            saveGraphAction();
        } else if (key == KeyCode.L && keyEvent.isControlDown()){
            loadPrefixAction();
            loadGraphAction();
        } else if (key == KeyCode.L) {
            loadGraphAction();
        } else if (key == KeyCode.P) {
            addPrefixAction();
        } else if (key == KeyCode.X && keyEvent.isControlDown()){
            exportTtlAction();
            exportPngAction();
        } else if (key == KeyCode.X){
            exportTtlAction();
        }
    }

    /**
     * On clicking the Class button, the application knows that the next click will create a Class.
     */
    @FXML protected void classSelectAction() {
        drawStatusLbl.setText("Class selected");
        selectedType = Type.CLASS;
    }

    /**
     * On clicking the Property button, the application knows that the next click will make the subject of the Property.
     */
    @FXML protected void propSelectAction()  {
        drawStatusLbl.setText("Property selected");
        srcClick = true;
        selectedType = Type.PROPERTY;
    }

    /**
     * On clicking the Literal button, the application knows that the next click will create a Literal.
     */
    @FXML protected void literalSelectAction() {
        drawStatusLbl.setText("Literal selected");
        selectedType = Type.LITERAL;
    }

    /**
     * On clicking the 'Add Prefix' button, adds prefixes to the arraylist of existing prefixes unless malformed.
     */
    @FXML protected void addPrefixAction() {
        String prefixResult = showAddPrefixesDialog();

        if (prefixResult == null) return;
        String[] prefixList = prefixResult.split(", ");

        for (String prefix : prefixList){
            if (prefix.matches("[a-z]* : .*")) prefixes.add(prefix);
            else showPrefixMalformedAlert(prefix);
        }
    }

    /**
     * On clicking the 'Save Prefix' button, attempts to write existing prefixes to a user-specified .txt file.
     */
    @FXML protected void savePrefixAction() {
        File saveFile = showSaveFileDialog(
                "prefixes.txt",
                "Save Prefixes",
                new FileChooser.ExtensionFilter("Text Files (*.txt)", "*.txt")
        );
        if (saveFile != null && prefixes.size() != 0) {
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

    /**
     * On clicking the 'Load Prefix' button, attempts to load prefixes from a user-specified .txt file.
     */
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

    /**
     * on clicking 'Show Prefixes' button, show existing prefixes in an alert.
     */
    @FXML protected void showPrefixAction() {
        showPrefixesAlert();
    }

    /**
     * on clicking 'Clear Prefixes' button, removes existing user prefixes, excepting the base ones (owl, rdf, rdfs).
     */
    @FXML protected void clearPrefixAction() {
        prefixes.clear();
        statusLbl.setText("Prefixes cleared. ");
    }

    /**
     * on clicking 'Save Graph' button, attempt to traverse the graph and save a bespoke serialization of the graph to
     *   a user-specified .gat file. That's a Graph Accessor Type format, not just my name...
     */
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

    /**
     * Traverses the graph through the children of the canvas (the drawPane), in order of creation, and gives the
     *    canvas size.
     * There is no need for recursive definitions, as the tree is a shallow one with depth at most 3.
     * @return a bespoke string serialization of the children of the canvas (the elements of the graph).
     */
    private String traverseCanvas() {
        StringBuilder result = new StringBuilder();

        String canvasSize = "G" + drawPane.getWidth() + "x" + drawPane.getHeight();
        result.append(canvasSize);

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
                String shapeInfo = "A" + a.getStartX() + "|" + a.getStartY() + "|" + a.getEndX() + "|" + a.getEndY();
                String shapeName = "=" + ((Label) subelements.get(1)).getText();
                result.append(shapeInfo).append(shapeName);
            } else statusLbl.setText("TRAVERSAL FAILED. Berate the programmer for not generifying the algorithm.");

            result.append("]");
        }

        return result.toString();
    }

    /**
     * On clicking the 'Load Graph' button, clears the canvas and attempts to deserialize the user-specified .gat file
     *   into elements of a graph. It then binds the visual elements into meaningful java-friendly elements.
     */
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

    /**
     * Splits the output of the .gat file into it's respective elements and attempts to bind them.
     * @param graph the raw .gat file data.
     */
    private void bindGraph(String graph) {
        String[] elements = Arrays.stream(graph.split("]\\[|\\[|]"))
                .filter(s -> !s.equals(""))
                .toArray(String[]::new);

        for (String element : elements){
            if      (element.charAt(0) == 'R') bindLiteral(element);
            else if (element.charAt(0) == 'E') bindClass(element);
            else if (element.charAt(0) == 'A') bindProperty(element);
            else if (element.charAt(0) == 'G') bindCanvas(element);
        }
    }

    private void bindCanvas(String size) {
        String[] canvasSize = size.split("[Gx]");
        double width = Double.valueOf(canvasSize[1]);
        double height = Double.valueOf(canvasSize[2]);

        drawPane.setMinSize(width, height);
        drawPane.setPrefSize(width, height);
    }

    /**
     * Binds a literal into both a human-friendly visual element of the graph, and a java-friendly Literal GraphClass.
     * Helper method of {@link #bindGraph(String)}.
     * @param lit the .gat String serialization of a Literal.
     */
    private void bindLiteral(String lit) {
        String[] litElements = lit.split("=");
        String[] litInfo = litElements[0].substring(1).split("\\|");
        String   litName = litElements[1];
        double   x = Double.valueOf(litInfo[0]);
        double   y = Double.valueOf(litInfo[1]);
        double   w = Double.valueOf(litInfo[2]);
        double   h = Double.valueOf(litInfo[3]);
        Color    col = Color.web(litInfo[4]);

        resizeEdgeOfCanvas(x, y);

        StackPane compiledLit = new StackPane();
        compiledLit.setLayoutX(x);
        compiledLit.setLayoutY(y);

        Rectangle rect = new Rectangle();
        rect.setWidth(w);
        rect.setHeight(h);
        rect.setFill(col);
        rect.setStroke(Color.BLACK);

        Text name = new Text(litName);

        compiledLit.getChildren().addAll(rect, name);
        drawPane.getChildren().add(compiledLit);
        classes.add(new Vertex(rect, name));
    }

    /**
     * Binds a class into both a human-friendly visual element of the graph, and a java-friendly Class GraphClass.
     * Helper Method of {@link #bindGraph(String)}.
     * @param cls the .gat String serialization of a Class.
     */
    private void bindClass(String cls) {
        String[] clsElements = cls.split("=");
        String[] clsInfo     = clsElements[0].substring(1).split("\\|");
        String   clsName     = clsElements[1];
        double   x = Double.valueOf(clsInfo[0]);
        double   y = Double.valueOf(clsInfo[1]);
        double   rx = Double.valueOf(clsInfo[2]);
        double   ry = Double.valueOf(clsInfo[3]);
        Color    col = Color.web(clsInfo[4]);

        resizeEdgeOfCanvas(x, y);

        StackPane compiledCls = new StackPane();
        compiledCls.setLayoutX(x);
        compiledCls.setLayoutY(y);

        Ellipse ellipse = new Ellipse();
        ellipse.setCenterX(x);
        ellipse.setCenterY(y);
        ellipse.setRadiusX(rx);
        ellipse.setRadiusY(ry);
        ellipse.setFill(col);
        ellipse.setStroke(Color.BLACK);

        Text name = new Text(clsName);

        compiledCls.getChildren().addAll(ellipse, name);
        drawPane.getChildren().add(compiledCls);
        classes.add(new Vertex(ellipse, name));
    }

    /**
     * Creates a human-friendly graph property arrow, and binds a java-friendly GraphProperty.
     * Helper Method of {@link #bindGraph(String)}.
     * @param prop the .gat String serialization of a Property.
     */
    private void bindProperty(String prop) {
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
        properties.add(new Edge(
                name,
                bindClassUnder(sx, sy),
                bindClassUnder(ex, ey))
        );
    }

    /**
     * Ties a coordinate to the class or literal below it, used in finding the subject and object of a property given
     *    the general start and end coordinates of the arrow.
     * Helper Method of {@link #bindProperty(String)} and in extension {@link #bindGraph(String)}.
     * @param x a x coordinate, given some leeway in the Bounds.
     * @param y a y coordinate, given some leeway in the Bounds.
     * @return the class or literal under the (x, y) coordinate, or null otherwise.
     */
    private Vertex bindClassUnder(double x, double y) {
        for (Vertex klass : classes) {
            Bounds classBounds = klass.getBounds();
            Bounds pointBounds = new BoundingBox(x-1, y-1, 2, 2);

            if (classBounds.intersects(pointBounds)) return klass;
        }
        LOGGER.log(Level.SEVERE, "no class was found within ("+x+", "+y+"), left unbound. ");
        return null;
    }

    /**
     * On clicking 'Export as .ttl' button, attempt to write a graph-to-.ttl string to a user-specified .ttl file.
     */
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

    /**
     * On clicking "Export as .png' button, attempt to save the canvas to a user-specified .png file.
     */
    @FXML protected void exportPngAction() {
        File saveFile = showSaveFileDialog(
                "ontology.png",
                "Save Conceptual Image As",
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

    /**
     * On clicking the canvas, begin to draw the specified element to the canvas.
     * @param mouseEvent the event that triggered the method.
     */
    @FXML protected void addElementAction(MouseEvent mouseEvent) {
        if (selectedType == Type.CLASS){
            addClassSubaction(mouseEvent);
        } else if (selectedType == Type.LITERAL){
            addLiteralSubaction(mouseEvent);
        } else if (selectedType == Type.PROPERTY && srcClick) {
            addSubjectOfProperty(mouseEvent);
        } else if (selectedType == Type.PROPERTY){
            addObjectOfProperty(mouseEvent);
        }
    }

    /**
     * Keeps the arrow in line with the mouse as the user clicks on the target.
     * @param mouseEvent the event that triggered the method.
     */
    @FXML protected void moveArrowAction(MouseEvent mouseEvent) {
        if (arrow == null) return;
        arrow.setEndX(mouseEvent.getX());
        arrow.setEndY(mouseEvent.getY());
    }

    /**
     * Defines the Object, or range, of the property, and creates the association between the Subject and Object.
     * @param mouseEvent the second click on the canvas when 'Property' is selected.
     */
    private void addObjectOfProperty(MouseEvent mouseEvent) {
        EventTarget parent = ((Node) mouseEvent.getTarget()).getParent();
        Vertex obj;

        try {
            obj = new Vertex(parent, mouseEvent.getX(), mouseEvent.getY());
        } catch (OutsideElementException e){
            LOGGER.info("Outside Element: " + mouseEvent.toString());
            statusLbl.setText("Outside any class or literal, property creation cancelled. ");
            drawPane.getChildren().remove(arrow);
            sub = null;
            arrow = null;
            srcClick = true;
            return;
        }

        arrow.setEndX(obj.getX());
        arrow.setEndY(obj.getY());

        StackPane compiledProperty = new StackPane();
        compiledProperty.setLayoutX(sub.getX() < obj.getX() ? sub.getX() : obj.getX());
        compiledProperty.setLayoutY(sub.getY() < obj.getY() ? sub.getY() : obj.getY());

        Text propertyName0 = showNameElementDialog();
        if (propertyName0 == null){
            drawPane.getChildren().remove(arrow);
            statusLbl.setText("Property creation cancelled. ");
            sub = null;
            arrow = null;
            srcClick = true;
            return;
        }

        Label propertyName = new Label(propertyName0.getText());
        propertyName.setBackground(new Background(new BackgroundFill(
                Color.web("F4F4F4"),
                CornerRadii.EMPTY,
                Insets.EMPTY
        )));

        compiledProperty.getChildren().addAll(arrow, propertyName);
        drawPane.getChildren().add(compiledProperty);
        properties.add(new Edge(propertyName, sub, obj));
        statusLbl.setText("Property " + propertyName.getText() + " created. ");
        sub = null;
        arrow = null;
        srcClick = true;
    }

    /**
     * Defines the Subject, or domain, of the property.
     * @param mouseEvent the first click on the canvas when .
     */
    private void addSubjectOfProperty(MouseEvent mouseEvent) {
        EventTarget parent = ((Node) mouseEvent.getTarget()).getParent();

        try {
            sub = new Vertex(parent, mouseEvent.getX(), mouseEvent.getY());
        } catch (OutsideElementException e){
            LOGGER.warning("Outside Element: " + mouseEvent.toString());
            sub = null;
            return;
        }

        arrow = new Arrow();
        arrow.setMouseTransparent(true);
        arrow.setStartX(sub.getX());
        arrow.setStartY(sub.getY());
        arrow.setEndX(sub.getX());
        arrow.setEndY(sub.getY());

        drawPane.getChildren().add(arrow);
        srcClick = false;
        statusLbl.setText("Subject selected. Click another element for the Object.");
    }

    /**
     * On clicking 'Instructions' button, show the instructions...
     */
    @FXML protected void showInstructionsAction() {
        showInstructionsAlert();
    }

    /**
     * Draw a Literal and it's user-specified name to the canvas, also creating the GraphClass representation of it.
     * Helper Method of addElementAction(...).
     * @param mouseEvent the click to the canvas.
     */
    private void addLiteralSubaction(MouseEvent mouseEvent){
        resizeEdgeOfCanvas(mouseEvent.getX(), mouseEvent.getY());

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
        classes.add(new Vertex(elementType, elementName));
    }

    /**
     * Extend the canvas width and height if any new element gets to close to the bounds of the drawPane canvas.
     * @param x x coordinate to check if we need to extend the width of the canvas.
     * @param y y coordinate to check if we need to extend the height of the canvas.
     */
    private void resizeEdgeOfCanvas(double x, double y) {
        double height = drawPane.getHeight();
        double width = drawPane.getWidth();

        if (x > width - 150 && y > height - 150) {
            drawPane.setPrefSize(x + 300, y + 300);
        } else if (x > width - 150) {
            drawPane.setPrefWidth(x + 300);
        } else if (y > height - 150) {
            drawPane.setPrefHeight(y + 300);
        }
    }

    /**
     * Draw a Class and it's name to the canvas, and create the GraphClass representation of the element.
     * Helper method of {@link #addElementAction(MouseEvent) Add Element} method.
     * @param mouseEvent the click to the canvas.
     */
    private void addClassSubaction(MouseEvent mouseEvent){
        resizeEdgeOfCanvas(mouseEvent.getX(), mouseEvent.getY());

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
        classes.add(new Vertex(elementType, elementName));
    }

    /**
     * Creates a dialog that allows input of prefixes.
     * @return the prefixes inputted, or null otherwise.
     */
    private String showAddPrefixesDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Ontology Prefixes");
        dialog.setHeaderText("Of the form: <prefix name> : <URI prefix>\nCan add multiple as comma-seperated values.");

        Optional<String> optPrefixResult = dialog.showAndWait();
        return optPrefixResult.map(String::new).orElse(null);
    }

    /**
     * Creates a dialog that accepts a name of an element.
     * @return the name inputted or null otherwise.
     */
    private Text showNameElementDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setGraphic(null);
        dialog.setTitle("Add Ontology Element");
        dialog.setHeaderText("Can be set as defined in Turtle Syntax");

        Optional<String> optDialogResult = dialog.showAndWait();
        return optDialogResult.map(Text::new).orElse(null);
    }

    /**
     * Creates a save file dialog, prompting the user to select a file to create and/or save data to.
     * @param fileName the default filename the dialog will save the file as.
     * @param windowTitle the title of the dialog.
     * @param extFilter the list of extension filters, for easy access to specific file types.
     * @return the file the user has chosen to save to, or null otherwise.
     */
    private File showSaveFileDialog(String fileName, String windowTitle, FileChooser.ExtensionFilter extFilter) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialFileName(fileName);
        fileChooser.setTitle(windowTitle);
        fileChooser.setSelectedExtensionFilter(extFilter);
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        return fileChooser.showSaveDialog(root.getScene().getWindow());
    }

    /**
     * Creates a load file dialog, which prompts the user to load from a specific file.
     * @param title the title of the dialog.
     * @return the file that will be loaded from.
     */
    private File showLoadFileDialog(String title){
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        return fileChooser.showOpenDialog(root.getScene().getWindow());
    }

    /**
     * Creates an alert that notifies the user that the specified prefix is malformed.*
     * @param badPrefix the prefix that doesn't meet the regex criteria.
     */
    private void showPrefixMalformedAlert(String badPrefix) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText("Prefix: " + badPrefix + " was not of the form:\n" +
                "\"<prefix name> : <URI prefix>\"\n" +
                "This malformed prefix was discarded, try again.\n" +
                "Example: \"foaf : http://xmlns.com/foaf/0.1/\"");
        alert.showAndWait();
    }

    /**
     * Creates an instructional alert.
     */
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

    /**
     * Creates an alert that displays the current prefixes.
     */
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

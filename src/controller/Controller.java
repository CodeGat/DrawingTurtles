package controller;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.conceptual.Edge;
import model.conceptual.Vertex;
import model.conceptual.Vertex.OutsideElementException;
import model.conversion.gat.FromGatConverter;
import model.conversion.gat.ToGatConverter;
import model.conversion.rdfxml.RDFXMLGenerator;
import model.graph.Arrow;
import model.conversion.ttl.Converter;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
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
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.RenderedImage;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Controller for application.fxml: takes care of actions from the application.
 */
public final class Controller implements Initializable {
    private static final Logger LOGGER = Logger.getLogger(Controller.class.getName());

    @FXML protected BorderPane root;
    @FXML protected Pane drawPane;
    @FXML protected ScrollPane scrollPane;
    @FXML protected Button prefixBtn, saveGraphBtn, loadGraphBtn, exportTllBtn, exportPngBtn, eatCsvBtn, rdfXmlBtn,
            instrBtn, optionsBtn;
    @FXML ImageView ttlPrefImv, ttlGraphImv, instPrefImv, instGraphImv, instCsvImv;
    @FXML protected Label  statusLbl;
    private ArrayList<Boolean> config = new ArrayList<>(Arrays.asList(false, false, false));

    private Map<String, String>     prefixes   = new HashMap<>();
    private final ArrayList<Edge>   properties = new ArrayList<>();
    private final ArrayList<Vertex> classes    = new ArrayList<>();

    private Arrow arrow;
    private Vertex subject;
    private boolean srcClick = true;

    private List<CSVRecord> csv;
    private Map<String, Integer> headers;

    private BooleanProperty prefixesInspected = new SimpleBooleanProperty(false);
    private BooleanProperty graphCreated = new SimpleBooleanProperty(false);
    private BooleanProperty csvIngested = new SimpleBooleanProperty(false);

    @Override public void initialize(URL location, ResourceBundle resources) {
        Image cross = new Image("/view/images/cross.png");
        Image tick  = new Image("/view/images/tick.png");
        ttlPrefImv.setImage(cross);
        ttlGraphImv.setImage(cross);
        instPrefImv.setImage(cross);
        instGraphImv.setImage(cross);
        instCsvImv.setImage(cross);

        prefixesInspected.addListener(((observable, oldValue, newValue) -> {
            if (observable.getValue().booleanValue()){
                ttlPrefImv.setImage(tick);
                instPrefImv.setImage(tick);
            } else {
                ttlPrefImv.setImage(cross);
                instPrefImv.setImage(cross);
            }
        }));

        graphCreated.addListener((observable, oldValue, newValue) -> {
            if (observable.getValue().booleanValue()){
                ttlGraphImv.setImage(tick);
                instGraphImv.setImage(tick);
            } else {
                ttlGraphImv.setImage(cross);
                instGraphImv.setImage(cross);
            }
        });

        csvIngested.addListener(((observable, oldValue, newValue) -> {
            if (observable.getValue().booleanValue()) instCsvImv.setImage(tick);
            else instCsvImv.setImage(cross);
        }));
    }

    /**
     * Method invoked on any key press in the main application.
     * @param keyEvent the key that invoked the method.
     */
    @FXML protected void keyPressedAction(KeyEvent keyEvent) {
        KeyCode key = keyEvent.getCode();

        if (key == KeyCode.X && keyEvent.isControlDown()){
            exportTtlAction();
            exportPngAction();
        } else if (key == KeyCode.S) saveGraphAction();
        else if (key == KeyCode.L) loadGraphAction();
        else if (key == KeyCode.P) showPrefixMenuAction();
        else if (key == KeyCode.X) exportTtlAction();
        else if (key == KeyCode.O) showOptionsAction();
    }

    /**
     * Creates and displays the Window defined in the fxml file, also passing data to a controller C.
     * @param fxml the fxml file in which the layout is defined.
     * @param title the title of the new window.
     * @param data the parameters passed to the Controller.
     * @param <C> a Controller that can pass data to and recieve data from this method (extending
     *           AbstractDataSharingController).
     * @param <T> the type of data passed to and from the Controller.
     * @return the data after it has been modified by the Controller.
     */
    @FXML @SuppressWarnings("unchecked")
    private <C extends AbstractDataSharingController<T>, T> ArrayList<T> showWindow(String fxml, String title, ArrayList<T> data){
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent parent = loader.load();
            C controller = loader.getController();

            Method methodName = controller.getClass().getMethod("setData", ArrayList.class);
            methodName.invoke(controller, data);

            Scene scene = new Scene(parent);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(title);
            stage.setScene(scene);
            stage.setResizable(false);
            stage.showAndWait();

            methodName = controller.getClass().getMethod("getData");
            return (ArrayList<T>) methodName.invoke(controller);
        } catch (IOException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Shows the Prefixes menu, updating the prefixes if they have been modified in the menu.
     */
    @FXML void showPrefixMenuAction() {
        ArrayList<Map<String, String>> data = new ArrayList<>();
        data.add(prefixes);

        ArrayList<Map<String, String>> updatedData = showWindow("/view/prefixmenu.fxml", "Prefixes Menu", data);

        if (updatedData != null && updatedData.get(0) != null) prefixes = updatedData.get(0);
        prefixesInspected.setValue(true);
    }

    /**
     * on clicking 'Save Graph' button, attempt to traverse the graph and save a bespoke serialization of the graph to
     *   a user-specified .gat file. That's a Graph Accessor Type format, not just my name...
     */
    @FXML public void saveGraphAction() {
        File saveFile = showSaveFileDialog("graph.gat", "Save Graph As", null);
        if (saveFile != null){
            ToGatConverter converter = new ToGatConverter(
                    drawPane.getWidth(),
                    drawPane.getHeight(),
                    classes, properties
            );
            String filetext = converter.traverseCanvas();
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
     * On clicking the 'Load Graph' button, clears the canvas and attempts to deserialize the user-specified .gat file
     *   into elements of a graph. It then binds the visual elements into meaningful java-friendly elements.
     */
    @FXML public void loadGraphAction() {
        File loadFile = showLoadFileDialog("Load Graph File");
        if (loadFile != null){
            drawPane.getChildren().clear();
            prefixes.clear();
            classes.clear();
            properties.clear();

            try (FileReader reader = new FileReader(loadFile)){
                char[] rawGraph = new char[10000]; //needs to be arbitrary
                if (reader.read(rawGraph) == 0 ) {
                    statusLbl.setText("Read failed: nothing in graph file.");
                    LOGGER.warning("Nothing in graph file.");
                }
                FromGatConverter binder = new FromGatConverter(new String(rawGraph));
                binder.bindGraph();
                classes.addAll(binder.getClasses());
                properties.addAll(binder.getProperties());
                drawPane.setPrefSize(binder.getCanvasWidth(), binder.getCanvasHeight());
                drawPane.getChildren().addAll(binder.getCompiledElements());
                for (StackPane compiledProperty : binder.getCompiledProperties()){
                    drawPane.getChildren().add(compiledProperty);
                    compiledProperty.toBack();
                }
                graphCreated.setValue(true);
                prefixesInspected.setValue(false);
                statusLbl.setText("Graph load successful.");
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Loading the graph failed: ", e);
            }
        } else statusLbl.setText("Graph load cancelled.");
    }

    /**
     * Ties a coordinate to the class or literal below it, used in finding the subject and object of a property given
     *    the general start and end coordinates of the arrow. Also finds the class below a users click.
     * @param x a x coordinate, given some leeway in the Bounds.
     * @param y a y coordinate, given some leeway in the Bounds.
     * @return the class or literal under the (x, y) coordinate, or null otherwise.
     */
    private Vertex findClassUnder(double x, double y) {
        for (Vertex klass : classes) {
            Bounds classBounds = klass.getBounds();
            Bounds pointBounds = new BoundingBox(x-1, y-1, 2, 2);

            if (classBounds.intersects(pointBounds)) return klass;
        }
        return null;
    }

    /**
     * Finds the property under the users click.
     * @param x a x coordinate, plus some leeway for the bounds.
     * @param y a y coordinate, plus some leeway for the bounds.
     * @return the Edge under the click, or null otherwise.
     */
    private Edge findPropertyUnder(double x, double y) {
        for (Edge property : properties) {
            Bounds propBounds = property.getBounds();
            Bounds pointBounds = new BoundingBox(x-1, y-1, 2, 2);

            if (propBounds.intersects(pointBounds)) return property;
        }
        LOGGER.info("no property was found within ("+x+", "+y+"), left unbound. ");
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
            String ttl = Converter.convertGraphToTtlString(prefixes, classes, properties, config);
            try {
                FileWriter writer = new FileWriter(saveFile);
                writer.write(ttl);
                writer.close();
                statusLbl.setText("File saved.");
                Desktop.getDesktop().open(saveFile);
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
    @FXML protected void canvasAction(MouseEvent mouseEvent) {
        Vertex vertex;
        double x = mouseEvent.getX();
        double y = mouseEvent.getY();

        if (mouseEvent.isSecondaryButtonDown()){
            deleteGraphElement(mouseEvent);
        } else if ((vertex = findClassUnder(x, y)) != null && srcClick){
            addSubjectOfProperty(vertex);
        } else if ((vertex = findClassUnder(x, y)) != null){
            addObjectOfProperty(mouseEvent, vertex);
        } else if (srcClick){
            addElementSubaction(mouseEvent);
        } else {
            drawPane.getChildren().remove(arrow);
            statusLbl.setText("Outside any class or literal, property creation cancelled. ");
            subject = null;
            arrow = null;
            srcClick = true;
        }
        if (classes.size() > 0) graphCreated.setValue(true);
        else graphCreated.setValue(false);
    }

    /**
     * Remove the specified graph element from both the canvas and the internal representation.
     * @param mouseEvent the click that specfies which graph element to remove.
     */
    private void deleteGraphElement(MouseEvent mouseEvent) {
        double x = mouseEvent.getX();
        double y = mouseEvent.getY();
        Vertex klass;
        Edge   property;

        if ((klass = findClassUnder(x, y)) != null) {
            drawPane.getChildren().remove(klass.getContainer());

            for (Edge incEdge : klass.getIncomingEdges()){
                drawPane.getChildren().remove(incEdge.getContainer());
                properties.remove(incEdge);
            }
            for (Edge outEdge : klass.getOutgoingEdges()){
                drawPane.getChildren().remove(outEdge.getContainer());
                properties.remove(outEdge);
            }
            classes.remove(klass);
        } else if ((property = findPropertyUnder(x, y)) != null) {
            drawPane.getChildren().remove(property.getContainer());
            property.getSubject().getOutgoingEdges().remove(property);
            property.getObject().getIncomingEdges().remove(property);
            properties.remove(property);
        } else {
            statusLbl.setText("No graph element is under your cursor to delete. ");
            LOGGER.info("Nothing under (" + x + ", " + y + ") for deletion.");
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
    private void addObjectOfProperty(MouseEvent mouseEvent, Vertex obj) {
        obj.setSnapTo(subject.getX(), subject.getY(), mouseEvent.getX(), mouseEvent.getY());

        arrow.setEndX(obj.getX());
        arrow.setEndY(obj.getY());

        StackPane compiledProperty = new StackPane();
        compiledProperty.setLayoutX(subject.getX() < obj.getX() ? subject.getX() : obj.getX());
        compiledProperty.setLayoutY(subject.getY() < obj.getY() ? subject.getY() : obj.getY());

        Text propertyName0 = showNameElementDialog();
        if (propertyName0 == null){
            drawPane.getChildren().remove(arrow);
            statusLbl.setText("Property creation cancelled. ");
            subject = null;
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
        compiledProperty.toBack();

        Edge edge = new Edge(compiledProperty, propertyName, subject, obj);
        properties.add(edge);
        subject.addOutgoingEdge(edge);
        obj.addIncomingEdge(edge);

        statusLbl.setText("Property " + propertyName.getText() + " created. ");
        subject = null;
        arrow = null;
        srcClick = true;
    }

    /**
     * Defines the Subject, or domain, of the property.
     */
    private void addSubjectOfProperty(Vertex sub) {
        subject = sub;
        subject.snapToCenter();

        arrow = new Arrow();
        arrow.setMouseTransparent(true);
        arrow.setStartX(subject.getX());
        arrow.setStartY(subject.getY());
        arrow.setEndX(subject.getX());
        arrow.setEndY(subject.getY());

        drawPane.getChildren().add(arrow);
        arrow.toBack();
        srcClick = false;
        statusLbl.setText("Subject selected. Click another element for the Object.");
    }

    /**
     * Draw a Class or Literal and it's name to the canvas, and create the GraphClass representation of the element.
     * Helper method of {@link #canvasAction(MouseEvent) Add Element} method.
     * @param mouseEvent the click to the canvas.
     */
    private void addElementSubaction(MouseEvent mouseEvent) {
        double x = mouseEvent.getX();
        double y = mouseEvent.getY();
        boolean isOntology = config.get(2);
        boolean isClass;

        // from https://www.w3.org/TR/turtle/ definition of a literal.
        String globalLiteralRegex = "\".*\".*" +
                "|[+\\-]?[0-9]+(\\.[0-9]+)?" +
                "|([+\\-]?[0-9]+\\.[0-9]+|[+\\-]?\\.[0-9]+|[+\\-]?[0-9])E[+\\-]?[0-9]+";
        String instanceLiteralRegex = "[^\"](.* .*)*[^\"]";

        resizeEdgeOfCanvas(x, y);

        StackPane compiledElement = new StackPane();
        compiledElement.setLayoutX(x);
        compiledElement.setLayoutY(y);

        Text elementName;
        ArrayList<String> ontologyClassInfo =  new ArrayList<>();

        if (isOntology) {
            ontologyClassInfo = showNameOntologyClassDialog();
            if (ontologyClassInfo == null) return;
            elementName = new Text(ontologyClassInfo.get(0));
        } else {
            elementName = showNameElementDialog();
            if (elementName == null) return;
        }

        if (elementName.getText().equals("")){
            isClass = true;
            elementName = new Text("_:" + Vertex.getNextBlankNodeName());
        } else isClass = !elementName.getText().matches(globalLiteralRegex + "|" + instanceLiteralRegex);

        double textWidth = elementName.getBoundsInLocal().getWidth();

        if (isClass){
            Ellipse elementType = new Ellipse(x, y, textWidth / 2 > 62.5 ? textWidth / 2 + 10 : 62.5, 37.5);
            elementType.setFill(Color.web("f4f4f4"));
            elementType.setStroke(Color.BLACK);
            compiledElement.getChildren().addAll(elementType, elementName);

        } else {
            Rectangle elementType = new Rectangle(textWidth > 125 ? textWidth + 15 : 125, 75);
            String name = elementName.getText();

            elementType.setFill(Color.web("f4f4f4"));
            elementType.setStroke(Color.BLACK);
            if (name.matches(instanceLiteralRegex) && !name.matches(globalLiteralRegex))
                elementType.getStrokeDashArray().addAll(10d, 10d);
            compiledElement.getChildren().addAll(elementType, elementName);
        }

        drawPane.getChildren().add(compiledElement);
        try {
            if (isOntology) {
                String rdfslabel = ontologyClassInfo.get(1);
                String rdfscomment = ontologyClassInfo.get(2);
                classes.add(new Vertex(compiledElement, rdfslabel, rdfscomment));
            }
            else classes.add(new Vertex(compiledElement));
        } catch (OutsideElementException e) {
            e.printStackTrace();
        }
    }

    private ArrayList<String> showNameOntologyClassDialog() {
        ArrayList<String> ontologyClass = showWindow("/view/ontologyclassdialog.fxml", "Add new Ontology Class", null);
        if (ontologyClass != null && ontologyClass.size() != 0) return ontologyClass;
        else return null;
    }

    /**
     * On clicking 'Instructions' button, show the instructions...
     */
    @FXML protected void showInstructionsAction() {
        showInstructionsAlert();
    }

    /**
     * On clicking options button, show the options dialog...
     */
    @FXML private void showOptionsAction() {
        showOptionsDialog();
    }

    /**
     * Extend the canvas width and height if any new element gets to close to the bounds of the drawPane canvas.
     * @param x x coordinate to check if we need to extend the width of the canvas.
     * @param y y coordinate to check if we need to extend the height of the canvas.
     */
    private void resizeEdgeOfCanvas(double x, double y) {
        double height = drawPane.getHeight();
        double width  = drawPane.getWidth();

        if (x > width - 150 && y > height - 150) {
            drawPane.setPrefSize(x + 300, y + 300);
        } else if (x > width - 150) {
            drawPane.setPrefWidth(x + 300);
        } else if (y > height - 150) {
            drawPane.setPrefHeight(y + 300);
        }
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
     * @return the file that will be loaded from.
     */
    private File showLoadFileDialog(String title){
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        return fileChooser.showOpenDialog(root.getScene().getWindow());
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
                        " add to the canvas, then click somewhere on the canvas. Add a name (even in .ttl synta" +
                        "x!) and the item will be created in that position. \nIn regards to the Property button, you " +
                        "must click on a valid (already existing) element in the graph as the subject, and then anoth" +
                        "er as the object. If you click on something that is not a Class or Literal, you will need to" +
                        " click the subject-object pair again.\nFeel free to add elements near the edge of the graph," +
                        " it automatically resizes! "
        );

        instrAlert.showAndWait();
    }

    /**
     * Creates a options dialog.
     */
    private void showOptionsDialog() {
        ArrayList<Boolean> updatedConfig = showWindow("/view/optionsmenu.fxml", "Options for the Current Project", config);
        if (updatedConfig != null) config = updatedConfig;
    }

    @FXML protected void ingestCsvAction(){
        File loadFile = showLoadFileDialog("Load .csv for RDF/XML generation");
        if (loadFile != null){
            csv = null;
            headers = null;
            try (Reader reader = new BufferedReader(new FileReader(loadFile))){
                CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader);
                headers = parser.getHeaderMap();
                csv = parser.getRecords();
                statusLbl.setText(".csv ingested. Yum.");
                LOGGER.info("Ingested " + loadFile.getName() + ".\nFound csv headers: " + headers);
                rdfXmlBtn.setDisable(false);
                parser.close();
                csvIngested.setValue(true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML protected void rdfXmlGenAction() {
        String rdfxml;
        RDFXMLGenerator rdfxmlGenerator = new RDFXMLGenerator(headers, csv, classes, prefixes);
        rdfxmlGenerator.attemptCorrelationOfHeaders();

        LOGGER.info("BEFORE Correlation:\nCorrelated: " + rdfxmlGenerator.getCorrelations().toString() +
                "\nUncorrelated: " + rdfxmlGenerator.uncorrelatedToString());

        if (rdfxmlGenerator.getUncorrelated() != null && rdfxmlGenerator.getUncorrelated().getKey().size() != 0)
            showManualCorrelationDialog(rdfxmlGenerator);
        if (rdfxmlGenerator.getUncorrelated() != null && rdfxmlGenerator.getUncorrelated().getKey().size() != 0){
            LOGGER.info("Cancelled Manual Correlations. ");
            return;
        }

        LOGGER.info("AFTER Correlation:" +
                "\nCorrelated: " + rdfxmlGenerator.getCorrelations().toString() +
                "\nUncorrelated (assumed constant): " + rdfxmlGenerator.uncorrelatedClassesToString());

        rdfxml = rdfxmlGenerator.generate();
        File saveFile = showSaveFileDialog("rdf.rdf", "Save RDF/XML Document", null);
        if (saveFile != null){
            try {
                FileWriter writer = new FileWriter(saveFile);
                writer.write(rdfxml);
                writer.flush();
                writer.close();
                statusLbl.setText("RDF/XML saved.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void showManualCorrelationDialog(RDFXMLGenerator generator){
        ArrayList<RDFXMLGenerator> data = new ArrayList<>();
        data.add(generator);
        showWindow("/view/correlateDialog.fxml", "Set Manual Correlations", data);
    }
}

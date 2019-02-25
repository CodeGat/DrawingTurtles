package controller;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.conceptual.Edge;
import model.conceptual.Vertex;
import model.conceptual.Vertex.OutsideElementException;
import model.conceptual.Vertex.UndefinedElementTypeException;
import model.conversion.gat.FromGatConverter;
import model.conversion.gat.ToGatConverter;
import model.dataintegration.DataIntegrator;
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
import model.graph.SelfReferentialArrow;
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

import static javafx.stage.FileChooser.*;

/**
 * The Controller for application.fxml: takes care of actions from the application.
 */
public final class Controller implements Initializable {
    private static final Logger LOGGER = Logger.getLogger(Controller.class.getName());

    @FXML protected BorderPane root;
    @FXML protected Pane drawPane;
    @FXML protected ScrollPane scrollPane;
    @FXML protected Button prefixBtn, saveGraphBtn, loadGraphBtn, exportTllBtn, exportPngBtn, eatCsvBtn, instanceBtn,
            instrBtn, optionsBtn;
    @FXML ImageView ttlPrefImv, ttlGraphImv, instPrefImv, instGraphImv, instCsvImv;
    @FXML protected Label statusLbl;
    @FXML protected ToolBar toolBar;

    private ArrayList<Boolean> config = new ArrayList<>(Arrays.asList(false, false, false));

    private Map<String, String>     prefixes   = new HashMap<>();
    private final ArrayList<Edge>   properties = new ArrayList<>();
    private final ArrayList<Vertex> classes    = new ArrayList<>();

    private Arrow arrow;
    private Vertex subject;
    private boolean srcClick = true;

    private List<CSVRecord> csv;
    private Map<String, Integer> headers;

    static String lastDirectory;

    private BooleanProperty prefixesInspected = new SimpleBooleanProperty(false);
    private BooleanProperty graphCreated = new SimpleBooleanProperty(false);
    private BooleanProperty csvIngested = new SimpleBooleanProperty(false);

    public static final Color JFX_DEFAULT_COLOUR = Color.web("F4F4F4");

    /**
     * Adds listeners for the Boolean Properties (and hence the workflow checklist) of prefix inspection, graph
     *    creation and .csv ingestion. Also adds the initial common prefixes.
     */
    @Override public void initialize(URL location, ResourceBundle resources) {
        Image cross = new Image("/view/images/cross.png");
        Image tick  = new Image("/view/images/tick.png");
        ttlPrefImv.setImage(cross);
        ttlGraphImv.setImage(cross);
        instPrefImv.setImage(cross);
        instGraphImv.setImage(cross);
        instCsvImv.setImage(cross);

        prefixes.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        prefixes.put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        prefixes.put("owl", "http://www.w3.org/2002/07/owl#");
        prefixes.put("xsd", "http://www.w3.org/2001/XMLSchema#");

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
     * Creates and displays the Window defined in the fxml file, also passing data of type T to a controller C.
     * @param fxml the fxml file in which the layout is defined.
     * @param title the title of the new window.
     * @param data the parameters passed to the Controller.
     * @param <C> a Controller that can pass data to and recieve data from this method (extending
     *           DataSharingController).
     * @param <T> the type of data passed to and from the Controller.
     * @return the data after it has been modified by the Controller.
     */
    @FXML @SuppressWarnings("unchecked")
    private <C extends DataSharingController<T>, T> ArrayList<T> showWindow(String fxml, String title, ArrayList<T> data){
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
     * On clicking 'Save Graph' button, attempt to traverse the graph and save a bespoke serialization of the graph to
     *   a user-specified .gat file. That's a Graph Accessor Type format, not just my name...
     */
    @FXML public void saveGraphAction() {
        File saveFile = showSaveFileDialog(
                "graph.gat",
                "Save Graph As",
                new ExtensionFilter("Graph Accessor Type Files (*.gat)", "*.gat"));
        if (saveFile != null) {
            if (!saveFile.getName().matches(".*\\.gat")){
                setWarnStatus("Failed to save Graph file: You attempted to save the file as a non-.gat file.");
                return;
            }
            ToGatConverter converter = new ToGatConverter(drawPane.getWidth(), drawPane.getHeight(), classes, properties);
            String filetext = converter.traverseCanvas();
            try {
                FileWriter writer = new FileWriter(saveFile);
                writer.write(filetext);
                writer.flush();
                writer.close();
                setInfoStatus("File saved.");
            } catch (IOException e) {
                setErrorStatus("Failed to save graph: IOException occurred during save. ");
                LOGGER.log(Level.SEVERE, "failed to save graph: ", e);
            }
        } else setInfoStatus("File save cancelled.");
    }

    /**
     * On clicking the 'Load Graph' button, clears the canvas and attempts to deserialize the user-specified .gat file
     *   into elements of a graph. It then binds the visual elements into meaningful java-friendly elements.
     */
    @FXML public void loadGraphAction() {
        File loadFile = showLoadFileDialog(
                    "Load Graph File",
                    new ExtensionFilter("Graph Accessor Type file (*.gat)", "*.gat")
            );
        if (loadFile != null){
            lastDirectory = loadFile.getParent();
            drawPane.getChildren().clear();
            classes.clear();
            properties.clear();

            try (BufferedReader reader = new BufferedReader(new FileReader(loadFile))){
                String graph = reader.readLine(); // TODO: 24/02/2019 may need to read more than one line in case of multiline rdfs:comment?
                if (graph == null || graph.length() == 0){
                    setWarnStatus("Graph Read failed: nothing in graph file.");
                    LOGGER.warning("Nothing in graph file.");
                    return;
                }
                FromGatConverter binder = new FromGatConverter(graph);
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
                setInfoStatus("Graph load successful.");
            } catch (IOException e) {
                setErrorStatus("Graph load failed: IOException occurred while reading the graph from file. ");
                LOGGER.log(Level.SEVERE, "Loading the graph failed: ", e);
            } catch (FromGatConverter.PropertyElemMissingException e) {
                setErrorStatus("Graph load failed: " + e.getMissingElement() + " is missing from " +
                        e.getPropertyName() + ". Try adding the arrow again. ");
                LOGGER.log(Level.SEVERE, "Parsing the graph failed: ", e);
            } catch (OutsideElementException e) {
                setErrorStatus("Graph load failed: The .gat file has been corrupted, properties do not match classes." +
                        " Re-create the graph. ");
                LOGGER.log(Level.SEVERE, "Parsing the graph failed: ", e);
            } catch (UndefinedElementTypeException e) {
                setErrorStatus("Graph Load failed: The name of a class does not match Turtle syntax. Recreate the" +
                        " graph. ");
                LOGGER.log(Level.SEVERE, "Parsing the graph failed: ", e);
            }
        } else setInfoStatus("Graph load cancelled.");
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
            Bounds pointBounds = new BoundingBox(x-2, y-2, 4, 4);

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
                new ExtensionFilter("Turtle Files (*.ttl)", "*.ttl")
        );
        if (saveFile != null){
            if (!saveFile.getName().matches(".*\\.ttl")){
                setWarnStatus("Failed to save Turtle File: You attempted to save the file as a non-.ttl file.");
                return;
            }

            String ttl = Converter.convertGraphToTtlString(prefixes, classes, properties, config);
            try {
                FileWriter writer = new FileWriter(saveFile);
                writer.write(ttl);
                writer.close();
                setInfoStatus("File saved.");
                Desktop.getDesktop().open(saveFile);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "failed to export to .tll: ", e);
            }

        } else setInfoStatus("File save cancelled.");
    }

    /**
     * On clicking "Export as .png' button, attempt to save the canvas to a user-specified .png file.
     */
    @FXML protected void exportPngAction() {
        File saveFile = showSaveFileDialog(
                "ontology.png",
                "Save Conceptual Image As",
                new ExtensionFilter("Portable Network Graphic Files (*.png)", "*.png")
        );
        if (saveFile != null){
            if (!saveFile.getName().matches(".*\\.png")){
                setWarnStatus("Failed to save PNG File: You attempted to save the file as a non-.png file.");
                return;
            }
            try {
                WritableImage writableImage = drawPane.snapshot(new SnapshotParameters(), null);
                RenderedImage renderedImage = SwingFXUtils.fromFXImage(writableImage, null);
                ImageIO.write(renderedImage, "png", saveFile);
                setInfoStatus("File saved.");
                Desktop.getDesktop().open(saveFile);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "failed to export to .png: ", e);
            }
        } else setInfoStatus("Image save cancelled.");
    }

    /**
     * On clicking the canvas, determine the type of action and execute it.
     * @param mouseEvent the click that triggered the method.
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
            setInfoStatus("Outside any class or literal, property creation cancelled. ");
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
                incEdge.getSubject().getOutgoingEdges().remove(incEdge);
                properties.remove(incEdge);
            }
            for (Edge outEdge : klass.getOutgoingEdges()){
                drawPane.getChildren().remove(outEdge.getContainer());
                outEdge.getObject().getIncomingEdges().remove(outEdge);
                properties.remove(outEdge);
            }
            classes.remove(klass);
        } else if ((property = findPropertyUnder(x, y)) != null) {
            drawPane.getChildren().remove(property.getContainer());
            property.getSubject().getOutgoingEdges().remove(property);
            property.getObject().getIncomingEdges().remove(property);
            properties.remove(property);
        } else {
            setInfoStatus("No graph element is under your cursor to delete. ");
            LOGGER.info("Nothing under (" + x + ", " + y + ") for deletion.");
        }
    }

    /**
     * Keeps the arrow in line with the mouse as the user clicks on the graph Object.
     * @param mouseEvent the event that triggered the method.
     */
    @FXML protected void moveArrowAction(MouseEvent mouseEvent) {
        if (arrow == null) return;
        arrow.setEndX(mouseEvent.getX());
        arrow.setEndY(mouseEvent.getY());
    }

    /**
     * Defines the Object, or range, of the property, and creates the association between the Subject and Object.
     * @param mouseEvent the second click on the canvas after a graph Subject is clicked.
     * @param object the object specified by the users click.
     */
    private void addObjectOfProperty(MouseEvent mouseEvent, Vertex object) {
        boolean isSelfReferential = subject == object;
        StackPane compiledProperty;

        compiledProperty = isSelfReferential ? addSelfReferentialProperty() : addNormalProperty(mouseEvent, object);

        if (compiledProperty == null) return;

        drawPane.getChildren().add(compiledProperty);
        compiledProperty.toBack();

        Label propertyName = (Label) compiledProperty.getChildren().get(1);
        Edge edge = new Edge(compiledProperty, propertyName, subject, object);
        properties.add(edge);
        subject.addOutgoingEdge(edge);
        object.addIncomingEdge(edge);

        setInfoStatus("Property " + propertyName.getText() + " created. ");
        subject = null;
        arrow = null;
        srcClick = true;
    }

    /**
     * Adds a normal (non-self-referential) property to the canvas.
     * @param mouseEvent the location of the users second click.
     * @param object the object under the users second click.
     * @return the compiled container of the arrow and the associated name.
     */
    private StackPane addNormalProperty(MouseEvent mouseEvent, Vertex object) {
        StackPane compiled = new StackPane();

        object.setSnapTo(subject.getX(), subject.getY(), mouseEvent.getX(), mouseEvent.getY());

        arrow.setEndX(object.getX());
        arrow.setEndY(object.getY());

        compiled.setLayoutX(subject.getX() < object.getX() ? subject.getX() : object.getX());
        compiled.setLayoutY(subject.getY() < object.getY() ? subject.getY() : object.getY());

        ArrayList<String> propertyInfo = showNameElementDialog();
        if (propertyInfo == null || propertyInfo.size() == 0){
            drawPane.getChildren().remove(arrow);
            setInfoStatus("Property creation cancelled. ");
            subject = null;
            arrow = null;
            srcClick = true;
            return null;
        }

        Label propertyName = new Label(propertyInfo.get(0));
        BackgroundFill fill = new BackgroundFill(JFX_DEFAULT_COLOUR, CornerRadii.EMPTY, Insets.EMPTY);
        propertyName.setBackground(new Background(fill));

        double textWidth = (new Text(propertyInfo.get(0))).getBoundsInLocal().getWidth();
        if (textWidth > arrow.getWidth()) {
            double overrunOneSide = (textWidth - arrow.getWidth()) / 2;
            compiled.setLayoutX(compiled.getLayoutX() - overrunOneSide);
        }
        compiled.getChildren().addAll(arrow, propertyName);

        return compiled;
    }

    /**
     * Adds a self-referential property to the canvas.
     * @return the compiled container of the "arrow" and the associated name.
     */
    private StackPane addSelfReferentialProperty() {
        StackPane compiled = new StackPane();
        Ellipse subEllipse = (Ellipse) subject.getContainer().getChildren().get(0);

        SelfReferentialArrow selfRefArrow = new SelfReferentialArrow();
        selfRefArrow.setCenterX(subEllipse.getCenterX() + subEllipse.getRadiusX() / 2);
        selfRefArrow.setCenterY(subEllipse.getCenterY() + subEllipse.getRadiusY() / 2);
        selfRefArrow.setRadiusX(subEllipse.getRadiusX() / 1.5);
        selfRefArrow.setRadiusY(subEllipse.getRadiusY());

        compiled.setLayoutX(subject.getX());
        compiled.setLayoutY(subject.getY());
        drawPane.getChildren().remove(arrow);
        arrow = null;

        ArrayList<String> propertyInfo = showNameElementDialog();
        if (propertyInfo == null || propertyInfo.size() == 0){
            drawPane.getChildren().remove(arrow);
            setInfoStatus("Property creation cancelled. ");
            subject = null;
            arrow = null;
            srcClick = true;
            return null;
        }

        Label propertyName = new Label(propertyInfo.get(0));
        BackgroundFill fill = new BackgroundFill(JFX_DEFAULT_COLOUR, CornerRadii.EMPTY, Insets.EMPTY);
        propertyName.setBackground(new Background(fill));

        double textWidth = (new Text(propertyInfo.get(0))).getBoundsInLocal().getWidth();

       if (textWidth > selfRefArrow.getRadiusX() * 2){
            double overrunOneSide = (textWidth - selfRefArrow.getRadiusX()) / 2;
            compiled.setLayoutX(compiled.getLayoutX() - overrunOneSide + subEllipse.getRadiusX() / 3);
        }
        compiled.getChildren().addAll(selfRefArrow, propertyName);
        StackPane.setAlignment(propertyName, Pos.BOTTOM_CENTER);

        return compiled;
    }

    /**
     * Defines the Subject, or domain, of the property.
     * @param sub the Subject associated with the users click.
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
        setInfoStatus("Subject selected. Click another element for the Object.");
    }

    /**
     * Draw a Class or Literal and it's name to the canvas, and create the Vertex representation of the element.
     * @param mouseEvent the click to the canvas that specifies where the element is to be placed.
     */
    private void addElementSubaction(MouseEvent mouseEvent) {
        double x = mouseEvent.getX();
        double y = mouseEvent.getY();
        boolean isOntology = config.get(2);
        boolean isClass;

        // from https://www.w3.org/TR/turtle/ definition of a literal.
        String globalLiteralRegex = "\".*\"(\\^\\^.*|@.*)?" + // unspecified / String
                "|true|false" + // boolean
                "|[+\\-]?\\d+" + //integer
                "|[+\\-]?\\d*\\.\\d+" + // decimal
                "|([+\\-]?\\d+\\.\\d+|[+\\-]?\\.\\d+|[+\\-]?\\d+)[Ee][+\\-]\\d+"; // double
        String instanceLiteralRegex = "(?<!\")[^:]*(?!\")";

        resizeEdgeOfCanvas(x, y);

        StackPane compiledElement = new StackPane();
        compiledElement.setLayoutX(x);
        compiledElement.setLayoutY(y);

        ArrayList<String> classInfo = isOntology ? showNameOntologyClassDialog() : showNameElementDialog();
        if (classInfo == null || classInfo.size() == 0) return;
        Text elementName = new Text(classInfo.get(0));

        if (elementName.getText().equals("")){
            isClass = true;
            elementName = new Text("_:" + Vertex.getNextBlankNodeName());
        } else isClass = !elementName.getText().matches(globalLiteralRegex + "|" + instanceLiteralRegex);

        double textWidth = elementName.getBoundsInLocal().getWidth();
        if (isClass){
            Ellipse elementType = new Ellipse(x, y, textWidth / 2 > 62.5 ? textWidth / 2 + 10 : 62.5, 37.5);
            elementType.setFill(JFX_DEFAULT_COLOUR);
            elementType.setStroke(Color.BLACK);
            compiledElement.getChildren().addAll(elementType, elementName);
        } else {
            Rectangle elementType = new Rectangle(textWidth > 125 ? textWidth + 15 : 125, 75);
            String name = elementName.getText();

            elementType.setFill(JFX_DEFAULT_COLOUR);
            elementType.setStroke(Color.BLACK);
            if (name.matches(instanceLiteralRegex) && !name.matches(globalLiteralRegex))
                elementType.getStrokeDashArray().addAll(10d, 10d);
            compiledElement.getChildren().addAll(elementType, elementName);
        }

        drawPane.getChildren().add(compiledElement);
        try {
            if (isOntology && isClass) {
                String rdfslabel = classInfo.get(2);
                String rdfscomment = classInfo.get(3);
                classes.add(new Vertex(compiledElement, rdfslabel, rdfscomment));
            } else if (isOntology){
                String dataType = classInfo.get(1);
                classes.add(new Vertex(compiledElement, dataType));
            } else classes.add(new Vertex(compiledElement));
        } catch (OutsideElementException | UndefinedElementTypeException e) {
            e.printStackTrace();
        }
    }

    /**
     * Opens the Ontology Class Dialog window and promts the user to input information regarding the class.
     * @return the new user-specified ontology class information, if it exists.
     */
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
     * Show the basic dialog for creating a new element.
     * @return the ArrayList containing the name and type of the given element, if applicable.
     */
    private ArrayList<String> showNameElementDialog() {
        return showWindow("/view/newClassDialog.fxml", "Add new Graph Element", null);
    }

    /**
     * Creates a save file dialog, prompting the user to select a file to create and/or save data to.
     * @param fileName the default filename the dialog will save the file as.
     * @param windowTitle the title of the dialog.
     * @param extFilter the list of extension filters, for easy access to specific file types.
     * @return the file the user has chosen to save to, or null otherwise.
     */
    private File showSaveFileDialog(String fileName, String windowTitle, ExtensionFilter extFilter) {
        File directory = new File(lastDirectory != null ? lastDirectory : System.getProperty("user.home"));
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialFileName(fileName);
        fileChooser.setTitle(windowTitle);
        if (extFilter != null) fileChooser.getExtensionFilters().add(extFilter);
        fileChooser.setInitialDirectory(directory);

        return fileChooser.showSaveDialog(root.getScene().getWindow());
    }

    /**
     * Creates a load file dialog, which prompts the user to load from a specific file.
     * @param title the title of the Dialog.
     * @param extFilter the extension filter for the dialog, which restricts selection to files of a given type.
     * @return the file that will be loaded from.
     */
    private File showLoadFileDialog(String title, ExtensionFilter extFilter){
        File directory = new File(lastDirectory != null ? lastDirectory : System.getProperty("user.home"));
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        fileChooser.setInitialDirectory(directory);
        fileChooser.getExtensionFilters().add(extFilter);

        return fileChooser.showOpenDialog(root.getScene().getWindow());
    }

    /**
     * Creates an instructional alert.
     */
    private void showInstructionsAlert() {
        showWindow("/view/instructions.fxml", "Instructions for Drawing Turtles", null);
    }

    /**
     * Creates a options dialog.
     */
    private void showOptionsDialog() {
        ArrayList<Boolean> updatedConfig = showWindow("/view/optionsmenu.fxml", "Options for the Current Project", config);
        if (updatedConfig != null) config = updatedConfig;
    }

    /**
     * Loads and parses a given .csv file into a List<CSVRecord>.
     */
    @FXML protected void ingestCsvAction(){
        File loadFile = showLoadFileDialog(
                "Load .csv for Instance-Level Turtle Generation",
                new ExtensionFilter("Comma Separated Values (*.csv)", "*.csv")
        );
        if (loadFile != null){
            csv = null;
            headers = null;
            try (Reader reader = new BufferedReader(new FileReader(loadFile))){
                CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader);
                headers = parser.getHeaderMap();
                csv = parser.getRecords();
                setInfoStatus(".csv ingested. Yum.");
                LOGGER.info("Ingested " + loadFile.getName() + ".\nFound csv headers: " + headers);
                instanceBtn.setDisable(false);
                parser.close();
                csvIngested.setValue(true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else setInfoStatus(".csv ingesting cancelled. ");
    }

    /**
     * Attempts to generate instance-level Turtle given a valid graph and .csv data.
     * Also attempts to correlate the .csv headers and graph classes - if there are some left over, it is left to the
     *    user.
     */
    @FXML protected void instanceGenAction() {
        String instanceData;
        DataIntegrator dataIntegrator = new DataIntegrator(headers, csv, classes, prefixes);
        dataIntegrator.attemptCorrelationOfHeaders();

        LOGGER.info("BEFORE Correlation:\nCorrelated: " + dataIntegrator.getCorrelations().toString() +
                "\nUncorrelated: " + dataIntegrator.uncorrelatedToString());

        if (dataIntegrator.getUncorrelated() != null && dataIntegrator.getUncorrelated().getKey().size() != 0)
            showManualCorrelationDialog(dataIntegrator);
        if (dataIntegrator.getUncorrelated() != null && dataIntegrator.getUncorrelated().getKey().size() != 0){
            LOGGER.info("Cancelled Manual Correlations. ");
            return;
        }

        LOGGER.info("AFTER Correlation:" +
                "\nCorrelated: " + dataIntegrator.getCorrelations().toString() +
                "\nUncorrelated (assumed constant): " + dataIntegrator.uncorrelatedClassesToString());

        try {
            instanceData = dataIntegrator.generate();
        } catch (DataIntegrator.PrefixMissingException e) {
            setErrorStatus("Data Integration failed: '" + e.getMissing() + "' is referenced in graph but not " +
                    "defined in the Prefixes Menu. ");
            LOGGER.log(Level.SEVERE, "Integration failed: ", e);
            return;
        }

        File saveFile = showSaveFileDialog(
                "instance.ttl",
                "Save Instance-Level Turtle Document",
                new ExtensionFilter("Turtle Files (*.ttl)", "*.ttl")
        );
        if (saveFile != null){
            if (!saveFile.getName().matches(".*\\.ttl")){
                setWarnStatus("Failed to save Turtle File: You attempted to save the file as a non-.ttl file.");
                return;
            }
            try {
                FileWriter writer = new FileWriter(saveFile);
                writer.write(instanceData);
                writer.flush();
                writer.close();
                setInfoStatus("Instance-level Turtle saved.");
                Desktop.getDesktop().open(saveFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Show the manual correlations dialog, prompting the user to correlate the .csv headers and the graph classes,
     *    modifying the underlying DataIntegerator.
     * @param generator the DataIntegrator that is modified when the user determines the correlations between data.
     */
    private void showManualCorrelationDialog(DataIntegrator generator){
        ArrayList<DataIntegrator> data = new ArrayList<>();
        data.add(generator);
        showWindow("/view/correlateDialog.fxml", "Set Manual Correlations", data);
    }

    /**
     * Sets the toolbar to transparent and displays a informative message to the user.
     * @param message the message to send to the user.
     */
    private void setInfoStatus(String message) {
        statusLbl.setText(message);
        toolBar.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY)));
    }

    /**
     * Sets the toolbar to orange and displays a warning message to the user.
     * @param message the message to send to the user.
     */
    private void setWarnStatus(String message) {
        statusLbl.setText(message);
        toolBar.setBackground(new Background(new BackgroundFill(Color.ORANGE, CornerRadii.EMPTY, Insets.EMPTY)));
    }

    /**
     * Set the toolbar to red and displays an error message to the user.
     * @param message the message to send to the user.
     */
    private void setErrorStatus(String message){
        statusLbl.setText(message);
        toolBar.setBackground(new Background(new BackgroundFill(Color.RED, CornerRadii.EMPTY, Insets.EMPTY)));
    }
}

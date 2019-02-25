package controller;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static javafx.stage.FileChooser.*;

/**
 * Controller for view.prefixmenu.fxml.
 */
public class PrefixMenuController extends DataSharingController<Map<String, String>> implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(PrefixMenuController.class.getName());

    @FXML AnchorPane root;
    @FXML ToolBar toolBar;
    @FXML Button addPrefixBtn, remPrefixBtn, clrPrefixBtn, savPrefixBtn, lodPrefixBtn, cmtPrefixBtn, canPrefixBtn;
    @FXML ListView<String> prefixList;
    @FXML Label statusLbl;

    private final BooleanProperty isItemSelected = new SimpleBooleanProperty(false);

    private Map<String, String> commit_prefixes;
    private Map<String, String> prefixes;


    /**
     * Add listener for the BooleanProperty that determines whether a item is selected in the prefix list.
     */
    @Override public void initialize(URL location, ResourceBundle resources) {
        prefixList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue == null) isItemSelected.setValue(true);
            else if (newValue == null) isItemSelected.setValue(false);
        });

        isItemSelected.addListener((observable, oldValue, newValue) -> {
            if (observable.getValue().booleanValue()) remPrefixBtn.setDisable(false);
            else remPrefixBtn.setDisable(true);

        });
    }

    /**
     * On clicking the 'Add Prefix' button, adds prefixes to the arraylist of existing prefixes unless malformed.
     */
    @FXML protected void addPrefixAction() {
        String prefixResult = showAddPrefixesDialog();

        if (prefixResult == null) {
            setInfoStatus("Prefix addition cancelled. ");
            return;
        }
        String[] newPrefixes = prefixResult.split(", ");

        for (String prefix : newPrefixes){
            if (prefix.matches("[a-z]* ?: ?.*")) {
                String[] prefixParts = prefix.split(":", 2);
                String   acronym = prefixParts[0].trim();
                String   expansion = prefixParts[1].trim();

                prefixes.put(acronym, expansion);
                prefixList.getItems().add(prefix);

            } else showPrefixMalformedAlert(prefix);
        }
    }

    /**
     * Remove the currently selected prefix from the ListView and the underlying prefixes.
     */
    @FXML void removePrefixAction() {
        String prefix = prefixList.getSelectionModel().getSelectedItem();
        String prefixToRemove = prefix.split(":")[0].trim();
        prefixes.remove(prefixToRemove);
        prefixList.getItems().remove(prefix);
        prefixList.getSelectionModel().clearSelection();
    }

    /**
     * Removes existing user prefixes, excepting the base ones (owl, rdf, rdfs).
     */
    @FXML protected void clearPrefixAction() {
        prefixes.clear();
        prefixList.getItems().clear();
    }

    /**
     * On clicking the 'Save Prefix' button, attempts to write existing prefixes to a user-specified .txt file.
     */
    @FXML protected void savePrefixAction() {
        File saveFile = showSaveFileDialog(
                savPrefixBtn.getScene().getWindow(),
                new ExtensionFilter("Text Files (*.txt)", "*.txt")
        );
        if (saveFile != null && prefixes.size() != 0) {
            if (!saveFile.getName().matches(".*\\.txt")){
                setWarnStatus("Failed to save Prefixes file: You attempted to save the file as a non-.txt file.");
                return;
            }
            Controller.lastDirectory = saveFile.getParent();
            StringBuilder prefixesToSave = new StringBuilder();
            for (Map.Entry<String, String> prefix : prefixes.entrySet()) {
                String prefixStr = prefix.getKey() + " : " + prefix.getValue();
                prefixesToSave.append(prefixStr).append("\n");
            }
            prefixesToSave.deleteCharAt(prefixesToSave.length() - 1);

            try {
                FileWriter writer = new FileWriter(saveFile);
                writer.write(prefixesToSave.toString());
                writer.flush();
                writer.close();
            } catch (IOException e) {
                setErrorStatus("Failed to save Prefixes file: IOException occurred during save.");
                LOGGER.log(Level.SEVERE, "failed to save prefixes: ", e);
            }
        } else setInfoStatus("Saving of prefixes cancelled. ");
    }

    /**
     * On clicking the 'Load Prefix' button, attempts to load prefixes from a user-specified .txt file.
     */
    @FXML protected void loadPrefixAction(){
        File loadFile = showLoadFileDialog(
                lodPrefixBtn.getScene().getWindow(),
                new ExtensionFilter("Text file (*.txt)", "*.txt")
        );
        if (loadFile != null){
            Controller.lastDirectory = loadFile.getParent();
            try (BufferedReader reader = new BufferedReader(new FileReader(loadFile))){
                String line;

                while((line = reader.readLine()) != null) {
                    String[] prefixParts = line.split(":", 2);
                    String   acronym = prefixParts[0].trim();
                    String   expansion = prefixParts[1].trim();

                    if (!prefixes.containsKey(acronym)) {
                        prefixes.put(acronym, expansion);
                        prefixList.getItems().add(line);
                    }
                }
            } catch (IOException e) {
                setErrorStatus("Failed to load Prefixes file: IOException occurred during load. ");
                LOGGER.log(Level.SEVERE, "Loading prefixes failed: ", e);
            }
        } else setInfoStatus("Prefix loading cancelled. ");

    }

    /**
     * Add the current prefixes for commital, and close the Window.
     */
    @FXML void commitPrefixAction() {
        commit_prefixes = prefixes;
        Stage stage = (Stage) cmtPrefixBtn.getScene().getWindow();
        stage.close();
    }

    /**
     * Closes the Window.
     */
    @FXML void cancelPrefixAction() {
        Stage stage = (Stage) cmtPrefixBtn.getScene().getWindow();
        stage.close();
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
     * Creates a save file dialog, prompting the user to select a file to create and/or save data to.
     * @param owner the window that owns the dialog.
     * @param extFilter the list of extension filters, for easy access to the specified file types.
     * @return the file the user has chosen to save to, or null otherwise.
     */
    private File showSaveFileDialog(Window owner, ExtensionFilter extFilter) {
        File directory =
                new File(Controller.lastDirectory != null ? Controller.lastDirectory : System.getProperty("user.home"));
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialFileName("prefixes.txt");
        fileChooser.setTitle("Save Prefixes");
        fileChooser.getExtensionFilters().addAll(extFilter);
        fileChooser.setInitialDirectory(directory);

        return fileChooser.showSaveDialog(owner);
    }

    /**
     * Creates a load file dialog, which prompts the user to load from a specific file.
     * @param owner the window that owns the dialog.
     * @param extFilter the list of extension filters, for easy access to the specified file types.
     * @return the file that will be loaded from.
     */
    private File showLoadFileDialog(Window owner, ExtensionFilter extFilter){
        File directory =
                new File(Controller.lastDirectory != null ? Controller.lastDirectory : System.getProperty("user.home"));
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load Prefixes");
        fileChooser.setInitialDirectory(directory);
        fileChooser.getExtensionFilters().add(extFilter);

        return fileChooser.showOpenDialog(owner);
    }

    /**
     * The data passed to this controller from the calling Controller.
     * @param data the data to be passed.
     */
    @Override
    public void setData(ArrayList<Map<String, String>> data) {
        prefixes = data.get(0);
        ArrayList<String> prefixesAsList = prefixes
                .entrySet()
                .stream()
                .map(p -> p.getKey() + " : " + p.getValue())
                .collect(Collectors.toCollection(ArrayList::new));
        prefixList.setItems(FXCollections.observableArrayList(prefixesAsList));
    }

    /**
     * Data (the prefixes) to be accessable to the calling Controller.
     * @return the accessable data.
     */
    @Override
    public ArrayList<Map<String, String>> getData() {
        ArrayList<Map<String, String>> data = new ArrayList<>();
        data.add(commit_prefixes);

        return data;
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

package controller;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for the Prefix Menu.
 */
public class PrefixMenuController extends AbstractDataSharingController<String> implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(PrefixMenuController.class.getName());

    @FXML AnchorPane root;
    @FXML Button addPrefixBtn, remPrefixBtn, clrPrefixBtn, savPrefixBtn, lodPrefixBtn, cmtPrefixBtn, canPrefixBtn;
    @FXML ListView<String> prefixList;

    private final BooleanProperty isItemSelected = new SimpleBooleanProperty(false);

    private ArrayList<String> commit_prefixes;
    private ArrayList<String> prefixes;


    /**
     * Overridden initialize method that is run after the PrefixMenuController constructor is called.
     * @param location location of other resources.
     * @param resources text resources that can be used by the PrefixMenu Controller.
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

        if (prefixResult == null) return;
        String[] newPrefixes = prefixResult.split(", ");

        for (String prefix : newPrefixes){
            if (prefix.matches("[a-z]* : .*")) {
                prefixes.add(prefix);
                prefixList.getItems().add(prefix);

            } else showPrefixMalformedAlert(prefix);
        }
    }

    /**
     * Remove the currently selected prefix from the ListView and the underlying prefixes.
     */
    @FXML void removePrefixAction() {
        String prefix = prefixList.getSelectionModel().getSelectedItem();
        prefixes.remove(prefix);
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
                new FileChooser.ExtensionFilter("Text Files (*.txt)", "*.txt"),
                savPrefixBtn.getScene().getWindow()
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
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "failed to save prefixes: ", e);
            }
        }
    }

    /**
     * On clicking the 'Load Prefix' button, attempts to load prefixes from a user-specified .txt file.
     */
    @FXML protected void loadPrefixAction(){
        File loadFile = showLoadFileDialog(lodPrefixBtn.getScene().getWindow());

        if (loadFile != null){
            try (FileReader reader = new FileReader(loadFile)){
                char[] rawPrefixes = new char[10000];

                if (reader.read(rawPrefixes) == 0) LOGGER.warning("Nothing in prefix file. ");
                String[] strPrefixes = new String(rawPrefixes).trim().split("\\n");
                for (String strPrefix : strPrefixes) if (!prefixes.contains(strPrefix)) {
                    prefixes.add(strPrefix);
                    prefixList.getItems().add(strPrefix);
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Loading prefixes failed: ", e);
            }
        }

    }

    /**
     * Commit to the current prefixes, giving them to the base Controller.
     */
    @FXML void commitPrefixAction() {
        commit_prefixes = prefixes;
        Stage stage = (Stage) cmtPrefixBtn.getScene().getWindow();
        stage.close();
    }

    /**
     * Close the window.
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
     * @param extFilter the list of extension filters, for easy access to specific file types.
     * @param owner the window that owns the dialog.
     * @return the file the user has chosen to save to, or null otherwise.
     */
    private File showSaveFileDialog(FileChooser.ExtensionFilter extFilter, Window owner) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialFileName("prefixes.txt");
        fileChooser.setTitle("Save Prefixes");
        fileChooser.setSelectedExtensionFilter(extFilter);
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        return fileChooser.showSaveDialog(owner);
    }

    /**
     * Creates a load file dialog, which prompts the user to load from a specific file.
     * @param owner the window that owns the dialog.
     * @return the file that will be loaded from.
     */
    private File showLoadFileDialog(Window owner){
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load Prefixes");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        return fileChooser.showOpenDialog(owner);
    }

    @Override
    public void setData(ArrayList<String> data) {
        prefixes = data;
        prefixList.setItems(FXCollections.observableArrayList(prefixes));
    }

    @Override
    public ArrayList<String> getData() {
        return commit_prefixes;
    }
}
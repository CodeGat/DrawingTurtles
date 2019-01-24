package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextInputDialog;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PrefixMenuController extends Controller {
    private static final Logger LOGGER = Logger.getLogger(PrefixMenuController.class.getName());

    @FXML Button addPrefixBtn, remPrefixBtn, renPrefixBtn, clrPrefixBtn, savPrefixBtn, lodPrefixBtn, cmtPrefixBtn,
            canPrefixBtn;
    @FXML ListView prefixList;

    private ArrayList<String> prefixes;
    String e;

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
     * on clicking 'Clear Prefixes' button, removes existing user prefixes, excepting the base ones (owl, rdf, rdfs).
     */
    @FXML protected void clearPrefixAction() {
        prefixes.clear();
        statusLbl.setText("Prefixes cleared. ");
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

    public void setPrefixes(ArrayList<String> initialPrefixes){
        prefixes = initialPrefixes;
    }

    public void setPrefs(String eee){
        e = eee;
        System.out.println(e);
    }

    @FXML void removePrefixAction() {

    }

    @FXML void renamePrefixAction() {

    }

    @FXML void commitPrefixAction() {

    }

    @FXML void cancelPrefixAction() {

    }
}

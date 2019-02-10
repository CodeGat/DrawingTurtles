package controller;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

/**
 * The controller for view.newClassDialog.fxml.
 */
public class NewClassDialogController extends AbstractDataSharingController<String> implements Initializable {
    private final BooleanProperty isClasslike = new SimpleBooleanProperty(false);
    private final BooleanProperty isLiterallike = new SimpleBooleanProperty(false);

    private ArrayList<String> commitData = new ArrayList<>();

    @FXML TextField classNameTfd;
    @FXML Label dataTypeLbl;
    @FXML Button cmtBtn, cancelBtn;

    /**
     * Adds listeners for the BooleanProperties that determine what to show in the dataTypeLbl depending on the type
     *    of graph element - Instance or Global Literal, or Class.
     * Adds listener for change on the textfield to help facilitate the determination of the above BooleanProperties.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        classNameTfd.textProperty().addListener(((observable, oldValue, newValue) -> {
            final String stringLitRegex   = "\".+\"";
            final String otherLitRegex    = "\".+\"\\^\\^.*";
            final String langLitRegex     = "\".+\"@.*";
            final String instanceLitRegex = "(?<!\")[^:]*(?!\")";
            final String booleanLitRegex  = "true|false";
            final String integerLitRegex  = "[+\\-]?\\d+";
            final String decimalLitRegex  = "[+\\-]?\\d*\\.\\d+";
            final String doubleLitRegex   = "([+\\-]?\\d+\\.\\d+|[+\\-]?\\.\\d+|[+\\-]?\\d+)[Ee][+\\-]\\d+";

            final String literalRegex = stringLitRegex + "|" + otherLitRegex + "|" + langLitRegex + "|" +
                    instanceLitRegex + "|" + booleanLitRegex + "|" + integerLitRegex + "|" + decimalLitRegex + "|" +
                    doubleLitRegex;
            final String classRegex = "([a-z]*:[^\" ]*)?";

            if (newValue.matches(classRegex)){
                isClasslike.setValue(true);
                isLiterallike.setValue(false);
            } else if (newValue.matches(literalRegex)){
                isClasslike.setValue(false);
                isLiterallike.setValue(true);
                if (newValue.matches(stringLitRegex + "|" + langLitRegex)) dataTypeLbl.setText("xsd:string");
                else if (newValue.matches(booleanLitRegex)) dataTypeLbl.setText("xsd:boolean");
                else if (newValue.matches(integerLitRegex)) dataTypeLbl.setText("xsd:integer");
                else if (newValue.matches(decimalLitRegex)) dataTypeLbl.setText("xsd:decimal");
                else if (newValue.matches(doubleLitRegex)) dataTypeLbl.setText("xsd:double");
                else if (newValue.matches(otherLitRegex)) {
                    final String[] otherParts = newValue.split("\\^\\^");
                    dataTypeLbl.setText(otherParts.length > 1 ? otherParts[1] : "");
                } else dataTypeLbl.setText("");
            } else {
                isClasslike.setValue(false);
                isLiterallike.setValue(false);
            }
        }));

        isLiterallike.addListener(((observable, oldValue, newValue) -> {
            if (!observable.getValue().booleanValue()) dataTypeLbl.setText("");
        }));

        isClasslike.addListener(((observable, oldValue, newValue) -> {
            if (observable.getValue().booleanValue()) dataTypeLbl.setText("");
        }));
    }

    /**
     * Add data to be commited, and close the Window.
     */
    @FXML void addNewClassAction() {
        commitData.add(classNameTfd.getText());
        commitData.add(dataTypeLbl.getText());
        Stage stage = (Stage) cmtBtn.getScene().getWindow();
        stage.close();
    }

    /**
     * Close the Window.
     */
    @FXML void cancelAction() {
        Stage stage = (Stage) cancelBtn.getScene().getWindow();
        stage.close();
    }

    /**
     * Simplifies the addition of a new Class.
     * @param keyEvent the key pressed.
     */
    @FXML void keyPressedAction(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.ENTER) addNewClassAction();
    }

    /**
     * No data is needed in this Controller
     * @param data the data to be passed to this controller.
     */
    @Override
    public void setData(ArrayList<String> data) {}

    /**
     * Determine the data the Controller can access, if it exists.
     * @return the data accessable by the Controller.
     */
    @Override
    public ArrayList<String> getData() {
        return commitData;
    }
}

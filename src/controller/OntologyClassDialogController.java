package controller;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ResourceBundle;

public class OntologyClassDialogController extends AbstractDataSharingController<String> implements Initializable {
    @FXML Button cmtBtn, cancelBtn;
    @FXML TextField typeTfd, nameTfd, labelTfd;
    @FXML TextArea commentTxa;

    private final BooleanProperty isClasslike = new SimpleBooleanProperty(false);
    private final BooleanProperty isLiterallike = new SimpleBooleanProperty(false);

    private ArrayList<String> commit_data = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        final String stringLitRegex = "\".+\"";
        final String otherLitRegex = "\".+\"\\^\\^.*";
        final String langLitRegex = "\".+\"@.*";
        final String instanceLitRegex = "(?<!\")[^:]*(?!\")";
        final String booleanLitRegex = "true|false";
        final String integerLitRegex = "[+\\-]?\\d+";
        final String decimalLitRegex = "[+\\-]?\\d*\\.\\d+";
        final String doubleLitRegex = "([+\\-]?\\d+\\.\\d+|[+\\-]?\\.\\d+|[+\\-]?\\d+)[Ee][+\\-]\\d+";

        final String literalRegex = stringLitRegex + "|" + otherLitRegex + "|" + langLitRegex + "|" +
                instanceLitRegex + "|" + booleanLitRegex + "|" + integerLitRegex + "|" + decimalLitRegex + "|" +
                doubleLitRegex;
        final String classRegex = "([a-z]*:[^\" ]*)?";

        nameTfd.textProperty().addListener(((observable, oldValue, newValue) -> {
            if (newValue.matches(classRegex)){
                isClasslike.setValue(true);
                isLiterallike.setValue(false);
            } else if (newValue.matches(literalRegex)){
                isClasslike.setValue(false);
                isLiterallike.setValue(true);
                if (newValue.matches(stringLitRegex + "|" + langLitRegex)) typeTfd.setText("xsd:string");
                else if (newValue.matches(booleanLitRegex)) typeTfd.setText("xsd:boolean");
                else if (newValue.matches(integerLitRegex)) typeTfd.setText("xsd:integer");
                else if (newValue.matches(decimalLitRegex)) typeTfd.setText("xsd:decimal");
                else if (newValue.matches(doubleLitRegex)) typeTfd.setText("xsd:double");
                else if (newValue.matches(otherLitRegex)) {
                    final String[] otherParts = newValue.split("\\^\\^");
                    typeTfd.setText(otherParts.length > 1 ? otherParts[1] : "");
                } else typeTfd.setText("");
            } else {
                isClasslike.setValue(false);
                isLiterallike.setValue(false);
            }
        }));

        isClasslike.addListener(((observable, oldValue, newValue) -> {
            if (observable.getValue().booleanValue()){
                labelTfd.setDisable(false);
                commentTxa.setDisable(false);
            } else {
                labelTfd.setDisable(true);
                commentTxa.setDisable(true);
                labelTfd.setText("");
                commentTxa.setText("");
            }
        }));

        isLiterallike.addListener(((observable, oldValue, newValue) -> {
            if (observable.getValue().booleanValue()) typeTfd.setDisable(false);
            else {
                typeTfd.setDisable(true);
                typeTfd.setText("");
            }
        }));
    }

    @Override
    public void setData(ArrayList<String> data) {}

    @Override
    public ArrayList<String> getData() {
        return commit_data;
    }

    @FXML void addNewClassAction() {
        commit_data.addAll(Arrays.asList(nameTfd.getText(), typeTfd.getText(), labelTfd.getText(), commentTxa.getText()));
        Stage stage = (Stage) cmtBtn.getScene().getWindow();
        stage.close();
    }

    @FXML void cancelClassAction() {
        Stage stage = (Stage) cancelBtn.getScene().getWindow();
        stage.close();
    }
}

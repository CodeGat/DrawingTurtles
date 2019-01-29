package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Arrays;

public class OntologyClassDialogController extends AbstractDataSharingController<String> {
    @FXML Button cmtBtn, cancelBtn;
    @FXML TextField nameTfd, labelTfd;
    @FXML TextArea commentTxa;

    private ArrayList<String> commit_data = new ArrayList<>();

    @Override
    public void setData(ArrayList<String> data) {}

    @Override
    public ArrayList<String> getData() {
        return commit_data;
    }

    @FXML void addNewClassAction() {
        commit_data.addAll(Arrays.asList(nameTfd.getText(), labelTfd.getText(), commentTxa.getText()));
        Stage stage = (Stage) cmtBtn.getScene().getWindow();
        stage.close();
    }

    @FXML void cancelClassAction() {
        Stage stage = (Stage) cancelBtn.getScene().getWindow();
        stage.close();
    }
}

package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.util.ArrayList;

public class InstructionsController extends DataSharingController<String> {
    @FXML Button closeBtn;



    @FXML void closeAction() { ((Stage) closeBtn.getScene().getWindow()).close(); }

    @Override public void setData(ArrayList<String> data) {}
    @Override public ArrayList<String> getData() { return null; }
}

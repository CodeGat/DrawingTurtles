package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.stage.Stage;

import java.util.ArrayList;

/**
 * Controller for the Options Menu.
 */
public class OptionsMenuController extends AbstractDataSharingController<Boolean> {
    @FXML Button cancelBtn, commitBtn;
    @FXML CheckBox collectionsCbx, blankCbx;

    private ArrayList<Boolean> commit_config = new ArrayList<>();

    /**
     * Commits the changes to the base Controller.
     */
    @FXML void commitConfigBtn() {
        commit_config.add(collectionsCbx.isSelected());
        commit_config.add(blankCbx.isSelected());
        Stage stage = (Stage) commitBtn.getScene().getWindow();
        stage.close();
    }

    /**
     * Closes the window.
     */
    @FXML void cancelConfigBtn() {
        Stage stage = (Stage) cancelBtn.getScene().getWindow();
        stage.close();
    }

    @Override
    public void setData(ArrayList<Boolean> data) {
        collectionsCbx.setSelected(data.get(0));
        blankCbx.setSelected(data.get(1));
    }

    @Override
    public ArrayList<Boolean> getData() {
        return commit_config;
    }
}

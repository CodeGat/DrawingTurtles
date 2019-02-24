package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.stage.Stage;

import java.util.ArrayList;

/**
 * Controller for view.optionsmenu.fxml.
 */
public class OptionsMenuController extends AbstractDataSharingController<Boolean> {
    @FXML Button cancelBtn, commitBtn;
    @FXML CheckBox collectionsCbx, blankCbx, ontologyCbx;

    private ArrayList<Boolean> commit_config;

    /**
     * Adds the options state for commital, and closes the Window.
     */
    @FXML void commitConfigBtn() {
        commit_config = new ArrayList<>();
        commit_config.add(collectionsCbx.isSelected());
        commit_config.add(blankCbx.isSelected());
        commit_config.add(ontologyCbx.isSelected());
        Stage stage = (Stage) commitBtn.getScene().getWindow();
        stage.close();
    }

    /**
     * Closes the Window.
     */
    @FXML void cancelConfigBtn() {
        Stage stage = (Stage) cancelBtn.getScene().getWindow();
        stage.close();
    }

    /**
     * get the current options data from the Controller.
     * @param data the data to be passed.
     */
    @Override
    public void setData(ArrayList<Boolean> data) {
        collectionsCbx.setSelected(data.get(0));
        blankCbx.setSelected(data.get(1));
        ontologyCbx.setSelected(data.get(2));
    }

    /**
     * Pass the data from this controller to the calling Controller.
     * @return the data (options) if it exists.
     */
    @Override
    public ArrayList<Boolean> getData() {
        return commit_config;
    }
}

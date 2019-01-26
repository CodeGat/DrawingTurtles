package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.stage.Stage;

import java.util.ArrayList;

/**
 * Controller for the Options Menu.
 */
public class OptionsMenuController extends Controller {
    @FXML Button cancelBtn, commitBtn;
    @FXML CheckBox collectionsCbx, blankCbx, ontologyCbx;
    private ArrayList<Boolean> config;

    /**
     * Sets the config as defined in the Controller.
     * @param initial_config the config in the Controller.
     */
    @SuppressWarnings("WeakerAccess")
    public void setConfig(ArrayList<Boolean> initial_config){
        config = initial_config;
        collectionsCbx.setSelected(config.get(0));
        blankCbx.setSelected(config.get(1));
        ontologyCbx.setSelected(config.get(2));
    }

    /**
     * Commits the changes to the base Controller.
     */
    @FXML void commitConfigBtn() {
        config.set(0, collectionsCbx.isSelected());
        config.set(1, blankCbx.isSelected());
        config.set(2, ontologyCbx.isSelected());
        super.config = config;
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
}

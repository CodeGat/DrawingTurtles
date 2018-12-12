import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;

import java.util.ArrayList;
import java.util.Optional;

public class Controller {
    enum Type {
        CLASS, PROPERTY, LITERAL, UNKNOWN
    }

    public BorderPane root;
    public Button classBtn;
    public Button propBtn;
    public Button literalBtn;
    public Button addPrefixBtn;
    public Button exportTllBtn;
    public Button exportPngBtn;
    public Label statusLbl;
    public Pane drawPane;
    public Label drawStatusLbl;

    private Type selectedType = Type.CLASS;
    private ArrayList<String> prefixes = new ArrayList<>();

    @FXML protected void classSelectAction() {
        drawStatusLbl.setText("Class selected");
        selectedType = Type.CLASS;
    }

    @FXML protected void propSelectAction()  {
        drawStatusLbl.setText("Property selected");
        selectedType = Type.PROPERTY;
    }

    @FXML protected void literalSelectAction() {
        drawStatusLbl.setText("Literal selected");
        selectedType = Type.LITERAL;
    }

    @FXML protected void addPrefixAction() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Ontology Prefix");
        dialog.setHeaderText("Of the form: <prefix name> : <URI prefix>");

        Optional<String> dialogResult = dialog.showAndWait();
        dialogResult.ifPresent(prefix -> {
            if (prefix.matches("[a-z]* : .*")){
                prefixes.add(prefix);
                System.out.println("Added");
            } else {
                System.out.println("Not Added");
                showPrefixMalformedAlert();
            }
        });
    }

    @FXML protected void exportTtlAction() {}
    @FXML protected void exportPngAction() {}

    @FXML public void inPane(MouseEvent mouseEvent) {
        statusLbl.setText("In Pane at (" + mouseEvent.getX() + ", " + mouseEvent.getY() + ").");
    }


    private void showPrefixMalformedAlert() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText("Prefix was not of the form:\n" +
                "\"<prefix name> : <URI prefix>\"\n" +
                "The malformed prefix was discarded, try again.\n" +
                "Example: \"foaf : http://xmlns.com/foaf/0.1/\"");
        alert.showAndWait();
    }
}

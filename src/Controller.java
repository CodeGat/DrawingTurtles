import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;

public class Controller {
    enum Type {
        CLASS, PROPERTY, LITERAL, UNKNOWN
    }

    public Button classBtn;
    public Button propBtn;
    public Button exportTllBtn;
    public Button exportPngBtn;
    public Label statusLbl;
    public Pane drawPane;
    public Label drawStatusLbl;
    public Button literalBtn;

    private Type selectedType = Type.CLASS;

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

    @FXML protected void exportTtlAction() {}
    @FXML protected void exportPngAction() {}

    @FXML public void inPane(MouseEvent mouseEvent) {
        statusLbl.setText("In Pane at (" + mouseEvent.getX() + ", " + mouseEvent.getY() + ").");
    }
}

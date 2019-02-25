package controller;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import javafx.util.Pair;
import model.conceptual.Vertex;
import model.dataintegration.Correlation;
import model.dataintegration.DataIntegrator;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * The controller for view.correlateDialog.fxml.
 */
public class CorrelateDialogController extends DataSharingController<DataIntegrator> implements Initializable {
    @FXML Button addManualCorrBtn, addHeaderBtn, commitBtn, cancelBtn;
    @FXML ListView<String> csvHeaderList;
    @FXML ListView<String> csvTtlCorrelationList;
    @FXML ListView<String> ttlHeaderList;

    private BooleanProperty isSelectionFromCsvList = new SimpleBooleanProperty(false);
    private BooleanProperty isSelectionFromTtlList = new SimpleBooleanProperty(false);
    private BooleanBinding isSelectionFromBothLists = isSelectionFromCsvList.and(isSelectionFromTtlList);

    private BooleanProperty isCsvListEmpty = new SimpleBooleanProperty(false);
    private BooleanProperty isTtlListEmpty = new SimpleBooleanProperty(false);

    private ArrayList<Correlation> correlations = new ArrayList<>();
    private ArrayList<String> uncorrelatedCsvHeaders;
    private ArrayList<Vertex> uncorrelatedTtlClasses;
    private ArrayList<String> uncorrelatedTtlClassNames;

    private DataIntegrator dataIntegrator;

    /**
     * Initializes listeners for BooleanProperties relating to the state of the csvHeaderList, ttlHeaderList.
     */
    @Override public void initialize(URL location, ResourceBundle resources) {
        isSelectionFromBothLists.addListener((observable, oldV, newV) -> {
            if (observable.getValue().booleanValue()) addManualCorrBtn.setDisable(false);
            else addManualCorrBtn.setDisable(true);
        });

        isCsvListEmpty.addListener(((observable, oldV, newV) -> {
            if (observable.getValue().booleanValue()) commitBtn.setDisable(false);
            else commitBtn.setDisable(true);
        }));

        csvHeaderList.getSelectionModel().selectedItemProperty().addListener((observable, oldV, newV) -> {
            if (csvHeaderList.getItems().isEmpty()) isCsvListEmpty.setValue(true);
            if      (oldV == null) isSelectionFromCsvList.setValue(true);
            else if (newV == null) isSelectionFromCsvList.setValue(false);
        });

        ttlHeaderList.getSelectionModel().selectedItemProperty().addListener(((observable, oldV, newV) -> {
            if (ttlHeaderList.getItems().isEmpty()) isTtlListEmpty.setValue(true);
            if      (oldV == null) isSelectionFromTtlList.setValue(true);
            else if (newV == null) isSelectionFromTtlList.setValue(false);
        }));
    }

    /**
     * Once the user has selected a .csv header and a .ttl class that are to be correlated from the lists,
     *    and clicked the 'Add' button, remove those from the list and add it to the correlated list.
     */
    @FXML void addManualCorrelationAction() {
        String selectedCsvHeader = csvHeaderList.getSelectionModel().getSelectedItem();
        int index = Integer.parseInt(selectedCsvHeader.split(" ")[0]);
        String csvAttribute = selectedCsvHeader.split(" ")[1];

        String selectedTtlClass = ttlHeaderList.getSelectionModel().getSelectedItem();
        Vertex ttlClass = null;

        for (Vertex klass : uncorrelatedTtlClasses) {
            String klassName = klass.getName().toLowerCase();
            String selectedKlass = selectedTtlClass.toLowerCase();

            if (klassName.equals(selectedKlass)) {
                ttlClass = klass;
                uncorrelatedTtlClasses.remove(klass);
                uncorrelatedTtlClassNames.remove(selectedTtlClass);
                uncorrelatedCsvHeaders.remove(selectedCsvHeader);
                ttlHeaderList.getItems().remove(selectedTtlClass);
                csvHeaderList.getItems().remove(selectedCsvHeader);
                ttlHeaderList.getSelectionModel().clearSelection();
                csvHeaderList.getSelectionModel().clearSelection();
                break;
            }
        }

        correlations.add(new Correlation(index, csvAttribute, ttlClass));
        ArrayList<String> correlationStrList = correlations
                        .stream()
                        .map(Correlation::toString)
                        .collect(Collectors.toCollection(ArrayList::new));
        csvTtlCorrelationList.setItems(FXCollections.observableArrayList(correlationStrList));
    }

    /**
     * Stub for adding unique data .csv records.
     */
    @FXML void addHeaderAction() {}

    /**
     * Close the Window and mark the modified DataIntegrator for commital.
     */
    @FXML void commitCorrelationAction() {
        dataIntegrator.setCorrelations(correlations);
        dataIntegrator.setUncorrelated(new Pair<>(uncorrelatedCsvHeaders, uncorrelatedTtlClasses));
        Stage stage = (Stage) commitBtn.getScene().getWindow();
        stage.close();
    }

    /**
     * Close the Window.
     */
    @FXML void cancelCorrelationAction() {
        Stage stage = (Stage) cancelBtn.getScene().getWindow();
        stage.close();
    }

    /**
     * Set the data passed from the Controller to variables in the CorrelateDialogController.
     * @param data the data passed to the CorrelateDialogController to the Controller.
     */
    @Override
    public void setData(ArrayList<DataIntegrator> data) {
        dataIntegrator = data.get(0);
        correlations = dataIntegrator.getCorrelations();
        ArrayList<String> correlationStrings = correlations
                .stream()
                .map(Correlation::toString)
                .collect(Collectors.toCollection(ArrayList::new));

        csvTtlCorrelationList.setItems(FXCollections.observableArrayList(correlationStrings));

        uncorrelatedCsvHeaders = dataIntegrator.getUncorrelated().getKey();
        csvHeaderList.setItems(FXCollections.observableArrayList(uncorrelatedCsvHeaders));
        uncorrelatedTtlClasses = dataIntegrator.getUncorrelated().getValue();
        uncorrelatedTtlClassNames = uncorrelatedTtlClasses
                .stream()
                .map(Vertex::getName)
                .collect(Collectors.toCollection(ArrayList::new));
        ttlHeaderList.setItems(FXCollections.observableArrayList(uncorrelatedTtlClassNames));
    }

    /**
     * Add the data that the Controller can access from this class.
     * @return the data that can be accessed when the Controller calls this method.
     */
    @Override
    public ArrayList<DataIntegrator> getData() {
        ArrayList<DataIntegrator> data = new ArrayList<>();
        data.add(dataIntegrator);
        return data;
    }
}

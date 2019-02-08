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

public class CorrelateDialogController extends AbstractDataSharingController<DataIntegrator> implements Initializable {
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

    @FXML void addHeaderAction() {

    }

    @FXML void commitCorrelationBtn() {
        dataIntegrator.setCorrelations(correlations);
        dataIntegrator.setUncorrelated(new Pair<>(uncorrelatedCsvHeaders, uncorrelatedTtlClasses));
        Stage stage = (Stage) commitBtn.getScene().getWindow();
        stage.close();
    }

    @FXML void cancelCorrelationBtn() {
        Stage stage = (Stage) cancelBtn.getScene().getWindow();
        stage.close();
    }

    @Override
    public void setData(ArrayList<DataIntegrator> data) {
        DataIntegrator generator = data.get(0);

        dataIntegrator = generator;
        correlations = dataIntegrator.getCorrelations();
        ArrayList<String> correlationStrings = correlations
                .stream()
                .map(Correlation::toString)
                .collect(Collectors.toCollection(ArrayList::new));

        csvTtlCorrelationList.setItems(FXCollections.observableArrayList(correlationStrings));

        uncorrelatedCsvHeaders = dataIntegrator.getUncorrelated().getKey();
        csvHeaderList.setItems(FXCollections.observableArrayList(uncorrelatedCsvHeaders));
        uncorrelatedTtlClasses = generator.getUncorrelated().getValue();
        uncorrelatedTtlClassNames = uncorrelatedTtlClasses
                .stream()
                .map(Vertex::getName)
                .collect(Collectors.toCollection(ArrayList::new));
        ttlHeaderList.setItems(FXCollections.observableArrayList(uncorrelatedTtlClassNames));
    }

    @Override
    public ArrayList<DataIntegrator> getData() {
        ArrayList<DataIntegrator> data = new ArrayList<>();
        data.add(dataIntegrator);
        return data;
    }
}

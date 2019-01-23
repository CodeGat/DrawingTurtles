package controller;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.util.Pair;
import model.conceptual.Vertex;
import model.conversion.rdfxml.Correlations;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class CorrelateDialogController implements Initializable {
    @FXML Button addManualCorrBtn, remManualCorrBtn, addHeaderBtn, commitBtn;
    @FXML ListView<String> csvHeaderList;
    @FXML ListView<Correlations> csvTtlCorrelationList;
    @FXML ListView<String> ttlHeaderList;

    private BooleanProperty isSelectionFromCsvList = new SimpleBooleanProperty(false);
    private BooleanProperty isSelectionFromTtlList = new SimpleBooleanProperty(false);
    private BooleanBinding isSelectionFromBothLists = isSelectionFromCsvList.and(isSelectionFromTtlList);

    private Correlations      correlations = new Correlations();
    private ArrayList<String> uncorrelatedCsvHeaders;
    private ArrayList<Vertex> uncorrelatedTtlClasses;
    private ArrayList<String> uncorrelatedTtlClassNames;

    @Override public void initialize(URL location, ResourceBundle resources) {
        isSelectionFromBothLists.addListener((observable, oldValue, newValue) -> {
            if (observable.getValue().booleanValue()) addManualCorrBtn.setDisable(false);
            else addManualCorrBtn.setDisable(true);
        });
    }

    void setUncorrelated(Pair<ArrayList<String>, ArrayList<Vertex>> uncorrelated){
        uncorrelatedCsvHeaders = uncorrelated.getKey();
        csvHeaderList.setItems(FXCollections.observableArrayList(uncorrelatedCsvHeaders));
        uncorrelatedTtlClasses = uncorrelated.getValue();
        uncorrelatedTtlClassNames = uncorrelatedTtlClasses
                    .stream()
                    .map(Vertex::getName)
                    .collect(Collectors.toCollection(ArrayList::new));
        ttlHeaderList.setItems(FXCollections.observableArrayList(uncorrelatedTtlClassNames));

        csvHeaderList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            System.out.println(oldValue + " -> " + newValue);
            if (oldValue == null) isSelectionFromCsvList.setValue(true);
            else if (newValue == null) isSelectionFromCsvList.setValue(false);
        });
        ttlHeaderList.getSelectionModel().selectedItemProperty().addListener(((observable, oldValue, newValue) -> {
            System.out.println(oldValue + " -> " + newValue);
            if (oldValue == null) isSelectionFromTtlList.setValue(true);
            else if (newValue == null) isSelectionFromTtlList.setValue(false);
        }));
    }

    @FXML void addManualCorrelationAction() {
        String selectedCsvHeader = csvHeaderList.getSelectionModel().getSelectedItem();
        Integer index = Integer.parseInt(selectedCsvHeader.split(" ")[0]);
        String csvAttribute = selectedCsvHeader.split(" ")[1];
        String selectedTtlClass = ttlHeaderList.getSelectionModel().getSelectedItem();
        Vertex ttlClass = null;
        for (Vertex klass : uncorrelatedTtlClasses) {
            String lck = klass.getName().toLowerCase();
            String sk  = selectedTtlClass.toLowerCase();

            if (klass.getName().toLowerCase().matches(".*:" + selectedTtlClass.toLowerCase())) {
                ttlClass = klass;
                uncorrelatedTtlClasses.remove(klass);
                uncorrelatedTtlClassNames.remove(selectedTtlClass);
                uncorrelatedCsvHeaders.remove(selectedCsvHeader);
//                csvHeaderList.refresh();
//                ttlHeaderList.refresh();
//                ttlHeaderList.getItems().remove(selectedTtlClass);
//                csvHeaderList.getItems().remove(selectedCsvHeader);
                break;
            }
        }

        correlations.addCorrelation(index, csvAttribute, ttlClass);
    }

    @FXML void removeManualCorrelationAction() {

    }

    @FXML void addHeaderAction() {

    }

    @FXML void commitCorrelationBtn() {

    }
}

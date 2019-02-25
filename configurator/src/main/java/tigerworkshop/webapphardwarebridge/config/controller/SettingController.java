package tigerworkshop.webapphardwarebridge.config.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class SettingController implements Initializable {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @FXML
    private TableView<Pair<String, String>> tablePrinter;
    @FXML
    private TableColumn<Pair<String, String>, String> columnPrintType;
    @FXML
    private TableColumn<Pair<String, String>, String> columnPrinter;
    @FXML
    private Button buttonLog;
    @FXML
    private Button buttonSave;
    @FXML
    private Button buttonReset;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // PrinterMapping List
        ObservableList<String> printerList = FXCollections.observableArrayList();
        printerList.addAll();

        // PrinterMapping List
        tablePrinter.setEditable(true);
        tablePrinter.getSelectionModel().setCellSelectionEnabled(true);

        columnPrintType.setCellValueFactory(new PropertyValueFactory<>("type"));

        columnPrinter.setCellValueFactory(new PropertyValueFactory<>("printer"));
        columnPrinter.setCellFactory(ComboBoxTableCell.forTableColumn(printerList));
        columnPrinter.setOnEditCommit(new EventHandler<TableColumn.CellEditEvent<Pair<String, String>, String>>() {
            @Override
            public void handle(TableColumn.CellEditEvent event) {
                Pair<String, String> printerMapping = (Pair<String, String>) event.getTableView().getItems().get(event.getTablePosition().getRow());
                printerMapping.setValue((String) event.getNewValue());
            }
        });

        loadValues();

        buttonLog.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {
                    Desktop.getDesktop().open(new File("log"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        // Save Values
        buttonSave.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                saveValues();
            }
        });

        // Reset Values
        buttonReset.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                loadValues();
            }
        });
    }

    private void loadValues() {
        logger.debug("loadValues");

        /*
        ObservableList<PrinterMapping> printerMappingList = FXCollections.observableArrayList(PrinterManager.getInstance().getMapping());
        tablePrinter.setItems(printerMappingList);
        */
    }

    private void saveValues() {
        logger.debug("saveValues");

        /*
        ObservableList<PrinterMapping> list = tablePrinter.getItems();
        for(PrinterMapping map : list) {
            PrinterManager.getInstance().saveMapping(map);
        }
        */
    }
}
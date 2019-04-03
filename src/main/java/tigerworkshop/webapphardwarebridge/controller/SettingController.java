package tigerworkshop.webapphardwarebridge.controller;

import com.fazecast.jSerialComm.SerialPort;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;
import tigerworkshop.webapphardwarebridge.services.SettingService;
import tigerworkshop.webapphardwarebridge.utils.ObservableStringPair;

import javax.print.PrintService;
import java.awt.*;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

@SuppressWarnings("Duplicates")
public class SettingController implements Initializable {
    @FXML
    private TableView<ObservableStringPair> tableSerial;
    @FXML
    private TableColumn<ObservableStringPair, String> columnSerialType;
    @FXML
    private TableColumn<ObservableStringPair, String> columnPort;

    @FXML
    private TableView<ObservableStringPair> tablePrinter;
    @FXML
    private TableColumn<ObservableStringPair, String> columnPrintType;
    @FXML
    private TableColumn<ObservableStringPair, String> columnPrinter;
    @FXML
    private Button buttonLog;
    @FXML
    private Button buttonSave;
    @FXML
    private Button buttonSaveAndClose;
    @FXML
    private Button buttonReset;
    private ObservableList<ObservableStringPair> printerMappingList = FXCollections.observableArrayList();
    private ObservableList<ObservableStringPair> serialMappingList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Printer List
        ObservableList<String> printerList = FXCollections.observableArrayList();
        printerList.addAll(listPrinters());

        tablePrinter.getSelectionModel().setCellSelectionEnabled(true);
        columnPrintType.setCellValueFactory(new PropertyValueFactory<>("left"));
        columnPrintType.setCellFactory(TextFieldTableCell.forTableColumn());
        columnPrinter.setCellValueFactory(new PropertyValueFactory<>("right"));
        columnPrinter.setCellFactory(ComboBoxTableCell.forTableColumn(printerList));

        MenuItem addItemPrinter = new MenuItem("Add");
        addItemPrinter.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                printerMappingList.add(ObservableStringPair.of("KEY", "Select Printer"));
            }
        });

        tablePrinter.setRowFactory(
                new Callback<TableView<ObservableStringPair>, TableRow<ObservableStringPair>>() {
                    @Override
                    public TableRow<ObservableStringPair> call(TableView<ObservableStringPair> tableView) {
                        final TableRow<ObservableStringPair> row = new TableRow<>();
                        final ContextMenu rowMenu = new ContextMenu();

                        MenuItem removeItemPrinter = new MenuItem("Delete");
                        removeItemPrinter.setOnAction(new EventHandler<ActionEvent>() {
                            @Override
                            public void handle(ActionEvent event) {
                                tablePrinter.getItems().remove(row.getItem());
                            }
                        });
                        rowMenu.getItems().addAll(addItemPrinter, removeItemPrinter);

                        final ContextMenu emptyMenu = new ContextMenu();
                        emptyMenu.getItems().addAll(addItemPrinter);

                        // only display context menu for non-null items:
                        row.contextMenuProperty().bind(
                                Bindings.when(Bindings.isNotNull(row.itemProperty()))
                                        .then(rowMenu)
                                        .otherwise(emptyMenu));
                        return row;
                    }
                }
        );

        tablePrinter.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent t) {
                ContextMenu contextMenu = new ContextMenu();
                contextMenu.getItems().add(addItemPrinter);
                if (t.getButton() == MouseButton.SECONDARY) {
                    contextMenu.show(tableSerial, t.getScreenX(), t.getScreenY());
                }
            }
        });

        // Serial List
        ObservableList<String> serialList = FXCollections.observableArrayList();
        serialList.addAll(listSerials());

        tableSerial.getSelectionModel().setCellSelectionEnabled(true);
        columnSerialType.setCellValueFactory(new PropertyValueFactory<>("left"));
        columnSerialType.setCellFactory(TextFieldTableCell.forTableColumn());
        columnPort.setCellValueFactory(new PropertyValueFactory<>("right"));
        columnPort.setCellFactory(ComboBoxTableCell.forTableColumn(serialList));

        MenuItem addItemSerial = new MenuItem("Add");
        addItemSerial.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                serialMappingList.add(ObservableStringPair.of("KEY", "Select Port"));
            }
        });

        tableSerial.setRowFactory(
                new Callback<TableView<ObservableStringPair>, TableRow<ObservableStringPair>>() {
                    @Override
                    public TableRow<ObservableStringPair> call(TableView<ObservableStringPair> tableView) {
                        final TableRow<ObservableStringPair> row = new TableRow<>();
                        final ContextMenu rowMenu = new ContextMenu();

                        MenuItem removeItemSerial = new MenuItem("Delete");
                        removeItemSerial.setOnAction(new EventHandler<ActionEvent>() {
                            @Override
                            public void handle(ActionEvent event) {
                                tableSerial.getItems().remove(row.getItem());
                            }
                        });
                        rowMenu.getItems().addAll(addItemSerial, removeItemSerial);

                        final ContextMenu emptyMenu = new ContextMenu();
                        emptyMenu.getItems().addAll(addItemSerial);

                        // only display context menu for non-null items:
                        row.contextMenuProperty().bind(
                                Bindings.when(Bindings.isNotNull(row.itemProperty()))
                                        .then(rowMenu)
                                        .otherwise(emptyMenu));
                        return row;
                    }
                }
        );

        tableSerial.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent t) {
                ContextMenu contextMenu = new ContextMenu();
                contextMenu.getItems().add(addItemSerial);
                if (t.getButton() == MouseButton.SECONDARY) {
                    contextMenu.show(tableSerial, t.getScreenX(), t.getScreenY());
                }
            }
        });

        // Other controls
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
        buttonSaveAndClose.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                saveValues();
                Platform.exit();
                System.exit(0);
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

        loadValues();
    }

    private void loadValues() {
        SettingService settingService = SettingService.getInstance();

        // Printers
        HashMap<String, String> printerHashMap = settingService.getPrinters();
        for (Map.Entry<String, String> mapEntry : printerHashMap.entrySet()) {
            String key = mapEntry.getKey();
            String value = mapEntry.getValue();
            printerMappingList.add(ObservableStringPair.of(key, value));
        }
        tablePrinter.setItems(printerMappingList);

        // Serials
        HashMap<String, String> portHashMap = settingService.getSerials();
        for (Map.Entry<String, String> mapEntry : portHashMap.entrySet()) {
            String key = mapEntry.getKey();
            String value = mapEntry.getValue();
            serialMappingList.add(ObservableStringPair.of(key, value));
        }
        tableSerial.setItems(serialMappingList);
    }

    private void saveValues() {
        SettingService settingService = SettingService.getInstance();

        // Printers
        HashMap<String, String> printerHashMap = new HashMap<>();
        ObservableList<ObservableStringPair> printerList = tablePrinter.getItems();
        for (ObservableStringPair pair : printerList) {
            printerHashMap.put(pair.getLeft(), pair.getRight());
        }

        // Serials
        HashMap<String, String> serialHashMap = new HashMap<>();
        ObservableList<ObservableStringPair> serialList = tableSerial.getItems();
        for (ObservableStringPair pair : serialList) {
            serialHashMap.put(pair.getLeft(), pair.getRight());
        }

        settingService.setPrinters(printerHashMap);
        settingService.setSerials(serialHashMap);
        settingService.save();
    }

    private ArrayList<String> listPrinters() {
        ArrayList<String> printerList = new ArrayList<>();
        PrintService[] printServices = PrinterJob.lookupPrintServices();
        for (PrintService printService : printServices) {
            printerList.add(printService.getName());
        }
        return printerList;
    }

    private ArrayList<String> listSerials() {
        ArrayList<String> portList = new ArrayList<>();
        for (SerialPort port : SerialPort.getCommPorts()) {
            portList.add(port.getSystemPortName());
        }
        return portList;
    }
}
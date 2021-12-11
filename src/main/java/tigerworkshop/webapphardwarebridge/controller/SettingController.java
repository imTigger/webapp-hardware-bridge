package tigerworkshop.webapphardwarebridge.controller;

import com.fazecast.jSerialComm.SerialPort;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.util.Callback;
import tigerworkshop.webapphardwarebridge.responses.Setting;
import tigerworkshop.webapphardwarebridge.services.SettingService;
import tigerworkshop.webapphardwarebridge.utils.ObservableStringPair;

import javax.print.PrintService;
import java.awt.*;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

@SuppressWarnings("Duplicates")
public class SettingController implements Initializable {
    @FXML
    private TextField textBind;
    @FXML
    private TextField textAddress;
    @FXML
    private TextField textPort;

    @FXML
    private CheckBox checkboxCloudProxyEnabled;
    @FXML
    private TextField textCloudProxyUrl;
    @FXML
    private TextField textCloudProxyTimeout;

    @FXML
    private CheckBox checkboxTlsEnabled;
    @FXML
    private CheckBox checkboxTLSSelfSigned;
    @FXML
    private TextField textTLSCert;
    @FXML
    private TextField textTLSKey;
    @FXML
    private TextField textTLSCaBundle;

    @FXML
    private CheckBox checkboxAuthenticationEnabled;
    @FXML
    private TextField textAuthenticationToken;
    @FXML
    private CheckBox checkboxIgnoreTLSCertificateError;

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
    private CheckBox checkboxFallbackToDefaultPrinter;
    private final ObservableList<ObservableStringPair> printerMappingList = FXCollections.observableArrayList();
    private final ObservableList<ObservableStringPair> serialMappingList = FXCollections.observableArrayList();

    @FXML
    private Button buttonLog;
    @FXML
    private Button buttonSave;
    @FXML
    private Button buttonSaveAndClose;
    @FXML
    private Button buttonLoadDefault;
    @FXML
    private Button buttonReset;
    private final SettingService settingService = SettingService.getInstance();
    @FXML
    private CheckBox checkboxAutoRotation;
    @FXML
    private TextField textDownloadTimeout;

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
                Stage stage = (Stage) buttonSaveAndClose.getScene().getWindow();
                stage.close();
            }
        });

        // Save Values
        buttonSave.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                saveValues();
            }
        });

        // Default Values
        buttonLoadDefault.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                loadDefaultValues();
            }
        });

        // Reset Values
        buttonReset.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                loadCurrentValues();
            }
        });

        loadValues();

        Setting.registerNewPrintTypeObserver(this);
    }

    private void loadCurrentValues() {
        try {
            settingService.loadCurrent();
        } catch (Exception e) {
            e.printStackTrace();
        }
        loadValues();
    }


    private void loadDefaultValues() {
        try {
            settingService.loadDefault();
        } catch (Exception e) {
            e.printStackTrace();
        }
        loadValues();
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

    private void loadValues() {
        Setting setting = settingService.getSetting();

        // General
        textBind.setText(setting.getBind());
        textAddress.setText(setting.getAddress());
        textPort.setText(Integer.toString(setting.getPort()));

        // Cloud Proxy
        checkboxCloudProxyEnabled.setSelected(setting.getCloudProxyEnabled());
        textCloudProxyUrl.setText(setting.getCloudProxyUrl());
        textCloudProxyTimeout.setText(Double.toString(setting.getCloudProxyTimeout()));

        // TLS
        checkboxTlsEnabled.setSelected(setting.getTLSEnabled());
        checkboxTLSSelfSigned.setSelected(setting.getTLSSelfSigned());
        textTLSCert.setText(setting.getTLSCert());
        textTLSKey.setText(setting.getTLSKey());
        textTLSCaBundle.setText(setting.getTLSCaBundle());

        // Authentication
        checkboxAuthenticationEnabled.setSelected(setting.getAuthenticationEnabled());
        textAuthenticationToken.setText(setting.getAuthenticationToken());

        // SSL Errors
        checkboxIgnoreTLSCertificateError.setSelected(setting.getIgnoreTLSCertificateErrorEnabled());

        // Printers
        printerMappingList.clear();
        HashMap<String, String> printerHashMap = setting.getPrinters();
        for (Map.Entry<String, String> mapEntry : printerHashMap.entrySet()) {
            String key = mapEntry.getKey();
            String value = mapEntry.getValue();
            printerMappingList.add(ObservableStringPair.of(key, value));
        }
        tablePrinter.setItems(printerMappingList);
        checkboxFallbackToDefaultPrinter.setSelected(setting.getFallbackToDefaultPrinter());
        checkboxAutoRotation.setSelected(setting.getAutoRotation());
        textDownloadTimeout.setText(Double.toString(setting.getDownloadTimeout()));

        // Serials
        serialMappingList.clear();
        HashMap<String, String> portHashMap = setting.getSerials();
        for (Map.Entry<String, String> mapEntry : portHashMap.entrySet()) {
            String key = mapEntry.getKey();
            String value = mapEntry.getValue();
            serialMappingList.add(ObservableStringPair.of(key, value));
        }
        tableSerial.setItems(serialMappingList);
    }

    private void saveValues() {
        Setting setting = settingService.getSetting();

        // General
        setting.setAddress(textAddress.getText());
        setting.setBind(textBind.getText());
        setting.setPort(Integer.parseInt(textPort.getText()));

        // Cloud Proxy
        setting.setCloudProxyEnabled(checkboxCloudProxyEnabled.isSelected());
        setting.setCloudProxyUrl(textCloudProxyUrl.getText());
        setting.setCloudProxyTimeout(Double.parseDouble(textCloudProxyTimeout.getText()));

        // TLS
        setting.setTLSEnabled(checkboxTlsEnabled.isSelected());
        setting.setTLSSelfSigned(checkboxTLSSelfSigned.isSelected());
        setting.setTLSCert(textTLSCert.getText());
        setting.setTLSKey(textTLSKey.getText());
        setting.setTLSCaBundle(textTLSCaBundle.getText());

        // Authentication
        setting.setAuthenticationEnabled(checkboxAuthenticationEnabled.isSelected());
        setting.setAuthenticationToken(textAuthenticationToken.getText());

        // SSL Errors
        setting.setIgnoreTLSCertificateErrorEnabled(checkboxIgnoreTLSCertificateError.isSelected());

        // Printers
        HashMap<String, String> printerHashMap = new HashMap<>();
        ObservableList<ObservableStringPair> printerList = tablePrinter.getItems();
        for (ObservableStringPair pair : printerList) {
            printerHashMap.put(pair.getLeft(), pair.getRight());
        }
        setting.setPrinters(printerHashMap);
        setting.setFallbackToDefaultPrinter(checkboxFallbackToDefaultPrinter.isSelected());
        setting.setAutoRotation(checkboxAutoRotation.isSelected());
        setting.setDownloadTimeout(Double.parseDouble(textDownloadTimeout.getText()));

        // Serials
        HashMap<String, String> serialHashMap = new HashMap<>();
        ObservableList<ObservableStringPair> serialList = tableSerial.getItems();
        for (ObservableStringPair pair : serialList) {
            serialHashMap.put(pair.getLeft(), pair.getRight());
        }
        setting.setSerials(serialHashMap);

        settingService.save();
    }

    /**
     * Add a new printType to the list when window is open
     *
     * @param type the printType
     * @param printerPlaceHolder a place holder
     */
    public void newPrintType(String type, String printerPlaceHolder) {

        Optional<ObservableStringPair> isAlreadyInTheList =
                printerMappingList.stream()
                        .filter(x -> x.getLeft().equalsIgnoreCase(type))
                        .findAny();

        if(!isAlreadyInTheList.isPresent()) {
            printerMappingList.add(new ObservableStringPair(type, printerPlaceHolder));
        }
    }
}
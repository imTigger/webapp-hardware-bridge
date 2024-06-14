package tigerworkshop.webapphardwarebridge.websocketservices;

import com.fazecast.jSerialComm.SerialPort;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.binary.Hex;
import tigerworkshop.webapphardwarebridge.dtos.Config;
import tigerworkshop.webapphardwarebridge.interfaces.GUIInterface;
import tigerworkshop.webapphardwarebridge.interfaces.WebSocketServerInterface;
import tigerworkshop.webapphardwarebridge.interfaces.WebSocketServiceInterface;
import tigerworkshop.webapphardwarebridge.utils.ThreadUtil;

import java.awt.*;
import java.nio.charset.Charset;
import java.util.Objects;

@Log4j2
public class SerialWebSocketService implements WebSocketServiceInterface {
    private WebSocketServerInterface server;
    private final GUIInterface guiInterface;

    private final Config.SerialMapping mapping;
    private final SerialPort serialPort;
    private byte[] writeBuffer = {};

    private Thread readThread;
    private Thread writeThread;
    private Thread monitorThread;

    private Boolean isRunning = true;

    private static final String BINARY = "BINARY";

    public SerialWebSocketService(GUIInterface newGUIInterface, Config.SerialMapping newMapping) {
        log.info("Starting SerialWebSocketService on {}", newMapping.getName());

        this.mapping = newMapping;

        this.guiInterface = newGUIInterface;

        this.serialPort = SerialPort.getCommPort(newMapping.getName());

        if (mapping.getBaudRate() != null) serialPort.setBaudRate(mapping.getBaudRate());
        if (mapping.getNumDataBits() != null) serialPort.setNumDataBits(mapping.getNumDataBits());
        if (mapping.getNumStopBits() != null) serialPort.setNumStopBits(mapping.getNumStopBits());
        if (mapping.getParity() != null) serialPort.setParity(mapping.getParity());
    }

    @Override
    public void start() {
        isRunning = true;

        readThread = new Thread(() -> {
            log.debug("Serial Read Thread started for {}", mapping.getName());

            while (isRunning) {
                if (serialPort.isOpen()) {
                    int bytesAvailable = serialPort.bytesAvailable();
                    if (bytesAvailable == 0) {
                        // No data coming from COM portName
                        ThreadUtil.silentSleep(10);
                        continue;
                    } else if (bytesAvailable == -1) {
                        // Check if portName closed unexpected (e.g. Unplugged)
                        serialPort.closePort();
                        guiInterface.notify("Serial Port", "Serial " + mapping.getName() + "(" + mapping.getType() + ") unplugged", TrayIcon.MessageType.WARNING);
                        log.warn("Serial {} unplugged", mapping.getName());

                        continue;
                    }

                    int bytesToRead = mapping.getReadMultipleBytes() ? bytesAvailable : 1;

                    byte[] receivedData = new byte[bytesToRead];
                    serialPort.readBytes(receivedData, bytesToRead);

                    if (server != null) {
                        if (Objects.equals(mapping.getReadCharset(), "BINARY")) server.onDataReceived(getChannel(), receivedData);
                        else server.onDataReceived(getChannel(), new String(receivedData, Charset.forName(mapping.getReadCharset())));
                    }
                }
            }

            log.debug("Serial Read Thread stopped for {}", mapping.getName());
        });

        writeThread = new Thread(() -> {
            log.debug("Serial Write Thread started for {}", mapping.getName());

            while (isRunning) {
                if (serialPort.isOpen()) {
                    if (writeBuffer.length > 0) {
                        log.trace("Bytes: {}", Hex.encodeHexString(writeBuffer));

                        serialPort.writeBytes(writeBuffer, writeBuffer.length);
                        writeBuffer = new byte[]{};
                    }
                    ThreadUtil.silentSleep(10);
                }
            }

            log.debug("Serial Write Thread stopped for {}", mapping.getName());
        });

        monitorThread = new Thread(() -> {
            log.debug("Serial Monitor Thread started for {}", mapping.getName());

            while (isRunning) {
                if (serialPort.isOpen()) {
                    ThreadUtil.silentSleep(1000);
                } else {
                    log.info("Trying to connect the serial @ {}", serialPort.getSystemPortName());
                    serialPort.openPort();

                    ThreadUtil.silentSleep(1000);
                }
            }

            log.debug("Serial Monitor Thread stopped for {}", mapping.getName());
        });

        readThread.start();
        writeThread.start();
        monitorThread.start();
    }

    @Override
    public void stop() {
        log.info("Stopping SerialWebSocketService");

        isRunning = false;

        readThread.interrupt();
        writeThread.interrupt();
        monitorThread.interrupt();

        serialPort.closePort();

        log.info("Stopped SerialWebSocketService");
    }

    @Override
    public void onDataReceived(String message) {
        onDataReceived(message.getBytes());
    }

    @Override
    public void onDataReceived(byte[] message) {
        writeBuffer = message;
    }

    @Override
    public void onRegister(WebSocketServerInterface newServer) {
        this.server = newServer;
    }

    @Override
    public void onUnregister() {
        this.server = null;
    }

    @Override
    public String getChannel() {
        return "/serial/" + mapping.getType();
    }
}

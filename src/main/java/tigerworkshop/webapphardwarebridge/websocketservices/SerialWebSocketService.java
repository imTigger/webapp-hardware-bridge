package tigerworkshop.webapphardwarebridge.websocketservices;

import com.fazecast.jSerialComm.SerialPort;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.binary.Hex;
import tigerworkshop.webapphardwarebridge.dtos.Config;
import tigerworkshop.webapphardwarebridge.interfaces.GUIListenerInterface;
import tigerworkshop.webapphardwarebridge.interfaces.WebSocketServerInterface;
import tigerworkshop.webapphardwarebridge.interfaces.WebSocketServiceInterface;
import tigerworkshop.webapphardwarebridge.utils.ThreadUtil;

import java.awt.*;
import java.nio.charset.StandardCharsets;

@Log4j2
public class SerialWebSocketService implements WebSocketServiceInterface {
    private final Config.SerialMapping mapping;
    private final SerialPort serialPort;
    private byte[] writeBuffer = {};

    private WebSocketServerInterface server = null;
    private Thread readThread;
    private Thread writeThread;

    @Setter
    private GUIListenerInterface notificationListener;

    public SerialWebSocketService(Config.SerialMapping newMapping) {
        log.info("Starting SerialWebSocketService on {}", newMapping.getName());

        mapping = newMapping;

        serialPort = SerialPort.getCommPort(newMapping.getName());

        if (mapping.getBaudRate() != null) serialPort.setBaudRate(mapping.getBaudRate());
        if (mapping.getNumDataBits() != null) serialPort.setNumDataBits(mapping.getNumDataBits());
        if (mapping.getNumStopBits() != null) serialPort.setNumStopBits(mapping.getNumStopBits());
        if (mapping.getParity() != null) serialPort.setParity(mapping.getParity());
    }

    @Override
    public void start() {
        readThread = new Thread(() -> {
            log.trace("Serial Read Thread started for {}", mapping.getName());

            while (!Thread.interrupted()) {
                try {
                    if (serialPort.isOpen()) {
                        if (serialPort.bytesAvailable() == 0) {
                            // No data coming from COM portName
                            ThreadUtil.silentSleep(10);
                            continue;
                        } else if (serialPort.bytesAvailable() == -1) {
                            // Check if portName closed unexpected (e.g. Unplugged)
                            serialPort.closePort();

                            notificationListener.notify("Serial Port", "Serial " + mapping.getName() + "(" + mapping.getType() + ") unplugged", TrayIcon.MessageType.WARNING);

                            continue;
                        }

                        byte[] receivedData = new byte[1];
                        serialPort.readBytes(receivedData, 1);

                        if (server != null) {
                            server.onDataReceived(getChannel(), new String(receivedData, StandardCharsets.UTF_8));
                        }
                    } else {
                        log.trace("Trying to connect the serial @ {}", serialPort.getSystemPortName());
                        serialPort.openPort();
                    }
                } catch (Exception e) {
                    log.warn("Error: {}", e.getMessage(), e);
                    ThreadUtil.silentSleep(1000);
                }
            }
        });

        writeThread = new Thread(() -> {
            log.trace("Serial Write Thread started for {}", mapping.getName());

            while (!Thread.interrupted()) {
                if (serialPort.isOpen()) {
                    try {
                        if (writeBuffer.length > 0) {
                            log.trace("Bytes: {}", Hex.encodeHexString(writeBuffer));

                            serialPort.writeBytes(writeBuffer, writeBuffer.length);
                            writeBuffer = new byte[]{};
                        }
                        ThreadUtil.silentSleep(10);
                    } catch (Exception e) {
                        log.warn("Error: {}", e.getMessage());
                        ThreadUtil.silentSleep(1000);
                    }
                }
            }
        });

        readThread.start();
        writeThread.start();
    }

    @Override
    public void stop() {
        log.info("Stopping SerialWebSocketService");
        serialPort.closePort();

        readThread.interrupt();
        writeThread.interrupt();
    }

    @Override
    public void onDataReceived(String message) {
        send(message.getBytes());
    }

    @Override
    public void onDataReceived(byte[] message) {
        send(message);
    }

    @Override
    public void setServer(WebSocketServerInterface server) {
        this.server = server;
        server.subscribe(this, getChannel());
    }

    private void send(byte[] message) {
        writeBuffer = message;
    }

    private String getChannel() {
        return "/serial/" + mapping.getType();
    }
}

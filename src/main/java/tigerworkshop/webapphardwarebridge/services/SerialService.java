package tigerworkshop.webapphardwarebridge.services;

import jssc.SerialPort;
import jssc.SerialPortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tigerworkshop.webapphardwarebridge.BridgeWebSocketServer;
import tigerworkshop.webapphardwarebridge.interfaces.WebSocketServiceInterface;
import tigerworkshop.webapphardwarebridge.utils.ThreadUtil;

public class SerialService implements WebSocketServiceInterface {
    private final String portName;
    private final String mappingKey;
    private final SerialPort serialPort;
    private final Thread writeThread;
    private final Thread readThread;
    private byte[] writeBuffer = {};

    private Logger logger = LoggerFactory.getLogger(getClass());
    private BridgeWebSocketServer server = null;

    public SerialService(String portName, String mappingKey) {
        logger.info("Starting SerialService on " + portName);

        this.portName = portName;
        this.mappingKey = mappingKey;
        this.serialPort = new SerialPort(portName);

        this.readThread = new Thread(new Runnable() {
            @Override
            public void run() {
                logger.info("Serial Read Thread started");

                while (true) {
                    try {
                        if (serialPort.isOpened()) {
                            if (serialPort.getInputBufferBytesCount() == 0) {
                                // No data coming from COM portName
                                ThreadUtil.silentSleep(10);
                                continue;
                            } else if (serialPort.getInputBufferBytesCount() == -1) {
                                // Check if portName closed unexpected (e.g. Unplugged)
                                serialPort.closePort();
                                logger.warn("Serial unplugged!");
                                continue;
                            }

                            String receivedData = serialPort.readString(1);

                            if (server != null) {
                                server.onDataReceived(SerialService.this, receivedData);
                            }
                        } else {
                            logger.info("Trying to connect the serial @ " + serialPort.getPortName());
                            serialPort.openPort();
                            serialPort.setParams(SerialPort.BAUDRATE_9600,
                                    SerialPort.DATABITS_8,
                                    SerialPort.STOPBITS_1,
                                    SerialPort.PARITY_NONE);
                        }
                    } catch (Exception e) {
                        logger.warn("Error: " + e.getMessage());
                        ThreadUtil.silentSleep(1000);
                    }
                }
            }
        });

        this.writeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                logger.info("Serial Write Thread started");

                while (true) {
                    if (serialPort.isOpened()) {
                        try {
                            if (writeBuffer.length > 0) {
                                serialPort.writeBytes(writeBuffer);
                                writeBuffer = new byte[]{};
                            }
                            ThreadUtil.silentSleep(10);
                        } catch (SerialPortException e) {
                            logger.warn("Error: " + e.getMessage());
                            ThreadUtil.silentSleep(1000);
                        }
                    }
                }
            }
        });

        this.readThread.start();
        this.writeThread.start();
    }

    public void send(byte[] message) {
        writeBuffer = message;
    }

    @Override
    public String getPrefix() {
        return "/serial/" + mappingKey;
    }

    @Override
    public void onDataReceived(String message) {
        send(message.getBytes());
    }

    @Override
    public void setServer(BridgeWebSocketServer server) {
        this.server = server;
    }
}

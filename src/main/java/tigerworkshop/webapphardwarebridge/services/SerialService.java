package tigerworkshop.webapphardwarebridge.services;

import jssc.SerialPort;
import jssc.SerialPortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tigerworkshop.webapphardwarebridge.interfaces.SerialListener;
import tigerworkshop.webapphardwarebridge.utils.ThreadUtil;

public class SerialService {
    private final String portName;
    private final String mappingKey;
    private final SerialPort serialPort;
    private final Thread writeThread;
    private final Thread readThread;
    private SerialListener listener;
    private byte[] writeBuffer = {};

    private Logger logger = LoggerFactory.getLogger(getClass());

    public SerialService(SerialListener listener, String portName, String mappingKey) {
        logger.info("Starting SerialService on " + portName);

        this.listener = listener;
        this.portName = portName;
        this.mappingKey = mappingKey;
        this.serialPort = new SerialPort(portName);

        listener.onStart(this);

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
                            if (listener != null) {
                                listener.onDataReceived(SerialService.this, receivedData);
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

    public String getPortName() {
        return portName;
    }

    public String getMappingKey() {
        return mappingKey;
    }

    public void send(byte[] message) {
        writeBuffer = message;
    }
}

package tigerworkshop.webapphardwarebridge.services;

import jssc.SerialPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tigerworkshop.webapphardwarebridge.BridgeWebSocketServer;
import tigerworkshop.webapphardwarebridge.utils.ThreadUtil;

public class SerialService {
    private final BridgeWebSocketServer bridgeWebSocketServer;
    private final Thread readThread;

    private Logger logger = LoggerFactory.getLogger(getClass());

    public SerialService(BridgeWebSocketServer bridgeWebSocketServer, String port) {
        logger.info("Starting SerialService on " + port);

        this.bridgeWebSocketServer = bridgeWebSocketServer;
        this.readThread = new Thread(new Runnable() {
            @Override
            public void run() {
                SerialPort serialPort = new SerialPort(port);
                logger.info("Weight Read Thread started");

                while (true) {
                    try {
                        if (serialPort.isOpened()) {
                            if (serialPort.getInputBufferBytesCount() == 0) {
                                // No data coming from COM port
                                ThreadUtil.silentSleep(50);
                                continue;
                            } else if (serialPort.getInputBufferBytesCount() == -1) {
                                // Check if port closed unexpected (e.g. Unplugged)
                                serialPort.closePort();
                                logger.warn("Serial unplugged!");
                                continue;
                            }

                            String receivedData = serialPort.readString(1);
                            if (SerialService.this.bridgeWebSocketServer != null) {
                                SerialService.this.bridgeWebSocketServer.updateSerial(receivedData, port);
                            }
                        }  else {
                            logger.info("Trying to connect the serial @ " + serialPort.getPortName());
                            serialPort.openPort();
                            serialPort.setParams(SerialPort.BAUDRATE_9600,
                                    SerialPort.DATABITS_8,
                                    SerialPort.STOPBITS_1,
                                    SerialPort.PARITY_NONE);
                        }
                    } catch (Exception e) {
                        logger.info("Error: " + e.getMessage());
                        e.printStackTrace();
                        ThreadUtil.silentSleep(1000);
                    }
                }
            }
        });

        this.readThread.start();
    }
}

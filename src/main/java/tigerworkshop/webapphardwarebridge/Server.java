package tigerworkshop.webapphardwarebridge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fazecast.jSerialComm.SerialPort;
import io.javalin.Javalin;
import io.javalin.community.ssl.SslPlugin;
import io.javalin.http.ContentType;
import io.javalin.plugin.bundled.CorsPluginConfig;
import io.javalin.websocket.WsContext;
import lombok.extern.log4j.Log4j2;
import tigerworkshop.webapphardwarebridge.dtos.Config;
import tigerworkshop.webapphardwarebridge.dtos.PrintServiceDTO;
import tigerworkshop.webapphardwarebridge.dtos.SerialPortDTO;
import tigerworkshop.webapphardwarebridge.interfaces.GUIInterface;
import tigerworkshop.webapphardwarebridge.interfaces.WebSocketServerInterface;
import tigerworkshop.webapphardwarebridge.interfaces.WebSocketServiceInterface;
import tigerworkshop.webapphardwarebridge.services.ConfigService;
import tigerworkshop.webapphardwarebridge.utils.CertificateGenerator;
import tigerworkshop.webapphardwarebridge.websocketservices.PrinterWebSocketService;
import tigerworkshop.webapphardwarebridge.websocketservices.SerialWebSocketService;

import javax.print.PrintService;
import java.awt.*;
import java.awt.print.PrinterJob;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

@Log4j2
public class Server implements WebSocketServerInterface {
    private Javalin javalinServer;

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final ConfigService configService = ConfigService.getInstance();

    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<WsContext>> socketChannelSubscriptions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<WebSocketServiceInterface>> serviceChannelSubscriptions = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<WebSocketServiceInterface> services = new ConcurrentLinkedQueue<>();

    private final GUIInterface guiInterface;

    public Server(GUIInterface guiInterface) {
        this.guiInterface = guiInterface;
    }

    public static void main(String[] args) {
        try {
            new Server(null).start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void start() throws Exception {
        Config config = configService.getConfig();

        Config.Server serverConfig = config.getServer();

        // Create Javalin Server
        javalinServer = Javalin.create(cfg -> {
            cfg.showJavalinBanner = false;
            cfg.staticFiles.add(staticFiles -> staticFiles.directory = "web");
            cfg.bundledPlugins.enableCors(cors -> cors.addRule(CorsPluginConfig.CorsRule::anyHost));

            if (serverConfig.getTls().isEnabled()) {
                if (serverConfig.getTls().isSelfSigned()) {
                    log.info("TLS Enabled with self-signed certificate");

                    CertificateGenerator.generateSelfSignedCertificate(serverConfig.getAddress(), serverConfig.getTls().getCert(), serverConfig.getTls().getKey());

                    log.info("For first time setup, open in browser and trust the certificate: {}", serverConfig.getUri().replace("http", "https"));
                }

                SslPlugin plugin = new SslPlugin(conf -> {
                    conf.insecure = false;
                    conf.securePort = serverConfig.getPort();
                    conf.pemFromPath(serverConfig.getTls().getCert(), serverConfig.getTls().getKey());
                });
                cfg.registerPlugin(plugin);
            }
        });

        // Add WebSocket Auth
        javalinServer.wsBefore(ctx -> {
            ctx.onConnect(wsConnectContext -> {
                wsConnectContext.enableAutomaticPings(5, TimeUnit.SECONDS);

                if (serverConfig.getAuthentication().isEnabled()) {
                    if (Optional.ofNullable(wsConnectContext.queryParam("token")).orElse("").equals(serverConfig.getAuthentication().getToken())) {
                        return;
                    }

                    wsConnectContext.closeSession(1003, "Invalid token");
                }
            });
        });

        // Add WebSocket Printer Service
        Config.Printer printerConfig = config.getPrinter();
        if (printerConfig.isEnabled() && !printerConfig.getMappings().isEmpty()) {
            PrinterWebSocketService printerWebSocketService = new PrinterWebSocketService(guiInterface);
            printerWebSocketService.start();

            javalinServer.ws(printerWebSocketService.getChannel(), ws -> {
                ws.onConnect(ctx -> {
                    log.info("{} connected to {}", ctx.host(), printerWebSocketService.getChannel());

                    addSocketToChannel(printerWebSocketService.getChannel(), ctx);
                });

                ws.onClose(ctx -> {
                    log.info("{} disconnected from {}", ctx.host(), printerWebSocketService.getChannel());

                    removeSocketFromChannel(printerWebSocketService.getChannel(), ctx);
                });

                ws.onMessage(ctx -> {
                    log.info("{} sent message to {}: {}", ctx.host(), printerWebSocketService.getChannel(), ctx.message());

                    processMessage("/printer", ctx.message());
                });
            });

            registerService(printerWebSocketService);
        }

        // Add WebSocket Serial Service
        Config.Serial serialConfig = config.getSerial();
        if (serialConfig.isEnabled()) {
            serialConfig.getMappings().forEach(mapping -> {
                try {
                    log.info("Starting SerialWebSocketService: {}", mapping.toString());
                    SerialWebSocketService serialWebSocketService = new SerialWebSocketService(guiInterface, mapping);
                    serialWebSocketService.start();

                    registerService(serialWebSocketService);

                    javalinServer.ws(serialWebSocketService.getChannel(), ws -> {
                        ws.onConnect(ctx -> {
                            log.info("{} connected to {}", ctx.host(), serialWebSocketService.getChannel());

                            addSocketToChannel(serialWebSocketService.getChannel(), ctx);
                        });

                        ws.onClose(ctx -> {
                            log.info("{} disconnected from {}", ctx.host(), serialWebSocketService.getChannel());

                            removeSocketFromChannel(serialWebSocketService.getChannel(), ctx);
                        });

                        ws.onMessage(ctx -> {
                            log.info("{} sent message to {}: {}", ctx.host(), serialWebSocketService.getChannel(), ctx.message());

                            processMessage(serialWebSocketService.getChannel(), ctx.message());
                        });

                        ws.onBinaryMessage(ctx -> {
                            log.info("{} sent binary message to {}: {}", ctx.host(), serialWebSocketService.getChannel(), ctx.data());

                            processMessage(serialWebSocketService.getChannel(), ctx.data());
                        });
                    });
                } catch (Exception e) {
                    String message = "Failed to start SerialWebSocketService for " + mapping.getType() + ": " + e.getMessage();
                    log.error(message);

                    if (guiInterface != null) {
                        guiInterface.notify("Error", message, TrayIcon.MessageType.ERROR);
                    }
                }
            });
        }

        // Add HTTP Auth
        javalinServer.before(ctx -> {
            if (serverConfig.getAuthentication().isEnabled()) {
                try {
                    // Bearer Token
                    if (Optional.ofNullable(ctx.header("Authorization")).orElse("").endsWith(serverConfig.getAuthentication().getToken())) {
                        return;
                    }

                    // Basic Auth
                    if (ctx.basicAuthCredentials() != null && Objects.equals(ctx.basicAuthCredentials().getPassword(), serverConfig.getAuthentication().getToken())) {
                        return;
                    }
                } catch (Exception e) {
                    // NOOP
                }

                ctx.header("WWW-Authenticate", "Basic realm=\"Token required\"");
                ctx.res().sendError(401, "Token mismatch");
            }
        });

        // Add HTTP Service
        javalinServer.get("/config.json", ctx -> {
            ctx.contentType(ContentType.APPLICATION_JSON).result(configService.getConfig().toJson());
        });

        javalinServer.put("/config.json", ctx -> {
            configService.loadFromJson(ctx.body());
            configService.save();

            if (guiInterface != null) {
                guiInterface.notify("Setting", "Setting saved successfully", TrayIcon.MessageType.INFO);
            }

            ctx.contentType(ContentType.APPLICATION_JSON).result(configService.getConfig().toJson());
        });

        javalinServer.get("/system/printers.json", ctx -> {
            ArrayList<PrintServiceDTO> dtos = new ArrayList<>();
            for (PrintService service : PrinterJob.lookupPrintServices()) {
                dtos.add(new PrintServiceDTO(service.getName(), ""));
            }

            ctx.contentType(ContentType.APPLICATION_JSON).result(objectMapper.writeValueAsString(dtos));
        });

        javalinServer.get("/system/serials.json", ctx -> {
            ArrayList<SerialPortDTO> dtos = new ArrayList<>();
            for (SerialPort port : SerialPort.getCommPorts()) {
                dtos.add(new SerialPortDTO(port.getSystemPortName(), port.getPortDescription(), port.getManufacturer()));
            }

            ctx.contentType(ContentType.APPLICATION_JSON).result(objectMapper.writeValueAsString(dtos));
        });

        javalinServer.post("/system/restart.json", ctx -> {
            stop();
            start();

            if (guiInterface != null) {
                guiInterface.notify("Restart", "Server restarted successfully", TrayIcon.MessageType.INFO);
            }
        });

        javalinServer.start(serverConfig.getBind(), serverConfig.getPort());
    }

    public void stop() throws Exception {
        for (Iterator<WebSocketServiceInterface> it = services.iterator(); it.hasNext(); ) {
            WebSocketServiceInterface service  = it.next();
            service.stop();
            it.remove();
        }

        javalinServer.stop();
    }

    /*
     * Service to Server listener
     */
    @Override
    public void onDataReceived(String channel, String message) {
        log.trace("Received data from channel: {}, Data: {}", channel, message);

        ConcurrentLinkedQueue<WsContext> connectionList = socketChannelSubscriptions.getOrDefault(channel, new ConcurrentLinkedQueue<>());

        for (Iterator<WsContext> it = connectionList.iterator(); it.hasNext(); ) {
            try {
                WsContext conn = it.next();
                conn.send(message);
            } catch (Exception e) {
                log.warn("Exception {}: {}, removing connection from list", e.getClass().getSimpleName(), e.getMessage());
                it.remove();
            }
        }
    }

    @Override
    public void onDataReceived(String channel, byte[] message) {
        log.trace("Received data from channel: {}, Data: {}", channel, message);

        ConcurrentLinkedQueue<WsContext> connectionList = socketChannelSubscriptions.getOrDefault(channel, new ConcurrentLinkedQueue<>());

        for (Iterator<WsContext> it = connectionList.iterator(); it.hasNext(); ) {
            WsContext conn = it.next();
            try {
                conn.send(ByteBuffer.wrap(message));
            } catch (Exception e) {
                log.warn("Exception: Removing connection from list");
                it.remove();
            }
        }
    }

    @Override
    public void registerService(WebSocketServiceInterface service) {
        service.onRegister(this);
        addServiceToChannel(service.getChannel(), service);
    }

    @Override
    public void unregisterService(WebSocketServiceInterface service) {
        service.onUnregister();
        removeServiceFromChannel(service.getChannel(), service);
    }

    /*
     * Message handler
     */
    private void processMessage(String channel, String message) {
        ConcurrentLinkedQueue<WebSocketServiceInterface> services = getServicesForChannel(channel);
        for (WebSocketServiceInterface service : services) {
            log.trace("Attempt to send: {} to channel: {}", message, channel);

            service.onDataReceived(message);
        }
    }

    private void processMessage(String channel, byte[] bytes) {
        ConcurrentLinkedQueue<WebSocketServiceInterface> services = getServicesForChannel(channel);
        for (WebSocketServiceInterface service : services) {
            log.trace("Attempt to send: {} to channel: {}", bytes, channel);

            service.onDataReceived(bytes);
        }
    }

    /*
     * Socket to Channel operations
     */
    private ConcurrentLinkedQueue<WsContext> getSocketsForChannel(String channel) {
        return socketChannelSubscriptions.getOrDefault(channel, new ConcurrentLinkedQueue<>());
    }

    void addSocketToChannel(String channel, WsContext socket) {
        ConcurrentLinkedQueue<WsContext> connectionList = getSocketsForChannel(channel);
        connectionList.add(socket);
        socketChannelSubscriptions.put(channel, connectionList);
    }

    private void removeSocketFromChannel(String channel, WsContext socket) {
        ConcurrentLinkedQueue<WsContext> connectionList = getSocketsForChannel(channel);
        connectionList.remove(socket);
        socketChannelSubscriptions.put(channel, connectionList);
    }

    /*
     * Service to Channel operations
     */
    private ConcurrentLinkedQueue<WebSocketServiceInterface> getServicesForChannel(String channel) {
        ConcurrentLinkedQueue<WebSocketServiceInterface> services = new ConcurrentLinkedQueue<>();

        services.addAll(serviceChannelSubscriptions.getOrDefault(channel, new ConcurrentLinkedQueue<>()));
        services.addAll(serviceChannelSubscriptions.getOrDefault("*", new ConcurrentLinkedQueue<>()));

        return services;
    }

    private void addServiceToChannel(String channel, WebSocketServiceInterface service) {
        ConcurrentLinkedQueue<WebSocketServiceInterface> serviceList = serviceChannelSubscriptions.getOrDefault(channel, new ConcurrentLinkedQueue<>());

        serviceList.add(service);
        serviceChannelSubscriptions.put(channel, serviceList);

        if (!services.contains(service)) {
            services.add(service);
        }
    }

    private void removeServiceFromChannel(String channel, WebSocketServiceInterface service) {
        ConcurrentLinkedQueue<WebSocketServiceInterface> serviceList = getServicesForChannel(channel);
        serviceList.remove(service);
        serviceChannelSubscriptions.put(channel, serviceList);

        services.remove(service);
    }
}

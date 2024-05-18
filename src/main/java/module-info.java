module tigerworkshop.webapphardwarebridge {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.bootstrapfx.core;
    requires java.desktop;
    requires com.fazecast.jSerialComm;
    requires jdk.management;
    requires org.bouncycastle.provider;
    requires org.bouncycastle.pkix;
    requires org.apache.commons.io;
    requires org.slf4j;
    requires com.google.gson;
    requires org.java_websocket;
    requires org.apache.pdfbox;
    requires org.apache.commons.codec;
    requires org.apache.httpcomponents.core5.httpcore5;

    opens tigerworkshop.webapphardwarebridge to javafx.fxml;
    opens tigerworkshop.webapphardwarebridge.controller to javafx.fxml;
    opens tigerworkshop.webapphardwarebridge.responses to com.google.gson;
    opens tigerworkshop.webapphardwarebridge.utils to javafx.base, com.google.gson;

    exports tigerworkshop.webapphardwarebridge;
}
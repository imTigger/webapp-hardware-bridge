module tigerworkshop.webapphardwarebridge {
    requires java.desktop;
    requires com.fazecast.jSerialComm;
    requires jdk.management;
    requires org.bouncycastle.provider;
    requires org.bouncycastle.pkix;
    requires org.apache.commons.io;
    requires org.slf4j;
    requires org.java_websocket;
    requires org.apache.pdfbox;
    requires org.apache.commons.codec;
    requires org.apache.httpcomponents.core5.httpcore5;
    requires org.apache.logging.log4j;
    requires io.javalin;
    requires com.fasterxml.jackson.databind;
    requires io.javalin.community.ssl;
    requires static lombok;

    opens tigerworkshop.webapphardwarebridge.dtos to com.fasterxml.jackson.databind;
    opens tigerworkshop.webapphardwarebridge.responses to com.fasterxml.jackson.databind;
    opens tigerworkshop.webapphardwarebridge.utils to com.fasterxml.jackson.databind;

    exports tigerworkshop.webapphardwarebridge;
    exports tigerworkshop.webapphardwarebridge.interfaces;
}
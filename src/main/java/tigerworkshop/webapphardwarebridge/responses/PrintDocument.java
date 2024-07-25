package tigerworkshop.webapphardwarebridge.responses;

import tigerworkshop.webapphardwarebridge.utils.AnnotatedPrintable;

import java.util.ArrayList;
import java.util.UUID;

public class PrintDocument {
    private final UUID uuid;
    String type;
    String url;
    String id;
    Integer qty = 1;
    String file_content;
    String raw_content;
    ArrayList<AnnotatedPrintable.AnnotatedPrintableAnnotation> extras = new ArrayList<>();

    public PrintDocument() {
        uuid = UUID.randomUUID();
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getType() {
        return type;
    }

    public String getUrl() {
        return url;
    }

    public String getId() {
        return id;
    }

    public Integer getQty() {
        return qty;
    }

    public String getFileContent() {
        return file_content;
    }

    public String getRawContent() {
        return raw_content;
    }

    public ArrayList<AnnotatedPrintable.AnnotatedPrintableAnnotation> getExtras() {
        return extras;
    }

    @Override
    public String toString() {
        return "PrintDocument{" +
                "uuid='" + uuid + '\'' +
                ", type='" + type + '\'' +
                ", url='" + url + '\'' +
                ", id='" + id + '\'' +
                ", qty=" + qty +
                ", file_content='" + file_content + '\'' +
                ", raw_content='" + raw_content + '\'' +
                ", extras=" + extras +
                '}';
    }
}
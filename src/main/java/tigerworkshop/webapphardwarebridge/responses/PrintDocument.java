package tigerworkshop.webapphardwarebridge.responses;

import tigerworkshop.webapphardwarebridge.utils.AnnotatedPrintable;

import java.util.ArrayList;

public class PrintDocument {
    String type;
    String url;
    String file_content;
    ArrayList<AnnotatedPrintable.AnnotatedPrintableAnnotation> extras = new ArrayList<>();

    public String getType() {
        return type;
    }

    public String getUrl() {
        return url;
    }

    public String getFileContent() {
        return file_content;
    }

    public ArrayList<AnnotatedPrintable.AnnotatedPrintableAnnotation> getExtras() {
        return extras;
    }

    @Override
    public String toString() {
        return "PrintDocument{" +
                "type='" + type + '\'' +
                ", url='" + url + '\'' +
                ", file_content='" + file_content + '\'' +
                ", extras=" + extras +
                '}';
    }
}
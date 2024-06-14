package tigerworkshop.webapphardwarebridge.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;
import tigerworkshop.webapphardwarebridge.utils.AnnotatedPrintable;

import java.util.ArrayList;

@ToString
@Getter
public class PrintDocument {
    String type;
    String url;
    String id;
    Integer qty = 1;
    @JsonProperty("file_content") String fileContent;
    @JsonProperty("raw_content") String rawContent;
    ArrayList<AnnotatedPrintable.AnnotatedPrintableAnnotation> extras = new ArrayList<>();
}
package tigerworkshop.webapphardwarebridge.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;
import tigerworkshop.webapphardwarebridge.utils.AnnotatedPrintable;

import java.util.ArrayList;
import java.util.UUID;

@ToString
@Getter
public class PrintDocument {
    String type;
    String url;
    String id;
    UUID uuid = UUID.randomUUID();
    Integer qty = 1;
    @JsonProperty("file_content") String fileContent;
    @JsonProperty("raw_content") String rawContent;
    ArrayList<AnnotatedPrintable.AnnotatedPrintableAnnotation> extras = new ArrayList<>();
}
package tigerworkshop.webapphardwarebridge.responses;

import lombok.AllArgsConstructor;
import lombok.ToString;

@ToString
@AllArgsConstructor
public class PrintResult {
    int status;
    String id;
    String url;
    String message;
}

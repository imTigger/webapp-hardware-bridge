package tigerworkshop.webapphardwarebridge.responses;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PrintResult {
    public int status;
    public String id;
    public String url;
    public String message;
}

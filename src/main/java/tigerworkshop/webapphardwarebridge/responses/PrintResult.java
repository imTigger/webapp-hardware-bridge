package tigerworkshop.webapphardwarebridge.responses;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PrintResult {
    public Boolean success;
    public String message;
    public String id;
    public String printerName;
}

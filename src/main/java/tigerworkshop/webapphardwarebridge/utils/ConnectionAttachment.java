package tigerworkshop.webapphardwarebridge.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.hc.core5.http.NameValuePair;

import java.util.List;

@Data
@AllArgsConstructor
public class ConnectionAttachment {
    private String channel;
    private List<NameValuePair> params;
}

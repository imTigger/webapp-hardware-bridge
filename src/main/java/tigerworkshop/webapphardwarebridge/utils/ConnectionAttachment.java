package tigerworkshop.webapphardwarebridge.utils;

import org.apache.http.NameValuePair;

import java.util.List;

public class ConnectionAttachment {
    private String channel;
    private List<NameValuePair> params;
    private String token;

    public ConnectionAttachment(String channel, List<NameValuePair> params, String token) {
        this.channel = channel;
        this.params = params;
        this.token = token;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public List<NameValuePair> getParams() {
        return params;
    }

    public void setParams(List<NameValuePair> params) {
        this.params = params;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}

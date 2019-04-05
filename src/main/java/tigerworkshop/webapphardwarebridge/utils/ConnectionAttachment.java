package tigerworkshop.webapphardwarebridge.utils;

public class ConnectionAttachment {
    private String uri;
    private String token;

    public ConnectionAttachment(String uri, String token) {
        this.uri = uri;
        this.token = token;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}

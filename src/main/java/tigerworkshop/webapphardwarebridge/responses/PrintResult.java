package tigerworkshop.webapphardwarebridge.responses;

public class PrintResult {
    private final int status;
    private final String id;
    private final String url;
    private final String message;

    public PrintResult(int status, String file, String url, String message) {
        this.status = status;
        this.id = file;
        this.url = url;
        this.message = message;
    }
}

package tigerworkshop.webapphardwarebridge.responses;

public class PrintResult {
    private int status;
    private String id;
    private String message;

    public PrintResult(int status, String file, String message) {
        this.status = status;
        this.id = file;
        this.message = message;
    }
}

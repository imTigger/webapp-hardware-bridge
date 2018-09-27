package tigerworkshop.webapphardwarebridge.utils;

public class ThreadUtil {
    public static void silentSleep(long duration) {
        try {
            Thread.sleep(duration);
        } catch (Exception e) {
        }
    }
}

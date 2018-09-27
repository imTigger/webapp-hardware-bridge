package tigerworkshop.webapphardwarebridge.utils;

import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;

public class ImagePrintable implements Printable {

    private final Image image;

    public ImagePrintable(Image image) {
        this.image = image;
    }

    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) {
        if (pageIndex >= 1) {
            return Printable.NO_SUCH_PAGE;
        }

        Graphics2D g2d = (Graphics2D) graphics;
        g2d.translate((int) pageFormat.getImageableX(), (int) pageFormat.getImageableY());

        Double width = pageFormat.getImageableWidth();
        Double height = pageFormat.getImageableHeight();

        g2d.drawImage(image, 0, 0, width.intValue(), height.intValue(), null, null);

        return Printable.PAGE_EXISTS;
    }
}

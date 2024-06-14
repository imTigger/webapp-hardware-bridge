package tigerworkshop.webapphardwarebridge.utils;

import lombok.Data;
import lombok.extern.log4j.Log4j2;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.util.ArrayList;

@Log4j2
public class AnnotatedPrintable implements Printable {
    private final Printable printable;
    private final ArrayList<AnnotatedPrintableAnnotation> annotatedPrintableAnnotationArrayList = new ArrayList<>();

    private static final Double MM_TO_PPI = 2.8346457;

    public AnnotatedPrintable(Printable printable) {
        this.printable = printable;
    }

    public void addAnnotation(AnnotatedPrintableAnnotation annotatedPrintableAnnotation) {
        annotatedPrintableAnnotationArrayList.add(annotatedPrintableAnnotation);
    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
        int result = printable.print(graphics, pageFormat, pageIndex);

        if (annotatedPrintableAnnotationArrayList.isEmpty()) {
            return result;
        }

        if (result == PAGE_EXISTS) {
            Graphics2D graphics2D = (Graphics2D) graphics;

            // On Windows we need getDefaultTransform() to print in correct scale
            // But on Mac it causes NullPointerException, however a blank AffineTransform works
            try {
                graphics2D.setTransform(graphics2D.getDeviceConfiguration().getDefaultTransform());
            } catch (Exception e) {
                graphics2D.setTransform(new AffineTransform());
            }

            float clipX = (float) graphics2D.getClipBounds().getX();
            float clipY = (float) graphics2D.getClipBounds().getY();

            // Catch Exceptions otherwise blank page occur while exceptions silently handled
            try {
                for (AnnotatedPrintableAnnotation annotatedPrintableAnnotation : annotatedPrintableAnnotationArrayList) {
                    if (annotatedPrintableAnnotation.getText() == null) {
                        log.warn("annotatedPrintableAnnotation.getText() is null");
                        continue;
                    }

                    float realX = (float) (clipX + annotatedPrintableAnnotation.getX() * MM_TO_PPI);
                    float realY = (float) (clipY + annotatedPrintableAnnotation.getY() * MM_TO_PPI);

                    int isBold = annotatedPrintableAnnotation.getBold() != null ? Font.BOLD : Font.PLAIN;
                    int fontSize = annotatedPrintableAnnotation.getSize() != null ? annotatedPrintableAnnotation.getSize() : 10;

                    Font font = new Font("Sans-Serif", isBold, fontSize);
                    graphics2D.setColor(Color.BLACK);
                    graphics2D.setFont(font);
                    graphics2D.drawString(annotatedPrintableAnnotation.getText(), realX, realY);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }

        }

        return result;
    }

    @Data
    public static class AnnotatedPrintableAnnotation {
        private String text;
        private Float x;
        private Float y;
        private Integer size;
        private Boolean bold;
    }
}

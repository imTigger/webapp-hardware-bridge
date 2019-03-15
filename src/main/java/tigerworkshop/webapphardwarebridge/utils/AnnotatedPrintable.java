package tigerworkshop.webapphardwarebridge.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.util.ArrayList;

public class AnnotatedPrintable implements Printable {

    private static final Double MM_TO_PPI = 2.8346457;
    private final Printable printable;
    private final ArrayList<AnnotatedPrintableAnnotation> annotatedPrintableAnnotationArrayList = new ArrayList<>();
    private Logger logger = LoggerFactory.getLogger(AnnotatedPrintable.class.getName());

    public AnnotatedPrintable(Printable printable) {
        this.printable = printable;
    }

    public void addAnnotation(AnnotatedPrintableAnnotation annotatedPrintableAnnotation) {
        annotatedPrintableAnnotationArrayList.add(annotatedPrintableAnnotation);
    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
        int result = printable.print(graphics, pageFormat, pageIndex);

        if (annotatedPrintableAnnotationArrayList.size() == 0) {
            return result;
        }

        if (result == PAGE_EXISTS) {
            Graphics2D graphics2D = (Graphics2D) graphics;

            // On Windows we need getDefaultTransform() to print in correct scale
            // But on Mac it cause NullPointerException, however a blank AffineTransform works
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
                        logger.warn("annotatedPrintableAnnotation.getText() is null");
                        continue;
                    }

                    float realX = (float) (clipX + annotatedPrintableAnnotation.getX() * MM_TO_PPI);
                    float realY = (float) (clipY + annotatedPrintableAnnotation.getY() * MM_TO_PPI);

                    Integer isBold = annotatedPrintableAnnotation.getBold() != null ? Font.BOLD : Font.PLAIN;
                    Integer fontSize = annotatedPrintableAnnotation.getSize() != null ? annotatedPrintableAnnotation.getSize() : 10;

                    Font font = new Font("Sans-Serif", isBold, fontSize);
                    graphics2D.setColor(Color.BLACK);
                    graphics2D.setFont(font);
                    graphics2D.drawString(annotatedPrintableAnnotation.getText(), realX, realY);
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

        }

        return result;
    }

    public class AnnotatedPrintableAnnotation {

        private String field;
        private String text;
        private Float x;
        private Float y;
        private Integer size;
        private Boolean bold;

        public String getField() {
            return field;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public Float getX() {
            return x;
        }

        public Float getY() {
            return y;
        }

        public Integer getSize() {
            return size;
        }

        public Boolean getBold() {
            return bold;
        }

        @Override
        public String toString() {
            return "AnnotatedPrintableAnnotation{" +
                    "field='" + field + '\'' +
                    ", text='" + text + '\'' +
                    ", x=" + x +
                    ", y=" + y +
                    ", size=" + size +
                    ", bold='" + bold + '\'' +
                    '}';
        }
    }
}

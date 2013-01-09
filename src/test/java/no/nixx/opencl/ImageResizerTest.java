package no.nixx.opencl;

import no.nixx.opencl.util.BufferedImageUtils;
import org.imgscalr.Scalr;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.image.BufferedImage;

import static junit.framework.Assert.assertEquals;

/**
 * Oddbjørn Kvalsund
 */
public class ImageResizerTest {

    private static ImageResizer imageResizer;

    @BeforeClass
    public static void setup() {
        imageResizer = new ImageResizer();
    }

    @AfterClass
    public static void teardown() {
        imageResizer.dispose();
    }

    @Test
    public void testResizeSpecifyingBothEdges() {
        final int width = 512;
        final int height = 1024;
        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        final BufferedImage resizedImage = imageResizer.resize(image, image.getWidth() * 4, image.getHeight() * 4);

        assertEquals(width * 4, resizedImage.getWidth());
        assertEquals(height * 4, resizedImage.getHeight());
    }

    @Test
    public void testResizeSpecifyingOnlyLongEdge() {
        final int width = 512;
        final int height = 1024;
        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        final BufferedImage resizedImage = imageResizer.resize(image, 2048);

        assertEquals(1024, resizedImage.getWidth());
        assertEquals(2048, resizedImage.getHeight());
    }

    /* To test visual performance of the resizer */
    /*
    public static void main(String[] args) {
        final BufferedImage image = BufferedImageUtils.loadBufferedImageFromClasspath("Car_128x128.jpg", BufferedImage.TYPE_INT_ARGB);

        setup();
        final BufferedImage openclResize = imageResizer.resize(image, image.getWidth() * 2);
        BufferedImageUtils.displayImage("OpenCL", openclResize);
        teardown();

        final BufferedImage scalrResize = Scalr.resize(image, image.getWidth() * 2);
        BufferedImageUtils.displayImage("Scalr", scalrResize);
    }
    */
}
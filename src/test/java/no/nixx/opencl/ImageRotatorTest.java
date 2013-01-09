package no.nixx.opencl;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.image.BufferedImage;

import static junit.framework.Assert.assertEquals;

/**
 * Oddbjørn Kvalsund
 */
public class ImageRotatorTest {

    private static ImageRotator imageRotator;

    @BeforeClass
    public static void setup() {
        imageRotator = new ImageRotator();
    }

    @AfterClass
    public static void teardown() {
        imageRotator.dispose();
    }

    @Test
    public void testRotation() {
        final int shortEdgeLength = 512;
        final int longEdgeLength = 1024;
        final BufferedImage image = new BufferedImage(longEdgeLength, shortEdgeLength, BufferedImage.TYPE_INT_ARGB);

        final BufferedImage rotatedCW90 = imageRotator.rotate(image, ImageRotator.Rotation.CW_90);
        assertEquals(rotatedCW90.getWidth(), shortEdgeLength);
        assertEquals(rotatedCW90.getHeight(), longEdgeLength);

        final BufferedImage rotatedCCW90 = imageRotator.rotate(image, ImageRotator.Rotation.CCW_90);
        assertEquals(rotatedCCW90.getWidth(), shortEdgeLength);
        assertEquals(rotatedCCW90.getHeight(), longEdgeLength);

        final BufferedImage rotatedFlipped = imageRotator.rotate(image, ImageRotator.Rotation.FLIP);
        assertEquals(rotatedFlipped.getWidth(), longEdgeLength);
        assertEquals(rotatedFlipped.getHeight(), shortEdgeLength);
    }
}
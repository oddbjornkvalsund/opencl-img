package no.nixx.opencl.util;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;

/**
 * @author Oddbjørn Kvalsund
 */
public class BufferedImageUtils {
    public static BufferedImage loadBufferedImageAsType(String fileName, int type) {
        final BufferedImage image;
        try {
            image = ImageIO.read(new File(fileName));
        } catch (IOException e) {
            throw new RuntimeException("Could not load image: " + fileName, e);
        }

        int sizeX = image.getWidth();
        int sizeY = image.getHeight();

        if (image.getType() == type) {
            return image;
        } else {
            // 'type' should be one of BufferedImage.TYPE_*
            BufferedImage result = new BufferedImage(sizeX, sizeY, type);
            Graphics g = result.createGraphics();
            g.drawImage(image, 0, 0, null);
            g.dispose();

            return result;
        }
    }

    public static void displayImage(final BufferedImage image) {
        new JFrame() {
            {
                final JLabel label = new JLabel("", new ImageIcon(image), 0);
                add(label);
                pack();
                setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                setVisible(true);
            }
        };
    }

    public static int[] getDataBufferInt(BufferedImage outputImage) {
        return ((DataBufferInt) outputImage.getRaster().getDataBuffer()).getData();
    }
}
package no.nixx.opencl.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Oddbjørn Kvalsund
 */
public class ClasspathUtils {
    public static String getClasspathResourceAsString(String resourceName) {
        final InputStream inputStream = ClasspathUtils.class.getClassLoader().getResourceAsStream(resourceName);
        final BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        final StringBuilder sb = new StringBuilder();

        String line;
        try {
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not load resource from classpath:" + resourceName, e);
        }

        return sb.toString();
    }
}

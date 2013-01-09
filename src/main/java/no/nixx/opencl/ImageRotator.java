package no.nixx.opencl;

import no.nixx.opencl.util.ClasspathUtils;
import org.jocl.*;

import java.awt.image.BufferedImage;

import static no.nixx.opencl.util.BufferedImageUtils.getDataBufferInt;
import static no.nixx.opencl.util.OCLUtils.*;
import static org.jocl.CL.*;

/**
 * @author Oddbjørn Kvalsund
 */
public class ImageRotator {

    @SuppressWarnings("unused")
    public enum Rotation {
        CW_90(90),
        CCW_90(270),
        FLIP(180);

        public final int angle;

        private Rotation(int angle) {
            this.angle = angle;
        }
    }

    private final cl_platform_id platformId;
    private final cl_device_id deviceId;
    private final cl_context context;
    private final cl_command_queue commandQueue;
    private final cl_program program;

    public ImageRotator() {
        platformId = getFirstPlatformId();
        deviceId = getFirstDeviceIdForPlatformId(platformId);
        context = getContextForPlatformIdAndDeviceId(platformId, deviceId);
        commandQueue = getCommandQueueForContextAndPlatformIdAndDeviceId(context, platformId, deviceId);
        program = createProgramFromSource(context, ClasspathUtils.getClasspathResourceAsString("rotate.cl"));
    }

    @SuppressWarnings("unused")
    public ImageRotator(cl_platform_id platformId, cl_device_id deviceId, cl_context context, cl_command_queue commandQueue, cl_program program) {
        this.platformId = platformId;
        this.deviceId = deviceId;
        this.context = context;
        this.commandQueue = commandQueue;
        this.program = program;
    }

    public BufferedImage rotate(BufferedImage inputImage, Rotation rotation) {
        final int angle = rotation.angle;
        final int outputImageWidth = (outputImageHasSameOrientation(angle)) ? inputImage.getWidth() : inputImage.getHeight();
        final int outputImageHeight = (outputImageHasSameOrientation(angle)) ? inputImage.getHeight() : inputImage.getWidth();
        final BufferedImage outputImage = new BufferedImage(outputImageWidth, outputImageHeight, BufferedImage.TYPE_INT_RGB);

        final cl_mem inputRaster = createReadOnlyImage(context, inputImage);
        final cl_mem outputRaster = createWritableImage(context, outputImageWidth, outputImageHeight);

        // Ref. http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/clSetKernelArg.html:
        // Rather than attempt to share cl_kernel objects among multiple host threads, applications are strongly
        // encouraged to make additional cl_kernel objects for kernel functions for each host thread.
        final cl_kernel kernel = clCreateKernel(program, "rotateImage", null);

        // Set up the work size and arguments, and execute the kernel
        long globalWorkSize[] = new long[2];
        globalWorkSize[0] = inputImage.getWidth();
        globalWorkSize[1] = inputImage.getHeight();
        clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(inputRaster));
        clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(outputRaster));
        clSetKernelArg(kernel, 2, Sizeof.cl_int, Pointer.to(new int[]{angle}));

        clEnqueueNDRangeKernel(commandQueue, kernel, 2, null, globalWorkSize, null, 0, null, null);

        // Read the pixel data into the output image
        final int outputData[] = getDataBufferInt(outputImage);
        clEnqueueReadImage(
                commandQueue, outputRaster, true, new long[3],
                new long[]{outputImageWidth, outputImageHeight, 1},
                outputImageWidth * Sizeof.cl_uint, 0,
                Pointer.to(outputData), 0, null, null);

        clReleaseMemObject(outputRaster);
        clReleaseMemObject(inputRaster);
        clReleaseKernel(kernel);

        return outputImage;
    }

    public void dispose() {
        clReleaseProgram(program);
        clReleaseCommandQueue(commandQueue);
        clReleaseContext(context);
    }

    private boolean outputImageHasSameOrientation(int angle) {
        return angle == 0 || angle == 180;
    }
}
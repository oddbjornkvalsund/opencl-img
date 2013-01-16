package no.nixx.opencl;

import no.nixx.opencl.util.ClasspathUtils;
import org.jocl.*;

import java.awt.image.BufferedImage;

import static no.nixx.opencl.util.BufferedImageUtils.getDataBufferInt;
import static no.nixx.opencl.util.OCLUtils.*;
import static no.nixx.opencl.util.OCLUtils.createProgramFromSource;
import static org.jocl.CL.*;
import static org.jocl.CL.clReleaseKernel;
import static org.jocl.CL.clReleaseMemObject;

/**
 * Oddbjørn Kvalsund
 */
public class ImageResizer {

    private final cl_platform_id platformId;
    private final cl_device_id deviceId;
    private final cl_context context;
    private final cl_command_queue commandQueue;
    private final cl_program program;

    public ImageResizer() {
        platformId = getFirstPlatformId();
        deviceId = getFirstDeviceIdForPlatformId(platformId);
        context = getContextForPlatformIdAndDeviceId(platformId, deviceId);
        commandQueue = getCommandQueueForContextAndPlatformIdAndDeviceId(context, deviceId);
        program = createProgramFromSource(context, ClasspathUtils.getClasspathResourceAsString("resize.cl"));
    }

    @SuppressWarnings("unused")
    public ImageResizer(cl_platform_id platformId, cl_device_id deviceId, cl_context context, cl_command_queue commandQueue, cl_program program) {
        this.platformId = platformId;
        this.deviceId = deviceId;
        this.context = context;
        this.commandQueue = commandQueue;
        this.program = program;
    }

    public BufferedImage resize(BufferedImage inputImage, int newLongEdgeLength) {
        final int inputImageWidth = inputImage.getWidth();
        final int inputImageHeight = inputImage.getHeight();

        final int outputImageWidth;
        final int outputImageHeight;
        if(inputImageWidth > inputImageHeight) {
            outputImageWidth = newLongEdgeLength;
            outputImageHeight = inputImageHeight * (newLongEdgeLength / inputImageWidth);
        } else {
            outputImageWidth = inputImageWidth * (newLongEdgeLength / inputImageHeight);
            outputImageHeight = newLongEdgeLength;
        }

        return resize(inputImage, outputImageWidth, outputImageHeight);
    }

    public BufferedImage resize(BufferedImage inputImage, int outputImageWidth, int outputImageHeight) {
        final BufferedImage outputImage = new BufferedImage(outputImageWidth, outputImageHeight, BufferedImage.TYPE_INT_RGB);

        final cl_mem inputRaster = createReadOnlyImage(context, inputImage);
        final cl_mem outputRaster = createWritableImage(context, outputImageWidth, outputImageHeight);

        // Ref. http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/clSetKernelArg.html:
        // Rather than attempt to share cl_kernel objects among multiple host threads, applications are strongly
        // encouraged to make additional cl_kernel objects for kernel functions for each host thread.
        final cl_kernel kernel = clCreateKernel(program, "resizeImage", null);
        final long globalWorkSize[] = new long[2];
        globalWorkSize[0] = outputImageWidth;
        globalWorkSize[1] = outputImageHeight;
        clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(inputRaster));
        clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(outputRaster));

        clEnqueueNDRangeKernel(commandQueue, kernel, 2, null, globalWorkSize, null, 0, null, null);

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
}
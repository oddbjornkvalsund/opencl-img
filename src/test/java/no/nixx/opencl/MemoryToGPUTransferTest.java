package no.nixx.opencl;

import no.nixx.opencl.util.ClasspathUtils;
import org.jocl.*;
import org.junit.Test;

import java.awt.image.BufferedImage;

import static no.nixx.opencl.util.BufferedImageUtils.getDataBufferInt;
import static no.nixx.opencl.util.OCLUtils.*;
import static org.jocl.CL.*;

/**
 * Oddbjørn Kvalsund
 */
public class MemoryToGPUTransferTest {

    private cl_context context;
    private cl_command_queue commandQueue;
    private cl_program program;
    private cl_platform_id platformId;
    private cl_device_id deviceId;
    private cl_event profilingEvent;

    @Test
    public void testTransferSpeed() {
        platformId = getFirstPlatformId();
        deviceId = getFirstDeviceIdForPlatformId(platformId);
        context = getContextForPlatformIdAndDeviceId(platformId, deviceId);
        commandQueue = getCommandQueueForContextAndPlatformIdAndDeviceId(context, deviceId);
        program = createProgramFromSource(context, ClasspathUtils.getClasspathResourceAsString("dummy.cl"));

        final int imageSize = 4096;
        final BufferedImage inputImage = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_ARGB);
        final BufferedImage outputImage = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_ARGB);
        final int outputData[] = getDataBufferInt(outputImage);
        final int outputImageWidth = inputImage.getWidth();
        final int outputImageHeight = inputImage.getHeight();

        final cl_mem inputRaster = createReadOnlyImage(context, inputImage);
        final cl_mem outputRaster = createWritableImage(context, outputImageWidth, outputImageHeight);

        final cl_kernel kernel = clCreateKernel(program, "dummyFunction", null);
        clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(inputRaster));
        clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(outputRaster));
        final long globalWorkSize[] = new long[]{outputImageWidth, outputImageHeight};

        startTiming();
        clEnqueueNDRangeKernel(commandQueue, kernel, 2, null, globalWorkSize, null, 0, null, profilingEvent);
        stopTiming("clEnqueueNDRangeKernel");

        // SLOOOOOW! Read http://www.nvidia.com/content/cudazone/CUDABrowser/downloads/papers/NVIDIA_OpenCL_BestPracticesGuide.pdf - section 3.1

        startTiming();
        clEnqueueReadImage(
                commandQueue, outputRaster, true, new long[3],
                new long[]{outputImageWidth, outputImageHeight, 1},
                outputImageWidth * Sizeof.cl_uint, 0,
                Pointer.to(outputData), 0, null, profilingEvent);
        stopTiming("clEnqueueReadImage");

        clReleaseProgram(program);
        clReleaseCommandQueue(commandQueue);
        clReleaseContext(context);
    }

    private void startTiming() {
        profilingEvent = clCreateUserEvent(context, null);
    }

    private void stopTiming(String functionName) {
        long start[] = new long[1];
        long end[] = new long[1];

        clFinish(commandQueue);
        clGetEventProfilingInfo(profilingEvent, CL_PROFILING_COMMAND_START, Sizeof.cl_ulong, Pointer.to(start), null);
        clGetEventProfilingInfo(profilingEvent, CL_PROFILING_COMMAND_END, Sizeof.cl_ulong, Pointer.to(end), null);

        long timeSpentInNanoSeconds = end[0] - start[0];
        System.out.format("Time spent in %s: %s\n", functionName, getNanosecondsAsSensibleUnit(timeSpentInNanoSeconds));
    }

    private String getNanosecondsAsSensibleUnit(long ns) {
        if (ns < 1000) {
            return ns + "ns";
        } else if (ns >= 1000 && ns < 1000000) {
            return ns / 1000.0 + "µs";
        } else if (ns >= 1000000 && ns < 1000000000) {
            return ns / 1000000.0 + "ms";
        } else {
            return ns / 1000000000.0 + "s";
        }
    }
}
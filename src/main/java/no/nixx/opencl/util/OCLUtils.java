package no.nixx.opencl.util;

import org.jocl.*;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

import static org.jocl.CL.*;

/**
 * @author Oddbjørn Kvalsund
 */
public class OCLUtils {

    static {
        CL.setExceptionsEnabled(true);
    }

    public static int getNumberOfPlatforms() {
        int numPlatformsArray[] = new int[1];
        clGetPlatformIDs(0, null, numPlatformsArray);

        return numPlatformsArray[0];
    }

    public static cl_platform_id[] getPlatformIds() {
        cl_platform_id platforms[] = new cl_platform_id[getNumberOfPlatforms()];
        clGetPlatformIDs(platforms.length, platforms, null);

        return platforms;
    }

    public static cl_context_properties getContextPropertiesForPlatformId(cl_platform_id platformId) {
        cl_context_properties contextProperties = new cl_context_properties();
        contextProperties.addProperty(CL_CONTEXT_PLATFORM, platformId);

        return contextProperties;
    }

    public static int getNumberOfDevicesForPlatformId(cl_platform_id platformId) {
        int numDevicesArray[] = new int[1];
        clGetDeviceIDs(platformId, CL_DEVICE_TYPE_GPU, 0, null, numDevicesArray);

        return numDevicesArray[0];
    }

    public static cl_device_id[] getDeviceIdsForPlatformId(cl_platform_id platformId) {
        final int numDevices = getNumberOfDevicesForPlatformId(platformId);
        final cl_device_id devices[] = new cl_device_id[numDevices];
        clGetDeviceIDs(platformId, CL_DEVICE_TYPE_GPU, numDevices, devices, null);

        return devices;
    }

    public static cl_device_id[] getDeviceIdsForPlatformIdAndType(cl_platform_id platformId, long type) {
        final int numDevices = getNumberOfDevicesForPlatformId(platformId);
        final cl_device_id devices[] = new cl_device_id[numDevices];
        clGetDeviceIDs(platformId, type, numDevices, devices, null); // type can be one of CL_DEVICE_TYPE_* defined in org.jocl.CL

        return devices;
    }

    public static cl_platform_id getFirstPlatformId() {
        for (cl_platform_id platformId : getPlatformIds()) {
            return platformId;
        }

        throw new RuntimeException("No platforms found!");
    }

    public static cl_device_id getFirstDeviceIdForPlatformId(cl_platform_id platformId) {
        for (cl_device_id deviceId : getDeviceIdsForPlatformId(platformId)) {
            return deviceId;
        }

        throw new RuntimeException("No devices found!");
    }

    public static cl_context getContextForPlatformIdAndDeviceId(cl_platform_id platformId, cl_device_id deviceId) {
        final cl_context_properties contextProperties = new cl_context_properties();
        contextProperties.addProperty(CL_CONTEXT_PLATFORM, platformId);

        return clCreateContext(contextProperties, 1, new cl_device_id[]{deviceId}, null, null, null);
    }

    public static cl_command_queue getCommandQueueForContextAndPlatformIdAndDeviceId(final cl_context context, cl_device_id deviceId) {
        long properties = 0;
        properties |= CL_QUEUE_PROFILING_ENABLE;
        properties |= CL_QUEUE_OUT_OF_ORDER_EXEC_MODE_ENABLE;

        return clCreateCommandQueue(context, deviceId, properties, null);
    }

    public static cl_program createProgramFromSource(cl_context context, String source) {
        final cl_program program = clCreateProgramWithSource(context, 1, new String[]{source}, null, null);
        clBuildProgram(program, 0, null, null, null, null);

        return program;
    }

    public static cl_kernel createKernelFromSource(cl_context context, String source, String kernelName) {
        final cl_program program = createProgramFromSource(context, source);

        return clCreateKernel(program, kernelName, null);
    }

    public static cl_kernel createKernelFromClasspathResource(cl_context context, String resourceName, String kernelName) {
        return createKernelFromSource(context, ClasspathUtils.getClasspathResourceAsString(resourceName), kernelName);
    }

    public static cl_mem createReadOnlyImage(cl_context context, BufferedImage image) {
        final DataBufferInt dataBufferSrc = (DataBufferInt) image.getRaster().getDataBuffer();
        final int dataSrc[] = dataBufferSrc.getData();

        final cl_image_format imageFormat = new cl_image_format();
        imageFormat.image_channel_order = CL_RGBA;
        imageFormat.image_channel_data_type = CL_UNSIGNED_INT8;

        final long imageSizeX = image.getWidth();
        final long imageSizeY = image.getHeight();

        return clCreateImage2D(
                context, CL_MEM_READ_ONLY | CL_MEM_USE_HOST_PTR,
                new cl_image_format[]{imageFormat}, imageSizeX, imageSizeY,
                imageSizeX * Sizeof.cl_uint, Pointer.to(dataSrc), null);
    }

    public static cl_mem createWritableImage(cl_context context, long imageSizeX, long imageSizeY) {
        final cl_image_format imageFormat = new cl_image_format();
        imageFormat.image_channel_order = CL_RGBA;
        imageFormat.image_channel_data_type = CL_UNSIGNED_INT8;

        return clCreateImage2D(
                context, CL_MEM_WRITE_ONLY,
                new cl_image_format[]{imageFormat}, imageSizeX, imageSizeY,
                0, null, null);
    }


    // Device information

    public boolean hasImage2dSupport(cl_device_id deviceId) {
        int imageSupport[] = new int[1];
        clGetDeviceInfo(deviceId, CL.CL_DEVICE_IMAGE_SUPPORT, Sizeof.cl_int, Pointer.to(imageSupport), null);

        return imageSupport[0] != 0;
    }

    public long getMaxImage2DWidth(cl_device_id deviceId) {
        final long val[] = new long[1];
        clGetDeviceInfo(deviceId, CL.CL_DEVICE_IMAGE2D_MAX_WIDTH, Sizeof.size_t, Pointer.to(val), null);

        return val[0];
    }

    public long getMaxImage2dHeight(cl_device_id deviceId) {
        final long val[] = new long[1];
        clGetDeviceInfo(deviceId, CL.CL_DEVICE_IMAGE2D_MAX_HEIGHT, Sizeof.size_t, Pointer.to(val), null);

        return val[0];
    }
}
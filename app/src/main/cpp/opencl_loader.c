#include <dlfcn.h>
#include <stddef.h>
#include <stdint.h>
#include <android/log.h>

#define TAG "OpenCL_Shim"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)

static void* g_driver = NULL;
static int g_init = 0;

static void* get_driver(void) {
    if (g_init) return g_driver;
    g_init = 1;

    static const char* const candidates[] = {
        "/vendor/lib64/libOpenCL.so",
        "/system/vendor/lib64/libOpenCL.so",
        "/vendor/lib64/egl/libGLESv2_adreno.so",
        "/system/lib64/libOpenCL.so",
        "/apex/com.android.runtime/lib64/libOpenCL.so",
        NULL
    };

    for (int i = 0; candidates[i] != NULL; ++i) {
        void* h = dlopen(candidates[i], RTLD_NOW | RTLD_LOCAL);
        if (h) {
            LOGI("Bound Android hardware OpenCL driver at %s", candidates[i]);
            g_driver = h;
            return g_driver;
        }
    }
    LOGW("Hardware OpenCL driver not accessible; falling back to CPU execution.");
    return NULL;
}

static void* resolve_sym(const char* name) {
    void* d = get_driver();
    return d ? dlsym(d, name) : NULL;
}

#define CL_API_ENTRY
#define CL_API_CALL
#define CL_CALLBACK

typedef int32_t cl_int;
typedef uint32_t cl_uint;
typedef uint64_t cl_ulong;
typedef uint64_t cl_bitfield;
typedef void* cl_platform_id;
typedef void* cl_device_id;
typedef void* cl_context;
typedef void* cl_command_queue;
typedef void* cl_mem;
typedef void* cl_program;
typedef void* cl_kernel;
typedef void* cl_event;
typedef void* cl_sampler;

#define CL_SUCCESS 0
#define CL_DEVICE_NOT_FOUND -1
#define CL_DEVICE_NOT_AVAILABLE -2
#define CL_INVALID_PLATFORM -32

CL_API_ENTRY cl_int CL_API_CALL
clGetPlatformIDs(cl_uint num_entries, cl_platform_id *platforms, cl_uint *num_platforms) {
    typedef cl_int (*fn_t)(cl_uint, cl_platform_id*, cl_uint*);
    static fn_t fn = NULL;
    static int looked = 0;
    if (!looked) { fn = (fn_t)resolve_sym("clGetPlatformIDs"); looked = 1; }
    if (fn) return fn(num_entries, platforms, num_platforms);
    if (num_platforms) *num_platforms = 0;
    return CL_DEVICE_NOT_FOUND;
}

CL_API_ENTRY cl_int CL_API_CALL
clGetPlatformInfo(cl_platform_id platform, cl_uint param_name, size_t param_value_size, void *param_value, size_t *param_value_size_ret) {
    typedef cl_int (*fn_t)(cl_platform_id, cl_uint, size_t, void*, size_t*);
    static fn_t fn = NULL;
    static int looked = 0;
    if (!looked) { fn = (fn_t)resolve_sym("clGetPlatformInfo"); looked = 1; }
    if (fn) return fn(platform, param_name, param_value_size, param_value, param_value_size_ret);
    return CL_INVALID_PLATFORM;
}

CL_API_ENTRY cl_int CL_API_CALL
clGetDeviceIDs(cl_platform_id platform, cl_bitfield device_type, cl_uint num_entries, cl_device_id *devices, cl_uint *num_devices) {
    typedef cl_int (*fn_t)(cl_platform_id, cl_bitfield, cl_uint, cl_device_id*, cl_uint*);
    static fn_t fn = NULL;
    static int looked = 0;
    if (!looked) { fn = (fn_t)resolve_sym("clGetDeviceIDs"); looked = 1; }
    if (fn) return fn(platform, device_type, num_entries, devices, num_devices);
    if (num_devices) *num_devices = 0;
    return CL_DEVICE_NOT_FOUND;
}

CL_API_ENTRY cl_int CL_API_CALL
clGetDeviceInfo(cl_device_id device, cl_uint param_name, size_t param_value_size, void *param_value, size_t *param_value_size_ret) {
    typedef cl_int (*fn_t)(cl_device_id, cl_uint, size_t, void*, size_t*);
    static fn_t fn = NULL;
    static int looked = 0;
    if (!looked) { fn = (fn_t)resolve_sym("clGetDeviceInfo"); looked = 1; }
    if (fn) return fn(device, param_name, param_value_size, param_value, param_value_size_ret);
    return CL_DEVICE_NOT_FOUND;
}

CL_API_ENTRY cl_context CL_API_CALL
clCreateContext(const intptr_t *properties, cl_uint num_devices, const cl_device_id *devices, void (CL_CALLBACK *pfn_notify)(const char *, const void *, size_t, void *), void *user_data, cl_int *errcode_ret) {
    typedef cl_context (*fn_t)(const intptr_t*, cl_uint, const cl_device_id*, void*, void*, cl_int*);
    static fn_t fn = NULL;
    static int looked = 0;
    if (!looked) { fn = (fn_t)resolve_sym("clCreateContext"); looked = 1; }
    if (fn) return fn(properties, num_devices, devices, (void*)pfn_notify, user_data, errcode_ret);
    if (errcode_ret) *errcode_ret = CL_DEVICE_NOT_FOUND;
    return NULL;
}

CL_API_ENTRY cl_context CL_API_CALL
clCreateContextFromType(const intptr_t *properties, cl_bitfield device_type, void (CL_CALLBACK *pfn_notify)(const char *, const void *, size_t, void *), void *user_data, cl_int *errcode_ret) {
    typedef cl_context (*fn_t)(const intptr_t*, cl_bitfield, void*, void*, cl_int*);
    static fn_t fn = NULL;
    static int looked = 0;
    if (!looked) { fn = (fn_t)resolve_sym("clCreateContextFromType"); looked = 1; }
    if (fn) return fn(properties, device_type, (void*)pfn_notify, user_data, errcode_ret);
    if (errcode_ret) *errcode_ret = CL_DEVICE_NOT_FOUND;
    return NULL;
}

CL_API_ENTRY cl_int CL_API_CALL
clRetainContext(cl_context context) {
    typedef cl_int (*fn_t)(cl_context);
    static fn_t fn = NULL;
    static int looked = 0;
    if (!looked) { fn = (fn_t)resolve_sym("clRetainContext"); looked = 1; }
    return fn ? fn(context) : CL_SUCCESS;
}

CL_API_ENTRY cl_int CL_API_CALL
clReleaseContext(cl_context context) {
    typedef cl_int (*fn_t)(cl_context);
    static fn_t fn = NULL;
    static int looked = 0;
    if (!looked) { fn = (fn_t)resolve_sym("clReleaseContext"); looked = 1; }
    return fn ? fn(context) : CL_SUCCESS;
}

CL_API_ENTRY cl_command_queue CL_API_CALL
clCreateCommandQueue(cl_context context, cl_device_id device, cl_bitfield properties, cl_int *errcode_ret) {
    typedef cl_command_queue (*fn_t)(cl_context, cl_device_id, cl_bitfield, cl_int*);
    static fn_t fn = NULL;
    static int looked = 0;
    if (!looked) { fn = (fn_t)resolve_sym("clCreateCommandQueue"); looked = 1; }
    if (fn) return fn(context, device, properties, errcode_ret);
    if (errcode_ret) *errcode_ret = CL_DEVICE_NOT_FOUND;
    return NULL;
}

CL_API_ENTRY cl_command_queue CL_API_CALL
clCreateCommandQueueWithProperties(cl_context context, cl_device_id device, const uint64_t *properties, cl_int *errcode_ret) {
    typedef cl_command_queue (*fn_t)(cl_context, cl_device_id, const uint64_t*, cl_int*);
    static fn_t fn = NULL;
    static int looked = 0;
    if (!looked) { fn = (fn_t)resolve_sym("clCreateCommandQueueWithProperties"); looked = 1; }
    if (fn) return fn(context, device, properties, errcode_ret);
    if (errcode_ret) *errcode_ret = CL_DEVICE_NOT_FOUND;
    return NULL;
}

CL_API_ENTRY cl_int CL_API_CALL
clRetainCommandQueue(cl_command_queue command_queue) {
    typedef cl_int (*fn_t)(cl_command_queue);
    static fn_t fn = NULL;
    static int looked = 0;
    if (!looked) { fn = (fn_t)resolve_sym("clRetainCommandQueue"); looked = 1; }
    return fn ? fn(command_queue) : CL_SUCCESS;
}

CL_API_ENTRY cl_int CL_API_CALL
clReleaseCommandQueue(cl_command_queue command_queue) {
    typedef cl_int (*fn_t)(cl_command_queue);
    static fn_t fn = NULL;
    static int looked = 0;
    if (!looked) { fn = (fn_t)resolve_sym("clReleaseCommandQueue"); looked = 1; }
    return fn ? fn(command_queue) : CL_SUCCESS;
}

CL_API_ENTRY cl_mem CL_API_CALL
clCreateBuffer(cl_context context, cl_bitfield flags, size_t size, void *host_ptr, cl_int *errcode_ret) {
    typedef cl_mem (*fn_t)(cl_context, cl_bitfield, size_t, void*, cl_int*);
    static fn_t fn = NULL;
    static int looked = 0;
    if (!looked) { fn = (fn_t)resolve_sym("clCreateBuffer"); looked = 1; }
    if (fn) return fn(context, flags, size, host_ptr, errcode_ret);
    if (errcode_ret) *errcode_ret = CL_DEVICE_NOT_FOUND;
    return NULL;
}

CL_API_ENTRY cl_mem CL_API_CALL
clCreateBufferWithProperties(cl_context context, const uint64_t *properties, cl_bitfield flags, size_t size, void *host_ptr, cl_int *errcode_ret) {
    typedef cl_mem (*fn_t)(cl_context, const uint64_t*, cl_bitfield, size_t, void*, cl_int*);
    static fn_t fn = NULL;
    static int looked = 0;
    if (!looked) { fn = (fn_t)resolve_sym("clCreateBufferWithProperties"); looked = 1; }
    if (fn) return fn(context, properties, flags, size, host_ptr, errcode_ret);
    if (errcode_ret) *errcode_ret = CL_DEVICE_NOT_FOUND;
    return NULL;
}

CL_API_ENTRY cl_mem CL_API_CALL
clCreateSubBuffer(cl_mem buffer, cl_bitfield flags, uint32_t buffer_create_type, const void *buffer_create_info, cl_int *errcode_ret) {
    typedef cl_mem (*fn_t)(cl_mem, cl_bitfield, uint32_t, const void*, cl_int*);
    static fn_t fn = NULL;
    static int looked = 0;
    if (!looked) { fn = (fn_t)resolve_sym("clCreateSubBuffer"); looked = 1; }
    if (fn) return fn(buffer, flags, buffer_create_type, buffer_create_info, errcode_ret);
    if (errcode_ret) *errcode_ret = CL_DEVICE_NOT_FOUND;
    return NULL;
}

CL_API_ENTRY cl_mem CL_API_CALL
clCreateImage(cl_context context, cl_bitfield flags, const void *image_format, const void *image_desc, void *host_ptr, cl_int *errcode_ret) {
    typedef cl_mem (*fn_t)(cl_context, cl_bitfield, const void*, const void*, void*, cl_int*);
    static fn_t fn = NULL;
    static int looked = 0;
    if (!looked) { fn = (fn_t)resolve_sym("clCreateImage"); looked = 1; }
    if (fn) return fn(context, flags, image_format, image_desc, host_ptr, errcode_ret);
    if (errcode_ret) *errcode_ret = CL_DEVICE_NOT_FOUND;
    return NULL;
}

CL_API_ENTRY cl_int CL_API_CALL
clRetainMemObject(cl_mem memobj) {
    typedef cl_int (*fn_t)(cl_mem);
    static fn_t fn = NULL;
    static int looked = 0;
    if (!looked) { fn = (fn_t)resolve_sym("clRetainMemObject"); looked = 1; }
    return fn ? fn(memobj) : CL_SUCCESS;
}

CL_API_ENTRY cl_int CL_API_CALL
clReleaseMemObject(cl_mem memobj) {
    typedef cl_int (*fn_t)(cl_mem);
    static fn_t fn = NULL;
    static int looked = 0;
    if (!looked) { fn = (fn_t)resolve_sym("clReleaseMemObject"); looked = 1; }
    return fn ? fn(memobj) : CL_SUCCESS;
}

CL_API_ENTRY cl_program CL_API_CALL
clCreateProgramWithSource(cl_context context, cl_uint count, const char **strings, const size_t *lengths, cl_int *errcode_ret) {
    typedef cl_program (*fn_t)(cl_context, cl_uint, const char**, const size_t*, cl_int*);
    static fn_t fn = NULL;
    static int looked = 0;
    if (!looked) { fn = (fn_t)resolve_sym("clCreateProgramWithSource"); looked = 1; }
    if (fn) return fn(context, count, strings, lengths, errcode_ret);
    if (errcode_ret) *errcode_ret = CL_DEVICE_NOT_FOUND;
    return NULL;
}

CL_API_ENTRY cl_program CL_API_CALL
clCreateProgramWithBinary(cl_context context, cl_uint num_devices, const cl_device_id *device_list, const size_t *lengths, const unsigned char **binaries, cl_int *binary_status, cl_int *errcode_ret) {
    typedef cl_program (*fn_t)(cl_context, cl_uint, const cl_device_id*, const size_t*, const unsigned char**, cl_int*, cl_int*);
    static fn_t fn = NULL;
    static int looked = 0;
    if (!looked) { fn = (fn_t)resolve_sym("clCreateProgramWithBinary"); looked = 1; }
    if (fn) return fn(context, num_devices, device_list, lengths, binaries, binary_status, errcode_ret);
    if (errcode_ret) *errcode_ret = CL_DEVICE_NOT_FOUND;
    return NULL;
}

CL_API_ENTRY cl_int CL_API_CALL
clRetainProgram(cl_program program) {
    typedef cl_int (*fn_t)(cl_program);
    static fn_t fn = NULL;
    static int looked = 0;
    if (!looked) { fn = (fn_t)resolve_sym("clRetainProgram"); looked = 1; }
    return fn ? fn(program) : CL_SUCCESS;
}

CL_API_ENTRY cl_int CL_API_CALL
clReleaseProgram(cl_program program) {
    typedef cl_int (*fn_t)(cl_program);
    static fn_t fn = NULL;
    static int looked = 0;
    if (!looked) { fn = (fn_t)resolve_sym("clReleaseProgram"); looked = 1; }
    return fn ? fn(program) : CL_SUCCESS;
}

CL_API_ENTRY cl_int CL_API_CALL
clBuildProgram(cl_program program, cl_uint num_devices, const cl_device_id *device_list, const char *options, void (CL_CALLBACK *pfn_notify)(cl_program, void *), void *user_data) {
    typedef cl_int (*fn_t)(cl_program, cl_uint, const cl_device_id*, const char*, void*, void*);
    static fn_t fn = NULL;
    static int looked = 0;
    if (!looked) { fn = (fn_t)resolve_sym("clBuildProgram"); looked = 1; }
    if (fn) return fn(program, num_devices, device_list, options, (void*)pfn_notify, user_data);
    return CL_DEVICE_NOT_FOUND;
}

CL_API_ENTRY cl_int CL_API_CALL
clGetProgramBuildInfo(cl_program program, cl_device_id device, cl_uint param_name, size_t param_value_size, void *param_value, size_t *param_value_size_ret) {
    typedef cl_int (*fn_t)(cl_program, cl_device_id, cl_uint, size_t, void*, size_t*);
    static fn_t fn = NULL;
    static int looked = 0;
    if (!looked) { fn = (fn_t)resolve_sym("clGetProgramBuildInfo"); looked = 1; }
    if (fn) return fn(program, device, param_name, param_value_size, param_value, param_value_size_ret);
    return CL_DEVICE_NOT_FOUND;
}

CL_API_ENTRY cl_int CL_API_CALL
clGetProgramInfo(cl_program program, cl_uint param_name, size_t param_value_size, void *param_value, size_t *param_value_size_ret) {
    typedef cl_int (*fn_t)(cl_program, cl_uint, size_t, void*, size_t*);
    static fn_t fn = NULL;
    static int looked = 0;
    if (!looked) { fn = (fn_t)resolve_sym("clGetProgramInfo"); looked = 1; }
    if (fn) return fn(program, param_name, param_value_size, param_value, param_value_size_ret);
    return CL_DEVICE_NOT_FOUND;
}

CL_API_ENTRY cl_kernel CL_API_CALL
clCreateKernel(cl_program program, const char *kernel_name, cl_int *errcode_ret) {
    typedef cl_kernel (*fn_t)(cl_program, const char*, cl_int*);
    static fn_t fn = NULL;
    static int looked = 0;
    if (!looked) { fn = (fn_t)resolve_sym("clCreateKernel"); looked = 1; }
    if (fn) return fn(program, kernel_name, errcode_ret);
    if (errcode_ret) *errcode_ret = CL_DEVICE_NOT_FOUND;
    return NULL;
}

CL_API_ENTRY cl_int CL_API_CALL
clRetainKernel(cl_kernel kernel) {
    typedef cl_int (*fn_t)(cl_kernel);
    static fn_t fn = NULL;
    static int looked = 0;
    if (!looked) { fn = (fn_t)resolve_sym("clRetainKernel"); looked = 1; }
    return fn ? fn(kernel) : CL_SUCCESS;
}

CL_API_ENTRY cl_int CL_API_CALL
clReleaseKernel(cl_kernel kernel) {
    typedef cl_int (*fn_t)(cl_kernel);
    static fn_t fn = NULL;
    static int looked = 0;
    if (!looked) { fn = (fn_t)resolve_sym("clReleaseKernel"); looked = 1; }
    return fn ? fn(kernel) : CL_SUCCESS;
}

CL_API_ENTRY cl_int CL_API_CALL
clSetKernelArg(cl_kernel kernel, cl_uint arg_index, size_t arg_size, const void *arg_value) {
    typedef cl_int (*fn_t)(cl_kernel, cl_uint, size_t, const void*);
    static fn_t fn = NULL;
    static int looked = 0;
    if (!looked) { fn = (fn_t)resolve_sym("clSetKernelArg"); looked = 1; }
    if (fn) return fn(kernel, arg_index, arg_size, arg_value);
    return CL_DEVICE_NOT_FOUND;
}

CL_API_ENTRY cl_int CL_API_CALL
clGetKernelWorkGroupInfo(cl_kernel kernel, cl_device_id device, cl_uint param_name, size_t param_value_size, void *param_value, size_t *param_value_size_ret) {
    typedef cl_int (*fn_t)(cl_kernel, cl_device_id, cl_uint, size_t, void*, size_t*);
    static fn_t fn = NULL;
    static int looked = 0;
    if (!looked) { fn = (fn_t)resolve_sym("clGetKernelWorkGroupInfo"); looked = 1; }
    if (fn) return fn(kernel, device, param_name, param_value_size, param_value, param_value_size_ret);
    return CL_DEVICE_NOT_FOUND;
}

CL_API_ENTRY cl_int CL_API_CALL
clGetKernelSubGroupInfo(cl_kernel kernel, cl_device_id device, cl_uint param_name, size_t input_value_size, const void *input_value, size_t param_value_size, void *param_value, size_t *param_value_size_ret) {
    typedef cl_int (*fn_t)(cl_kernel, cl_device_id, cl_uint, size_t, const void*, size_t, void*, size_t*);
    static fn_t fn = NULL;
    static int looked = 0;
    if (!looked) { fn = (fn_t)resolve_sym("clGetKernelSubGroupInfo"); looked = 1; }
    if (fn) return fn(kernel, device, param_name, input_value_size, input_value, param_value_size, param_value, param_value_size_ret);
    return CL_DEVICE_NOT_FOUND;
}

CL_API_ENTRY cl_int CL_API_CALL
clEnqueueNDRangeKernel(cl_command_queue command_queue, cl_kernel kernel, cl_uint work_dim, const size_t *global_work_offset, const size_t *global_work_size, const size_t *local_work_size, cl_uint num_events_in_wait_list, const cl_event *event_wait_list, cl_event *event) {
    typedef cl_int (*fn_t)(cl_command_queue, cl_kernel, cl_uint, const size_t*, const size_t*, const size_t*, cl_uint, const cl_event*, cl_event*);
    static fn_t fn = NULL;
    static int looked = 0;
    if (!looked) { fn = (fn_t)resolve_sym("clEnqueueNDRangeKernel"); looked = 1; }
    if (fn) return fn(command_queue, kernel, work_dim, global_work_offset, global_work_size, local_work_size, num_events_in_wait_list, event_wait_list, event);
    return CL_DEVICE_NOT_FOUND;
}

CL_API_ENTRY cl_int CL_API_CALL
clEnqueueReadBuffer(cl_command_queue command_queue, cl_mem buffer, uint32_t blocking_read, size_t offset, size_t size, void *ptr, cl_uint num_events_in_wait_list, const cl_event *event_wait_list, cl_event *event) {
    typedef cl_int (*fn_t)(cl_command_queue, cl_mem, uint32_t, size_t, size_t, void*, cl_uint, const cl_event*, cl_event*);
    static fn_t fn = NULL;
    static int looked = 0;
    if (!looked) { fn = (fn_t)resolve_sym("clEnqueueReadBuffer"); looked = 1; }
    if (fn) return fn(command_queue, buffer, blocking_read, offset, size, ptr, num_events_in_wait_list, event_wait_list, event);
    return CL_DEVICE_NOT_FOUND;
}

CL_API_ENTRY cl_int CL_API_CALL
clEnqueueWriteBuffer(cl_command_queue command_queue, cl_mem buffer, uint32_t blocking_write, size_t offset, size_t size, const void *ptr, cl_uint num_events_in_wait_list, const cl_event *event_wait_list, cl_event *event) {
    typedef cl_int (*fn_t)(cl_command_queue, cl_mem, uint32_t, size_t, size_t, const void*, cl_uint, const cl_event*, cl_event*);
    static fn_t fn = NULL;
    static int looked = 0;
    if (!looked) { fn = (fn_t)resolve_sym("clEnqueueWriteBuffer"); looked = 1; }
    if (fn) return fn(command_queue, buffer, blocking_write, offset, size, ptr, num_events_in_wait_list, event_wait_list, event);
    return CL_DEVICE_NOT_FOUND;
}

CL_API_ENTRY cl_int CL_API_CALL
clEnqueueCopyBuffer(cl_command_queue command_queue, cl_mem src_buffer, cl_mem dst_buffer, size_t src_offset, size_t dst_offset, size_t size, cl_uint num_events_in_wait_list, const cl_event *event_wait_list, cl_event *event) {
    typedef cl_int (*fn_t)(cl_command_queue, cl_mem, cl_mem, size_t, size_t, size_t, cl_uint, const cl_event*, cl_event*);
    static fn_t fn = NULL;
    static int looked = 0;
    if (!looked) { fn = (fn_t)resolve_sym("clEnqueueCopyBuffer"); looked = 1; }
    if (fn) return fn(command_queue, src_buffer, dst_buffer, src_offset, dst_offset, size, num_events_in_wait_list, event_wait_list, event);
    return CL_DEVICE_NOT_FOUND;
}

CL_API_ENTRY void* CL_API_CALL
clEnqueueMapBuffer(cl_command_queue command_queue, cl_mem buffer, uint32_t blocking_map, cl_bitfield map_flags, size_t offset, size_t size, cl_uint num_events_in_wait_list, const cl_event *event_wait_list, cl_event *event, cl_int *errcode_ret) {
    typedef void* (*fn_t)(cl_command_queue, cl_mem, uint32_t, cl_bitfield, size_t, size_t, cl_uint, const cl_event*, cl_event*, cl_int*);
    static fn_t fn = NULL;
    static int looked = 0;
    if (!looked) { fn = (fn_t)resolve_sym("clEnqueueMapBuffer"); looked = 1; }
    if (fn) return fn(command_queue, buffer, blocking_map, map_flags, offset, size, num_events_in_wait_list, event_wait_list, event, errcode_ret);
    if (errcode_ret) *errcode_ret = CL_DEVICE_NOT_FOUND;
    return NULL;
}

CL_API_ENTRY cl_int CL_API_CALL
clEnqueueUnmapMemObject(cl_command_queue command_queue, cl_mem memobj, void *mapped_ptr, cl_uint num_events_in_wait_list, const cl_event *event_wait_list, cl_event *event) {
    typedef cl_int (*fn_t)(cl_command_queue, cl_mem, void*, cl_uint, const cl_event*, cl_event*);
    static fn_t fn = NULL;
    static int looked = 0;
    if (!looked) { fn = (fn_t)resolve_sym("clEnqueueUnmapMemObject"); looked = 1; }
    if (fn) return fn(command_queue, memobj, mapped_ptr, num_events_in_wait_list, event_wait_list, event);
    return CL_DEVICE_NOT_FOUND;
}

CL_API_ENTRY cl_int CL_API_CALL
clEnqueueFillBuffer(cl_command_queue command_queue, cl_mem buffer, const void *pattern, size_t pattern_size, size_t offset, size_t size, cl_uint num_events_in_wait_list, const cl_event *event_wait_list, cl_event *event) {
    typedef cl_int (*fn_t)(cl_command_queue, cl_mem, const void*, size_t, size_t, size_t, cl_uint, const cl_event*, cl_event*);
    static fn_t fn = NULL;
    static int looked = 0;
    if (!looked) { fn = (fn_t)resolve_sym("clEnqueueFillBuffer"); looked = 1; }
    if (fn) return fn(command_queue, buffer, pattern, pattern_size, offset, size, num_events_in_wait_list, event_wait_list, event);
    return CL_DEVICE_NOT_FOUND;
}

CL_API_ENTRY cl_int CL_API_CALL
clFlush(cl_command_queue command_queue) {
    typedef cl_int (*fn_t)(cl_command_queue);
    static fn_t fn = NULL;
    static int looked = 0;
    if (!looked) { fn = (fn_t)resolve_sym("clFlush"); looked = 1; }
    return fn ? fn(command_queue) : CL_SUCCESS;
}

CL_API_ENTRY cl_int CL_API_CALL
clFinish(cl_command_queue command_queue) {
    typedef cl_int (*fn_t)(cl_command_queue);
    static fn_t fn = NULL;
    static int looked = 0;
    if (!looked) { fn = (fn_t)resolve_sym("clFinish"); looked = 1; }
    return fn ? fn(command_queue) : CL_SUCCESS;
}

CL_API_ENTRY cl_int CL_API_CALL
clWaitForEvents(cl_uint num_events, const cl_event *event_list) {
    typedef cl_int (*fn_t)(cl_uint, const cl_event*);
    static fn_t fn = NULL;
    static int looked = 0;
    if (!looked) { fn = (fn_t)resolve_sym("clWaitForEvents"); looked = 1; }
    return fn ? fn(num_events, event_list) : CL_SUCCESS;
}

CL_API_ENTRY cl_int CL_API_CALL
clRetainEvent(cl_event event) {
    typedef cl_int (*fn_t)(cl_event);
    static fn_t fn = NULL;
    static int looked = 0;
    if (!looked) { fn = (fn_t)resolve_sym("clRetainEvent"); looked = 1; }
    return fn ? fn(event) : CL_SUCCESS;
}

CL_API_ENTRY cl_int CL_API_CALL
clReleaseEvent(cl_event event) {
    typedef cl_int (*fn_t)(cl_event);
    static fn_t fn = NULL;
    static int looked = 0;
    if (!looked) { fn = (fn_t)resolve_sym("clReleaseEvent"); looked = 1; }
    return fn ? fn(event) : CL_SUCCESS;
}

CL_API_ENTRY cl_int CL_API_CALL
clEnqueueBarrierWithWaitList(cl_command_queue command_queue, cl_uint num_events_in_wait_list, const cl_event *event_wait_list, cl_event *event) {
    typedef cl_int (*fn_t)(cl_command_queue, cl_uint, const cl_event*, cl_event*);
    static fn_t fn = NULL;
    static int looked = 0;
    if (!looked) { fn = (fn_t)resolve_sym("clEnqueueBarrierWithWaitList"); looked = 1; }
    if (fn) return fn(command_queue, num_events_in_wait_list, event_wait_list, event);
    return CL_SUCCESS;
}

CL_API_ENTRY cl_int CL_API_CALL
clEnqueueMarkerWithWaitList(cl_command_queue command_queue, cl_uint num_events_in_wait_list, const cl_event *event_wait_list, cl_event *event) {
    typedef cl_int (*fn_t)(cl_command_queue, cl_uint, const cl_event*, cl_event*);
    static fn_t fn = NULL;
    static int looked = 0;
    if (!looked) { fn = (fn_t)resolve_sym("clEnqueueMarkerWithWaitList"); looked = 1; }
    if (fn) return fn(command_queue, num_events_in_wait_list, event_wait_list, event);
    return CL_SUCCESS;
}

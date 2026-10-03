#include <jni.h>
#include <vulkan/vulkan.h>

#include <sstream>
#include <string>
#include <vector>

namespace {
std::string Json(const std::string& value) {
  std::string result = "\"";
  for (unsigned char c : value) {
    if (c == '\\' || c == '"') result += '\\';
    if (c >= 32) result += char(c);
  }
  return result + '"';
}

// Neither renderer can start: the same reason for both verdicts.
std::string Failure(const std::string& reason) {
  const std::string missing = "[" + Json(reason) + "]";
  return "{\"compatible\":false,\"missing\":" + missing +
      ",\"xenosCompatible\":false,\"xenosMissing\":" + missing + "}";
}

std::string Probe() {
  uint32_t loader_version = VK_API_VERSION_1_0;
  auto version = reinterpret_cast<PFN_vkEnumerateInstanceVersion>(
      vkGetInstanceProcAddr(VK_NULL_HANDLE, "vkEnumerateInstanceVersion"));
  if (version) version(&loader_version);
  VkApplicationInfo app{VK_STRUCTURE_TYPE_APPLICATION_INFO};
  app.pApplicationName = "NFSMW diagnostics";
  app.apiVersion = loader_version >= VK_API_VERSION_1_2 ? VK_API_VERSION_1_2
      : loader_version >= VK_API_VERSION_1_1 ? VK_API_VERSION_1_1 : VK_API_VERSION_1_0;
  VkInstanceCreateInfo ci{VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO};
  ci.pApplicationInfo = &app;
  VkInstance instance{};
  VkResult result = vkCreateInstance(&ci, nullptr, &instance);
  if (result != VK_SUCCESS) {
    return Failure("Could not initialize Vulkan (" + std::to_string(result) + ")");
  }
  uint32_t count = 0;
  result = vkEnumeratePhysicalDevices(instance, &count, nullptr);
  if (result != VK_SUCCESS || !count) {
    vkDestroyInstance(instance, nullptr);
    return Failure("No Vulkan GPU found");
  }
  std::vector<VkPhysicalDevice> devices(count);
  result = vkEnumeratePhysicalDevices(instance, &count, devices.data());
  if (result != VK_SUCCESS) {
    vkDestroyInstance(instance, nullptr);
    return Failure("Could not query the GPU");
  }
  // Android's native renderer selects the first physical device by default.
  VkPhysicalDevice gpu = devices[0];
  VkPhysicalDeviceProperties props{};
  VkPhysicalDeviceFeatures features{};
  vkGetPhysicalDeviceProperties(gpu, &props);
  vkGetPhysicalDeviceFeatures(gpu, &features);
  VkPhysicalDeviceVulkan12Features v12{VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_2_FEATURES};
  VkPhysicalDeviceFeatures2 fs{VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2};
  if (props.apiVersion >= VK_API_VERSION_1_2) {
    fs.pNext = &v12;
    auto query = reinterpret_cast<PFN_vkGetPhysicalDeviceFeatures2>(
        vkGetInstanceProcAddr(instance, "vkGetPhysicalDeviceFeatures2"));
    if (query) query(gpu, &fs);
  }
  std::vector<std::string> missing;
  auto require = [&](bool supported, const char* name) {
    if (!supported) missing.emplace_back(name);
  };
  // The SDK currently enables the native shader interface through Vulkan 1.2.
  require(props.apiVersion >= VK_API_VERSION_1_2, "Vulkan 1.2 or later");
  require(features.shaderInt64, "shaderInt64");
  require(features.shaderSampledImageArrayDynamicIndexing, "shaderSampledImageArrayDynamicIndexing");
  require(v12.bufferDeviceAddress, "bufferDeviceAddress (Vulkan 1.2 interface)");
  require(v12.runtimeDescriptorArray, "runtimeDescriptorArray");
  require(v12.descriptorBindingPartiallyBound, "descriptorBindingPartiallyBound");
  require(v12.descriptorBindingSampledImageUpdateAfterBind, "descriptorBindingSampledImageUpdateAfterBind");
  require(v12.descriptorBindingUpdateUnusedWhilePending, "descriptorBindingUpdateUnusedWhilePending");
  // Both renderers create their Vulkan device with the SDK's GPU emulation checks
  // (VulkanDevice::CreateIfSupported with with_gpu_emulation, sdk/src/ui/vulkan/vulkan_device.cpp): a GPU
  // without these features is refused before anything is drawn. In the compatibility mode (xenos), the command
  // processor checks the two stores-and-atomics features again when it starts.
  std::vector<std::string> xenos_missing;
  auto require_both = [&](bool supported, const char* name) {
    if (!supported) {
      missing.emplace_back(name);
      xenos_missing.emplace_back(name);
    }
  };
  require_both(features.independentBlend, "independentBlend");
  require_both(features.fragmentStoresAndAtomics, "fragmentStoresAndAtomics");
  require_both(features.vertexPipelineStoresAndAtomics, "vertexPipelineStoresAndAtomics");
  // The launcher turns these two checks off only for the compatibility mode (GameOptions.arguments).
  require(features.geometryShader, "geometryShader");
  require(features.fillModeNonSolid, "fillModeNonSolid");
  std::ostringstream formats;
  formats << '{';
  const VkFormat bc[] = {VK_FORMAT_BC1_RGBA_UNORM_BLOCK, VK_FORMAT_BC2_UNORM_BLOCK,
                         VK_FORMAT_BC3_UNORM_BLOCK, VK_FORMAT_BC4_UNORM_BLOCK, VK_FORMAT_BC5_UNORM_BLOCK};
  for (unsigned i = 0; i < 5; ++i) {
    VkFormatProperties fp{};
    vkGetPhysicalDeviceFormatProperties(gpu, bc[i], &fp);
    constexpr auto required = VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT |
        VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT;
    bool supported = (fp.optimalTilingFeatures & required) == required;
    std::string name = "BC" + std::to_string(i + 1);
    if (i < 3 && !supported) missing.push_back("Texture format " + name);
    if (i) formats << ',';
    formats << Json(name) << ':' << (supported ? "true" : "false");
  }
  formats << '}';
  std::ostringstream output;
  output << "{\"gpu\":" << Json(props.deviceName)
         << ",\"vulkan\":" << Json(std::to_string(VK_VERSION_MAJOR(props.apiVersion)) + "." +
              std::to_string(VK_VERSION_MINOR(props.apiVersion)) + "." + std::to_string(VK_VERSION_PATCH(props.apiVersion)))
         << ",\"driverVersion\":" << props.driverVersion
         << ",\"vendorId\":" << props.vendorID
         << ",\"textureFormats\":" << formats.str()
         << ",\"checkedRenderer\":\"nativo\""
         << ",\"compatible\":" << (missing.empty() ? "true" : "false") << ",\"missing\":[";
  for (size_t i = 0; i < missing.size(); ++i) {
    if (i) output << ',';
    output << Json(missing[i]);
  }
  output << "],\"xenosCompatible\":" << (xenos_missing.empty() ? "true" : "false") << ",\"xenosMissing\":[";
  for (size_t i = 0; i < xenos_missing.size(); ++i) {
    if (i) output << ',';
    output << Json(xenos_missing[i]);
  }
  output << "]}";
  vkDestroyInstance(instance, nullptr);
  return output.str();
}
}  // namespace

extern "C" JNIEXPORT jstring JNICALL
Java_com_nfsmw_android_Diagnostics_nativeGpuReport(JNIEnv* env, jclass) {
  const std::string report = Probe();
  return env->NewStringUTF(report.c_str());
}

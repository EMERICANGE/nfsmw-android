#include <jni.h>
#include <android/log.h>
#include <vulkan/vulkan.h>

#include <filesystem>
#include <string>
#include <vector>

namespace {
constexpr char kTag[] = "NFSMW";

void Log(int priority, const char* tag, const std::string& message) {
  __android_log_write(priority, tag, message.c_str());
}

std::string VulkanStatus() {
  uint32_t api_version = VK_API_VERSION_1_0;
  const auto enumerate_instance_version = reinterpret_cast<PFN_vkEnumerateInstanceVersion>(
      vkGetInstanceProcAddr(VK_NULL_HANDLE, "vkEnumerateInstanceVersion"));
  if (enumerate_instance_version != nullptr) {
    const VkResult version_result = enumerate_instance_version(&api_version);
    if (version_result != VK_SUCCESS) {
      Log(ANDROID_LOG_WARN, "NFSMW-VULKAN", "Vulkan version query failed; using 1.0: " +
          std::to_string(version_result));
      api_version = VK_API_VERSION_1_0;
    }
  }
  Log(ANDROID_LOG_INFO, "NFSMW-VULKAN", "Loader API version: " + std::to_string(api_version));

  VkApplicationInfo app_info{};
  app_info.sType = VK_STRUCTURE_TYPE_APPLICATION_INFO;
  app_info.pApplicationName = "Need for Speed Most Wanted";
  app_info.applicationVersion = VK_MAKE_API_VERSION(0, 0, 1, 0);
  app_info.pEngineName = "nfsmw-android";
  app_info.engineVersion = VK_MAKE_API_VERSION(0, 0, 1, 0);
  app_info.apiVersion = VK_API_VERSION_1_0;

  VkInstanceCreateInfo create_info{};
  create_info.sType = VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
  create_info.pApplicationInfo = &app_info;
  VkInstance instance = VK_NULL_HANDLE;
  const VkResult create_result = vkCreateInstance(&create_info, nullptr, &instance);
  if (create_result != VK_SUCCESS) {
    Log(ANDROID_LOG_ERROR, "NFSMW-VULKAN", "vkCreateInstance failed: " +
        std::to_string(create_result));
    return "Vulkan instance creation failed (" + std::to_string(create_result) + ")";
  }

  uint32_t count = 0;
  VkResult result = vkEnumeratePhysicalDevices(instance, &count, nullptr);
  if (result != VK_SUCCESS || count == 0) {
    Log(ANDROID_LOG_WARN, "NFSMW-VULKAN", "No Vulkan physical devices found; result=" +
        std::to_string(result));
    vkDestroyInstance(instance, nullptr);
    return "Vulkan loader ready; no GPU device reported";
  }

  std::vector<VkPhysicalDevice> devices(count);
  result = vkEnumeratePhysicalDevices(instance, &count, devices.data());
  std::string device_name = "unknown";
  if (result == VK_SUCCESS && count != 0) {
    VkPhysicalDeviceProperties properties{};
    vkGetPhysicalDeviceProperties(devices.front(), &properties);
    device_name = properties.deviceName;
    Log(ANDROID_LOG_INFO, "NFSMW-VULKAN", "Vulkan device: " + device_name);
  }
  vkDestroyInstance(instance, nullptr);
  return "Vulkan ready: " + device_name;
}
}  // namespace

extern "C" JNIEXPORT jstring JNICALL
Java_com_nfsmw_android_MainActivity_nativeInitialize(JNIEnv* env, jclass, jstring files_dir) {
  Log(ANDROID_LOG_INFO, kTag, "Android ARM64 native bootstrap starting");
  const char* path = env->GetStringUTFChars(files_dir, nullptr);
  if (path == nullptr) {
    Log(ANDROID_LOG_ERROR, kTag, "Could not read app-private files directory");
    return env->NewStringUTF("Startup failed: app storage path unavailable");
  }

  std::string storage_path(path);
  env->ReleaseStringUTFChars(files_dir, path);
  const std::filesystem::path root = std::filesystem::path(storage_path) / "nfsmw";
  std::error_code error;
  std::filesystem::create_directories(root / "game_root", error);
  if (!error) {
    std::filesystem::create_directories(root / "cache", error);
  }
  if (error) {
    Log(ANDROID_LOG_ERROR, "NFSMW-MEM", "App-private directory setup failed: " + error.message());
    return env->NewStringUTF("Startup failed: private app storage could not be initialized");
  }
  Log(ANDROID_LOG_INFO, "NFSMW-MEM", "Private game/cache directories ready: " + root.string());
  Log(ANDROID_LOG_INFO, "NFSMW-REX", "ReXGlue runtime integration is not connected yet");
  const std::string status = VulkanStatus();
  Log(ANDROID_LOG_INFO, kTag, "Native bootstrap complete");
  return env->NewStringUTF(status.c_str());
}

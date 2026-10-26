#include <jni.h>
#include <link.h>
#include <cstdint>
#include <cstdio>
#include <string>

namespace {
struct Engine { std::string path; uintptr_t bias = 0; unsigned executableSegments = 0; };
int visit(dl_phdr_info* info, size_t, void* opaque) {
    const std::string path = info->dlpi_name ? info->dlpi_name : "";
    const auto slash = path.find_last_of('/');
    if (path.substr(slash == std::string::npos ? 0 : slash + 1) != "libminecraftpe.so") return 0;
    auto& result = *static_cast<Engine*>(opaque);
    result.path = path;
    result.bias = static_cast<uintptr_t>(info->dlpi_addr);
    for (unsigned i = 0; i < info->dlpi_phnum; ++i) {
        const auto& ph = info->dlpi_phdr[i];
        if (ph.p_type == PT_LOAD && (ph.p_flags & PF_X)) ++result.executableSegments;
    }
    return 1;
}
}

// Enumerates loaded objects only. Never loads Minecraft, calls game functions,
// writes engine memory, or derives function addresses from unverified offsets.
extern "C" JNIEXPORT jobjectArray JNICALL
Java_dev_nevertime_client_bridge_NativeProbe_snapshot(JNIEnv* env, jclass) {
    Engine engine;
    dl_iterate_phdr(visit, &engine);
    jclass stringClass = env->FindClass("java/lang/String");
    if (!stringClass) return nullptr;
    jobjectArray result = env->NewObjectArray(3, stringClass, nullptr);
    env->DeleteLocalRef(stringClass);
    if (!result) return nullptr;
    char bias[32];
    std::snprintf(bias, sizeof(bias), "0x%llx", static_cast<unsigned long long>(engine.bias));
    std::string values[] = {engine.path, bias, std::to_string(engine.executableSegments)};
    for (jsize i = 0; i < 3; ++i) {
        jstring value = env->NewStringUTF(values[i].c_str());
        if (!value) return nullptr;
        env->SetObjectArrayElement(result, i, value);
        env->DeleteLocalRef(value);
        if (env->ExceptionCheck()) return nullptr;
    }
    return result;
}

load(
    "//third_party/java/arcs/build_defs:build_defs.bzl",
    "arcs_kt_android_library",
    "arcs_kt_android_test_suite",
    "arcs_kt_gen",
)

licenses(["notice"])

arcs_kt_gen(
    name = "ttl_generation",
    srcs = ["ttl.arcs"],
)

arcs_kt_android_library(
    name = "ttl_particles",
    testonly = 1,
    srcs = [
        "Reader.kt",
        "Writer.kt",
    ],
    deps = [
        ":ttl_generation",
        "//java/arcs/core/entity",
        "//third_party/java/truth:truth-android",
        "//third_party/kotlin/kotlinx_coroutines",
    ],
)

arcs_kt_android_test_suite(
    name = "ttl-tests",
    size = "medium",
    srcs = glob(["*Test.kt"]),
    manifest = "//java/arcs/android/common:AndroidManifest.xml",
    package = "arcs.android.integration.ttl",
    deps = [
        ":ttl_generation",
        ":ttl_particles",
        "//java/arcs/core/allocator",
        "//java/arcs/core/host",
        "//java/arcs/core/testutil",
        "//java/arcs/core/util/testutil",
        "//javatests/arcs/android/integration",
        "//third_party/android/androidx_test/ext/junit",
        "//third_party/java/junit:junit-android",
        "//third_party/java/robolectric:annotations",
        "//third_party/java/truth:truth-android",
        "//third_party/kotlin/kotlinx_coroutines",
    ],
)

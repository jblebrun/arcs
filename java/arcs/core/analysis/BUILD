load(
    "//third_party/java/arcs/build_defs:build_defs.bzl",
    "arcs_kt_library",
)

licenses(["notice"])

package(default_visibility = ["//java/arcs:allowed-packages"])

arcs_kt_library(
    name = "analysis",
    srcs = glob(
        ["*.kt"],
    ),
    deps = [
        "//java/arcs/core/data",
        "//java/arcs/core/data:schema_fields",
        "//java/arcs/core/data/expression",
        "//java/arcs/core/policy",
        "//java/arcs/core/type",
        "//java/arcs/core/util",
    ],
)

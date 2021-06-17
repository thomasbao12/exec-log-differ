load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "ExecLogParser",
    srcs = glob(["src/main/java/com/airbnb/execlog_parser/*.kt"]),
    deps = [
        "@com_github_bazelbuild_bazel//src/main/protobuf:spawn_java_proto",
    ]
)

java_binary(
    name = "ExecLogDiffer",
    main_class = "com.airbnb.execlog_parser.ExecLogParser",
    runtime_deps = [":ExecLogParser"],
)


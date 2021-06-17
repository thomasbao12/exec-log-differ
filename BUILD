load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "ExecLogParser",
    srcs = glob(["src/main/java/com/airbnb/execlog_parser/*.kt"]),
)

java_binary(
    name = "executable",
    main_class = "com.airbnb.execlog_parser.ExecLogParser",
    runtime_deps = [":ExecLogParser"],
)


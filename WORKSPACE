load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

rules_kotlin_version = "v1.5.0-alpha-2"
rules_kotlin_sha = "6194a864280e1989b6d8118a4aee03bb50edeeae4076e5bc30eef8a98dcd4f07"
http_archive(
    name = "io_bazel_rules_kotlin",
    url = "https://github.com/bazelbuild/rules_kotlin/releases/download/{}/rules_kotlin_release.tgz".format(rules_kotlin_version),
    sha256 = rules_kotlin_sha,
)

load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kotlin_repositories", "kt_register_toolchains")
kotlin_repositories()
kt_register_toolchains()

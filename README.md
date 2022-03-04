# execlog-parser

## Build

`bazel build //:ExecLogParser --incompatible_java_common_parameters=false`

Note you may or may not need `--incompatible_java_common_parameters=false`

## Run

After building:

`bazel-bin/ExecLogDiffer <exec log filepath #1> <exec log filepath #2>`

There are two logs here.

Run `bazel build :ExecLogDiffer` from repo root
Run `bazel-bin/ExecLogDiffer ./integration-test/exec1.log ./integration-test/exec2.log`

You should get some output, saying `tools/swift/swift-idl-parser/thrift_grammar.srcjar` has differing file
hashes

Now rerun `bazel-bin/ExecLogDiffer ./integration-test/exec1.log ./integration-test/exec2.log ./integration-test/allowlist`

No output and exit code should be 0

package com.airbnb.execlog_parser

import com.google.devtools.build.lib.exec.Protos.SpawnExec
import com.google.devtools.build.lib.exec.Protos.Digest
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.Paths
import kotlin.jvm.Throws

object ExecLogParser {
  lateinit var inputStream: InputStream

  @Throws(IOException::class)
  fun getNext(): SpawnExec? {
    if (inputStream.available() <= 0) {
      // end of file
      return null
    }
    return SpawnExec.parseDelimitedFrom(inputStream)
  }

  fun normalizePath(path: String): String {
    return path.replace(Regex("^bazel-out/.*?/"), "<bazel-out>/")
  }

  @Throws(IOException::class)
  fun getFileDigests(logPath: String, normalizePaths: Boolean): LinkedHashMap<String, Digest> {
    inputStream = FileInputStream(logPath)
    val fileHashMap = LinkedHashMap<String, Digest>()
    var spawnExec = getNext()
    while (spawnExec != null) {
      spawnExec.inputsList.union(spawnExec.actualOutputsList).forEach { fileProto ->
        val digest = fileProto.digest
        val hash = digest.hash
        val path = fileProto.path
        var key = path
        if (normalizePaths) {
          key = normalizePath(path)
        }
        if (fileHashMap.get(key) != null && fileHashMap.get(key)!!.hash != hash) {
          throw Exception(
            "File hash changed during bazel build.  Something is seriously wrong!\n" +
              "$key has at least two different hashes: ${fileHashMap.get(key)!!.hash} $hash\n"
          )
        }
        fileHashMap[key] = digest
      }
      spawnExec = getNext()
    }
    return fileHashMap
  }

  @JvmStatic fun main(arg: Array<String>) {
    val argsList = arg.toMutableList()

    var normalizePaths = false
    if (argsList.contains("--normalizePaths")) {
        normalizePaths = true
        argsList.remove("--normalizePaths")
    }

    if (argsList.size < 2) {
      println("This program takes in 2-3 arguments:")
      println("logPath1 logPath2 [allowlist of filepaths]")
      throw Exception("Invalid arguments")
    }

    // Resolve relative paths to the current working directory. This is a noop if the path is already
    // absolute.
    val logPath1 = Paths.get(argsList.get(0)).toAbsolutePath().toString()
    val logPath2 = Paths.get(argsList.get(1)).toAbsolutePath().toString()
    val allowListPath = argsList.getOrNull(2)?.let { Paths.get(it).toAbsolutePath().toString() }
    var allowList = setOf<String>()
    if (allowListPath != null) {
      allowList = File(allowListPath).readLines().map { it.trim() }.toSet()
    }
    val fileHashMap1 = getFileDigests(logPath1, normalizePaths)
    val fileHashMap2 = getFileDigests(logPath2, normalizePaths)

    if (fileHashMap1.keys != fileHashMap2.keys) {
      throw Exception(
        "Execution logs have different sets of inputs and outputs!\n" +
          "Please ensure that these are execution logs for the same bazel target\n" +
          "The first log has these additional inputs/output files: ${fileHashMap1.keys.subtract(fileHashMap2.keys)}\n" +
          "The second log has these additional inputs/output files: ${fileHashMap2.keys.subtract(fileHashMap1.keys)}\n"
      )
    }
    val inputDigestDiffs = LinkedHashMap<String, Pair<Digest, Digest>>()
    fileHashMap1.keys.forEach { path ->
      if (fileHashMap1[path]!!.hash != fileHashMap2[path]!!.hash) {
        if (allowList.any {
            path.endsWith(it)
          }) {
          return@forEach
        }
        inputDigestDiffs[path] = Pair(fileHashMap1[path]!!, fileHashMap2[path]!!)
      }
    }
    if (inputDigestDiffs.isNotEmpty()) {
      println("Execution logs have unexpected hash diffs, indicating the build is not deterministic")
      println("Consider adding the path to the allowlist if its impact is small.  Otherwise, please fix")
      println("input file digests:")
      inputDigestDiffs.forEach {
        println("${it.key}: ${it.value.first} ${it.value.second}")
      }
      throw Exception("Execution logs have different hashes for the same input")
    }
  }
}

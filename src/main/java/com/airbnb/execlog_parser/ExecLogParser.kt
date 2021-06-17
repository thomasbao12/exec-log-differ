package com.airbnb.execlog_parser

import com.google.devtools.build.lib.exec.Protos.SpawnExec
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
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

  @Throws(IOException::class)
  fun getFileHashes(logPath: String): Map<String,String> {
    inputStream = FileInputStream(logPath)
    val fileHashMap = mutableMapOf<String, String>()
    var spawnExec = getNext()
    while (spawnExec != null) {
      spawnExec.inputsList.union(spawnExec.actualOutputsList).forEach { fileProto ->
        val hash = fileProto.digest.hash
        val path = fileProto.path
        if (fileHashMap.get(path) != null && fileHashMap.get(path) != hash) {
          throw Exception(
            "File hash changed during bazel build.  Something is seriously wrong!\n" +
              "$path has at least two different hashes: ${fileHashMap[path]} $hash\n"
          )
        }
        fileHashMap[path] = hash
      }
      spawnExec = getNext()
    }
    return fileHashMap
  }

  @JvmStatic fun main(arg: Array<String>) {
    val logPath1 = "/tmp/exec-1.log"
    val logPath2 = "/tmp/exec-2.log"
    val fileHashMap1 = getFileHashes(logPath1)
    val fileHashMap2 = getFileHashes(logPath2)
    if (fileHashMap1.keys != fileHashMap2.keys) {
      throw Exception(
        "Execution logs have different sets of inputs and outputs!\n" +
          "Please ensure that these are execution logs for the same bazel target\n" +
          "The first log has these additional inputs/output files: ${fileHashMap1.keys.subtract(fileHashMap2.keys)}\n" +
          "The second log has these additional inputs/output files: ${fileHashMap2.keys.subtract(fileHashMap1.keys)}\n"
      )
    }
    val inputHashDiffs = mutableMapOf<String, Pair<String, String>>()
    fileHashMap1.keys.forEach { path ->
      if (fileHashMap1[path] != fileHashMap2[path]) {
        inputHashDiffs[path] = Pair(fileHashMap1[path]!!, fileHashMap2[path]!!)
      }
    }
    if (inputHashDiffs.isNotEmpty()) {
      println("Execution logs have unexpected hash diffs, indicating the build is not deterministic")
      println("Consider adding the path to the allowlist if its impact is small.  Otherwise, please fix")
      println("input hash diffs:")
      inputHashDiffs.forEach {
        println("${it.key}: ${it.value.first} ${it.value.second}")
      }
      throw Exception("Execution logs have different hashes for the same input")
    }
  }
}
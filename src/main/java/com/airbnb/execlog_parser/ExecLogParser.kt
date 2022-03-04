package com.airbnb.execlog_parser

import com.google.devtools.build.lib.exec.Protos.SpawnExec
import com.google.devtools.build.lib.exec.Protos.Digest
import java.io.File
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
  fun getFileDigests(logPath: String): Map<String, Digest> {
    inputStream = FileInputStream(logPath)
    val fileHashMap = mutableMapOf<String, Digest>()
    var spawnExec = getNext()
    while (spawnExec != null) {
      spawnExec.inputsList.union(spawnExec.actualOutputsList).forEach { fileProto ->
        val digest = fileProto.digest
        val hash = digest.hash
        val path = fileProto.path
        if (fileHashMap.get(path) != null && fileHashMap.get(path)!!.hash != hash) {
          throw Exception(
            "File hash changed during bazel build.  Something is seriously wrong!\n" +
              "$path has at least two different hashes: ${fileHashMap[path]} $hash\n"
          )
        }
        fileHashMap[path] = digest
      }
      spawnExec = getNext()
    }
    return fileHashMap
  }

  @JvmStatic fun main(arg: Array<String>) {
    if (arg.size < 2) {
      println("This program takes in 2-3 arguments:")
      println("logPath1 logPath2 [allowlist of filepaths]")
      throw Exception("Invalid arguments")
    }
    val logPath1 = arg.get(0)
    val logPath2 = arg.get(1)
    val allowListPath = arg.getOrNull(2)
    var allowList = setOf<String>()
    if (allowListPath != null) {
      allowList = File(allowListPath).readLines().map { it.trim() }.toSet()
    }
    val fileHashMap1 = getFileDigests(logPath1)
    val fileHashMap2 = getFileDigests(logPath2)
    if (fileHashMap1.keys != fileHashMap2.keys) {
      throw Exception(
        "Execution logs have different sets of inputs and outputs!\n" +
          "Please ensure that these are execution logs for the same bazel target\n" +
          "The first log has these additional inputs/output files: ${fileHashMap1.keys.subtract(fileHashMap2.keys)}\n" +
          "The second log has these additional inputs/output files: ${fileHashMap2.keys.subtract(fileHashMap1.keys)}\n"
      )
    }
    val inputDigestDiffs = mutableMapOf<String, Pair<Digest, Digest>>()
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
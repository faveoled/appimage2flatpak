package app

import typings.node.bufferMod.global.Buffer
import typings.node.bufferMod.global.BufferEncoding
import typings.node.childProcessMod.ExecSyncOptionsWithStringEncoding
import typings.node.fsMod.MakeDirectoryOptions
import typings.node.processMod.global.process
import typings.node.childProcessMod as node_child_process
import typings.node.fsMod as node_fs
import typings.node.osMod as node_os
import typings.node.pathMod as node_path
import typings.node.utilMod as node_util
import typings.node.fsMod.RmOptions
import typings.node.childProcessMod.IOType

object Node {

  def readFile(filePath: String): String =
    val opts = BufferEncoding.utf8
    val read = node_fs.readFileSync(filePath, opts)
    read

  def writeFile(filePath: String, contents: String): Unit =
    node_fs.writeFileSync(filePath, contents)

  def rename(oldPath: String, newPath: String): Unit = 
    node_fs.renameSync(oldPath, newPath)

  def rm(path: String): Unit = {
    node_fs.rmSync(path)
  }

  def cp(filePath1: String, filePath2: String): Unit =
    node_fs.copyFileSync(filePath1, filePath2)

  def mv(oldFile: String, newFile: String): Unit = {
    node_fs.renameSync(oldFile, newFile)
  }

  def exists(filePath: String): Boolean = {
    node_fs.existsSync(filePath)
  }

  def makeDir(dirPath: String): Unit =
    try {
      if (!node_fs.existsSync(dirPath)) {
        val opts = node_fs.MakeDirectoryOptions()
        opts.recursive  = true
        node_fs.mkdirSync(dirPath, opts)
      }
    } catch {
      case e: Exception =>
        throw Exception(s"Couldn't create a new directory: ${dirPath}", e)
    }

  def makeTmpDir(): String =
    try {
      val tmpDir = node_fs.mkdtempSync(node_path.join(node_os.tmpdir(), "appimage2deb-"));
      tmpDir
    } catch {
      case e: Exception =>
        throw Exception(s"Couldn't create a new temporary directory", e)
    }

  def chmod(filePath: String, mode: Int): Unit =
    try {
      node_fs.chmodSync(filePath, mode.toDouble);
    } catch {
      case e: Exception =>
        throw Exception(s"Couldn't chmod ${filePath} on ${filePath}")
    }

  def filesInDir(dirPath: String): Array[String] =
    val res = node_fs.readdirSync(dirPath)
    Interop.toScalaArray(res)

  def filesInDirRecursive(dirPath: String): List[String] =
    val filesInDirectory = node_fs.readdirSync(dirPath);
    val a1 = 
      filesInDirectory
        .flatMap(file =>
          val nextPath = node_path.resolve(dirPath, file);
          val stats = node_fs.statSync(nextPath).getOrElse(scala.sys.error(s"Couldn't read directory ${nextPath}"))
          if (stats.isDirectory()) {
              filesInDirRecursive(nextPath);
          } else {
              List(nextPath);
          }
        )
        .toList
    a1

  def resolve(files: String*): String = {
    node_path.resolve(files: _*)
  }

  def getFileSize(file: String): Int = {
    val stats = node_fs.statSync(file).toOption
    if (!stats.isDefined) {
      throw Exception("Couldn't get file size: " + file)
    }
    stats.get.size.toInt
  }

  def desktopFileInDir(dirPath: String): Option[String] =
    filesInDir(dirPath)
      .filter(path => path.endsWith(".desktop"))
      .map(node_path.resolve(dirPath, _))
      .headOption

  def appIconInDir(dirPath: String): Option[String] =
    try {
      val linkVal = node_fs.readlinkSync(node_path.resolve(dirPath, ".DirIcon"))
      Some(node_path.resolve(dirPath, linkVal))
    } catch {
      case e: Exception =>
        filesInDir(dirPath)
          .filter(path => path.endsWith(".png") || path.endsWith(".svg"))
          .map(node_path.resolve(dirPath, _))
          .headOption
    }

  def runProcessGetOut(command: String, cwd: Option[String], details: String = ""): String = {
    val result = runProcess(command, cwd, details, false)
    result match {
      case s: String => 
        s
      case _: Unit =>
        throw Exception("Internal type error")
    }
  }

  def runProcessPrintOut(command: String, cwd: Option[String], details: String = ""): Unit = {
    val result = runProcess(command, cwd, details, true)
    result match {
      case s: String => 
        throw Exception("Internal type error")
      case u: Unit =>
        u
    }
  }

  private def runProcess(command: String, cwd: Option[String], details: String = "", inherit: Boolean): String | Unit = {
    val msg = if (details == "") then s"Running command `${command}`" else s"${details}"
    println(s"${msg}...")
    val options = ExecSyncOptionsWithStringEncoding(BufferEncoding.utf8)
    if (inherit) {
      options.stdio = IOType.inherit
    }
    cwd match {
      case Some(c) => 
        options.cwd = c
      case None => // ignore
    }
    val output = node_child_process.execSync(command, options)
    if (inherit) {
      return
    } else {
      println(s"Done: ${details}")
      output
    }
  }


}

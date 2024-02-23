package app

import typings.node.processMod.global.process
import typings.node.childProcessMod as node_child_process
import typings.node.utilMod as node_util
import typings.node.fsMod as node_fs
import typings.node.osMod as node_os
import typings.node.pathMod as node_path
import typings.node.bufferMod.global.Buffer
import typings.node.bufferMod.global.BufferEncoding
import typings.node.childProcessMod.ExecSyncOptionsWithStringEncoding
import typings.node.fsMod.MakeDirectoryOptions

import StringExtensions.substringAfterLast
import StringExtensions.substringAfterFirst
import StringExtensions.substringBeforeFirst
import StringExtensions.substringBeforeLast

import scala.util.{Try, Success, Failure}

import org.rogach.scallop._
import typings.std.stdBooleans.`true`
import typings.std.stdStrings.click
import app.Node.runProcess

class Conf(arguments: Seq[String]) extends ScallopConf(arguments) {

  banner("""Usage: appimage2flatpak [OPTIONS]
           |appimage2flatpak is a CLI utility for converting AppImage files to Flatpak manifests
           |Options:
           |""".stripMargin)

  val appId = opt[String](
    name = "app-id",
    noshort = true,
    required = true,
    descr = "Specifies application id. Reversed domain name (e.g. io.github.user.Superapp).",
    validate = (arg => arg.length >= 1 && arg.contains("."))
  )

  val appName = opt[String](
    name = "app-name",
    noshort = true,
    required = false,
    descr = "Specifies application name. Usually matches an executable name.",
    validate = (_.length >= 1)
  )


  val appimageUrl = opt[String](
    name = "appimage-url",
    noshort = true,
    required = true,
    descr = "Specifies appimage URL for download.",
    validate = (_.length >= 1)
  )


  val repoArchive = opt[String](
    name = "repo-archive",
    noshort = true,
    required = false,
    descr = s"Specifies a URL where to get raw template archive. Default: ${Defaults.REPO_ARCHIVE}",
    validate = (_.length >= 1),
    default = Some(Defaults.REPO_ARCHIVE),
  )

  val runtime = opt[String](
    name = "runtime",
    noshort = true,
    required = false,
    descr = s"Specifies a Flatpak runtime. Default: ${Defaults.RUNTIME}",
    validate = (_.length >= 1),
    default = Some(Defaults.RUNTIME),
  )

  val runtimeVersion = opt[String](
    name = "runtime-version",
    noshort = true,
    required = false,
    descr = s"Specifies a Flatpak runtime version. Default: ${Defaults.RUNTIME_VERSION}",
    validate = (a => a.length >= 1 && !a.contains("'")&& !a.contains("\"")),
    default = Some(Defaults.RUNTIME_VERSION),
  )

  val sdk = opt[String](
    name = "sdk",
    noshort = true,
    required = false,
    descr = s"Specifies a Flatpak SDK. Default: ${Defaults.SDK}",
    validate = (_.length >= 1),
    default = Some(Defaults.SDK),
  )

  val debug = opt[Boolean](
    name = "debug",
    noshort = true,
    descr = "Enables debugging mode if specified. Enables detailed stacktraces for errors."
  )
  verify()
}

object App {

  def main(unused: Array[String]): Unit =
    val dropCountNode = process.env.get("APPIMAGE2FLATPAK_DROP_COUNT")
    val dropCount = dropCountNode.flatMap(undefOr => undefOr.toOption).getOrElse("2")
    val args = Interop.toScalaArray(process.argv.drop(dropCount.toInt))
    val cliConf = new Conf(args)
    try {
      
      Node.makeDir(cliConf.appId())

      val appimageFile = cliConf.appimageUrl().substringAfterLast("/")
      if (!Node.exists(appimageFile)) {
          runProcess(
            s"wget ${cliConf.appimageUrl()}",
            Option.empty,
            s"Downloading appimage from URL: ${cliConf.appimageUrl()}"
          )
      }

      val fileSize = Node.getFileSize(appimageFile)
      val sha256Sum: String = 
        runProcess(
          s"sha256sum ${appimageFile}", Option.empty, "calculating sha256 for AppImage"
        ).substringBeforeFirst(" ")
      if (sha256Sum.length() != 64) {
        println("WARN: failed to calculate AppImage's sha256 sum")
      }

      val archiveFilename = cliConf.repoArchive().substringAfterLast("/")

      Node.runProcess(
        s"wget -O ${archiveFilename} ${cliConf.repoArchive()}",
        Some(cliConf.appId()),
        "Getting repository archive"
      )

      Node.runProcess(
        s"tar --strip-components=1 -xvf ${archiveFilename}",
        Some(cliConf.appId()),
        "Unpacking repo archive"
      )

      Node.runProcess(
        s"rm ${archiveFilename}",
        Some(cliConf.appId()),
        "Remove source archive"
      )

      val appName = cliConf.appName.toOption.getOrElse(cliConf.appId().substringAfterLast("."))
      val replacements = Map(
        "TMPL_APP_ID" -> cliConf.appId(),
        "TMPL_APP_NAME" -> appName,
        "TMPL_APPIMAGE_URL" -> cliConf.appimageUrl(),
        "TMPL_APPIMAGE_SIZE" -> fileSize.toString,
        "TMPL_APPIMAGE_SHA256" -> sha256Sum,
        "TMPL_RUNTIME" -> cliConf.runtime(),
        "TMPL_RUNTIME_VERSION" -> cliConf.runtimeVersion(),
        "TMPL_SDK" -> cliConf.sdk()
      )

      val allFiles = Node.filesInDirRecursive(cliConf.appId())
      println(s"All files: ${allFiles.length}")
      val toBeProcessed = allFiles
        .filter(file => Defaults.PROCESSABLE_EXTENSIONS.contains(file.substringAfterLast(".")))
      println(s"To be: ${toBeProcessed.length}")

      for (filePath <- toBeProcessed) {
        println(s"Processing: ${filePath}")

        val withReplacedContent = Template.replaceAllOccurrences(replacements, Node.readFile(filePath))
        Node.writeFile(filePath, withReplacedContent)
        val fileName = filePath.substringAfterLast("/")
        if (fileName.contains("org.app.name")) {
          val newFileName = fileName.replace("org.app.name", cliConf.appId())
          val newFilePath = Node.resolve(filePath.substringBeforeLast("/"), newFileName)
          println(s"Renaming $filePath to $newFilePath")
          Node.mv(filePath, newFilePath)
        }
      }

      val unpackDir = s"${cliConf.appId()}-unpacked"
      Node.makeDir(unpackDir)
      val offset = findOffset(appimageFile)
      extractAppImage(offset, unpackDir, appimageFile)


      val iconFilePath = Node.appIconInDir(unpackDir)
      val desktopFilePath = Node.desktopFileInDir(unpackDir)

      iconFilePath.foreach(iconFilePathE => {
        Node.makeDir(Node.resolve(cliConf.appId(), "icons"))
        Node.cp(
          iconFilePathE, 
          Node.resolve(cliConf.appId(), "icons", appName + "." + iconFilePathE.substringAfterLast("."))
        )
      })

      desktopFilePath.foreach(desktopFilePathE => {
        val desktopContents = Node.readFile(desktopFilePathE)
        val processedDesktopFile = DebFiles.processExistingDesktop(desktopContents, cliConf.appId(), appName)
        Node.writeFile(Node.resolve(cliConf.appId(), cliConf.appId() + ".desktop"), processedDesktopFile) 
      })


      println(s"DONE: ${cliConf.appId()} directory is created")
      println("Compile using:")
      println(s"cd ${cliConf.appId()}")
      println(s"flatpak run org.flatpak.Builder --force-clean --sandbox --user --install --ccache builddir ${cliConf.appId()}.yaml")
      return ()
    } catch {
      case e: Exception =>
        if (cliConf.debug()) {
          Console.err.println("Execution failed. Error details:")
          e.printStackTrace(Console.err)
        } else {
          Console.err.println(s"Execution failed. Error details: ${e.getMessage()}")
        }
        return ()
    }

  def findOffset(filePath: String): Int =
    val output = Node.runProcess(s"readelf -h ${filePath}", Option.empty, "Reading source AppImage")
    val outputKeyVals =
      output.linesIterator
      .filter(line => line.contains(":"))
      .map(line =>
        val colonIdx = line.indexOf(':')
        (line.substring(0, colonIdx).trim, line.substring(colonIdx + 1).trim))
      .toMap
    val v1: String = outputKeyVals.getOrElse("Start of section headers",
                                     scala.sys.error("Start of section headers not found"))
    val v2: String = outputKeyVals.getOrElse("Size of section headers",
                                     scala.sys.error("Size of section headers not found"))
    val v3: String = outputKeyVals.getOrElse("Number of section headers",
                                     scala.sys.error("Number of section headers not found"))
    val resultNums =
      Seq(v1, v2, v3)
      .map(str => firstNumInString(str).getOrElse(scala.sys.error(s"Couldn't get int in: ${str}")))
    resultNums.head + (resultNums(1) * resultNums(2))

  def firstNumInString(input: String): Option[Int] =
    val numPattern = """\d+(\.\d*)?|\.\d+""".r
    numPattern.findFirstIn(input).flatMap(_.toIntOption)


  def extractAppImage(offset: Int, destDirPath: String, appImagePath: String): Unit =
    Node.runProcess(s"unsquashfs -force -offset ${offset} -dest ${destDirPath} ${appImagePath}", Option.empty, "Extracting source AppImage")

}

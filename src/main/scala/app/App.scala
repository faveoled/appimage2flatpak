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
    descr = "Specifies application id. Reversed domain name (e.g. io.github.user.superapp).",
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

  val repoSrc = opt[String](
    name = "repo-src",
    noshort = true,
    required = false,
    descr = "Specifies a URL where to get raw template files. Has a default value.",
    validate = (_.length >= 1),
    default = Some("https://raw.githubusercontent.com/faveoled/AppImage-Flatpak-Template/master"),
  )

  val runtime = opt[String](
    name = "runtime",
    noshort = true,
    required = false,
    descr = "Specifies a Flatpak runtime. 'org.freedesktop.Platform' by default.",
    validate = (_.length >= 1),
    default = Some("org.freedesktop.Platform"),
  )

  val runtimeVersion = opt[String](
    name = "runtime-version",
    noshort = true,
    required = false,
    descr = "Specifies a Flatpak runtime version. Default: 23.08.",
    validate = (a => a.length >= 1 && !a.contains("'")&& !a.contains("\"")),
    default = Some("23.08"),
  )

  val sdk = opt[String](
    name = "sdk",
    noshort = true,
    required = false,
    descr = "Specifies a Flatpak SDK. Default: org.freedesktop.Sdk.",
    validate = (_.length >= 1),
    default = Some("org.freedesktop.Sdk"),
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

      val appdataTemplateName = s"org.app.name.appdata.xml"
      val desktopTemplateName = s"org.app.name.desktop"
      val builderTemplateName = s"org.app.name.yml"
      val applyExtraName = "apply_extra.sh"

      Node.runProcess(
        s"wget ${cliConf.repoSrc()}/${appdataTemplateName}",
        Some(cliConf.appId()),
        "Getting appdata template"
      )

      Node.runProcess(
        s"wget ${cliConf.repoSrc()}/${desktopTemplateName}",
        Some(cliConf.appId()),
        "Getting desktop file template"
      )

      Node.runProcess(
        s"wget ${cliConf.repoSrc()}/${builderTemplateName}",
        Some(cliConf.appId()),
        "Getting builder template"
      )

      Node.runProcess(
        s"wget ${cliConf.repoSrc()}/${applyExtraName}",
        Some(cliConf.appId()),
        "Getting apply_extra.sh"
      )

      val appdataFile = Node.readFile(Node.resolve(cliConf.appId(), appdataTemplateName))
      val desktopFile = Node.readFile(Node.resolve(cliConf.appId(), desktopTemplateName))
      val builderFile = Node.readFile(Node.resolve(cliConf.appId(), builderTemplateName))
      val applyExtraFile = Node.readFile(Node.resolve(cliConf.appId(), applyExtraName))
      
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

      val newAppdataFile = Tempate.replaceAllOccurrences(replacements, appdataFile)
      val newDesktopFile = Tempate.replaceAllOccurrences(replacements, desktopFile)
      val newBuilderFile = Tempate.replaceAllOccurrences(replacements, builderFile)
      val newApplyExtraFile = Tempate.replaceAllOccurrences(replacements, applyExtraFile)

      val appdataFileName = s"${cliConf.appId()}.appdata.xml"
      val desktopFileName = s"${cliConf.appId()}.desktop"
      val builderFileName = s"${cliConf.appId()}.yml"
      val applyExtraFileName = "apply_extra.sh"

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


      Node.writeFile(Node.resolve(cliConf.appId(), appdataFileName), newAppdataFile)
      if (!desktopFilePath.isDefined) {
        Node.writeFile(Node.resolve(cliConf.appId(), desktopFileName), newDesktopFile)
      }
      Node.writeFile(Node.resolve(cliConf.appId(), builderFileName), newBuilderFile)
      Node.writeFile(Node.resolve(cliConf.appId(), applyExtraFileName), newApplyExtraFile)

      Node.rm(Node.resolve(cliConf.appId(), appdataTemplateName))
      Node.rm(Node.resolve(cliConf.appId(), desktopTemplateName))
      Node.rm(Node.resolve(cliConf.appId(), builderTemplateName))

      println(s"DONE: ${cliConf.appId()} directory is created")
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

package app

import java.util.Locale

import StringExtensions.substringAfterLast
import StringExtensions.substringAfterFirst
import StringExtensions.substringBeforeFirst

object DebFiles {

  def processExistingDesktop(desktopFileContents: String, appId: String, appName: String): String =
    val v1 = 
      desktopFileContents.linesIterator
        .map(line =>
          val eqSignIdx = line.indexOf("=")
          if (eqSignIdx != -1) {
            val beforeSign = line.substringBeforeFirst("=").trim()
            if (beforeSign.equalsIgnoreCase("Exec")) {
              s"Exec=${appName}"
            } else if (beforeSign.equalsIgnoreCase("Icon")) {
              s"Icon=${appId}"
            } else {
              line
            }
          } else {
            line
          }
        ).mkString("\n")
    v1

}

package app

object Tempate {
  
  def replaceAllOccurrences(keyToReplacementMap: Map[String, String], inputStr: String): String = {
    val regexPattern = keyToReplacementMap.keys.mkString("|")
    val pattern = regexPattern.r
    pattern.replaceAllIn(inputStr, (matchObj) => keyToReplacementMap(matchObj.matched))
  }

}

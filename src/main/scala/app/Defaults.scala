package app

object Defaults {

  val REPO_ARCHIVE = "https://github.com/faveoled/AppImage-Flatpak-Template/archive/refs/heads/foss.tar.gz"
  val RUNTIME = "org.freedesktop.Platform"
  val RUNTIME_VERSION = "23.08"
  val SDK = "org.freedesktop.Sdk"
  
  val PROCESSABLE_EXTENSIONS: Set[String] = Set("xml", "json", "yaml", "yml", "sh", ".desktop")

}

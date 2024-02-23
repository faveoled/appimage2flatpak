package app

import utest._

object TutorialTest extends TestSuite {

  def tests = Tests {
    test("HelloWorld") {
      assert(1 == 1)
    }


    test("Template processing") {
      val actual = Template.replaceAllOccurrences(Map(
        "TMPL_ID" -> "1",
        "TMPL_NAME" -> "2"
      ), "0 TMPL_ID TMPL_NAME 3")
      assert(actual == "0 1 2 3")
    }

    test("Files in dir") {
      val actual = Node.filesInDirRecursive("/home/user/Dev/Scala/appimage2flatpak/src/test/scala/app").toList
      assert(actual == List("/home/user/Dev/Scala/appimage2flatpak/src/test/scala/app/AppTest.scala"))
    }

    test("mv test") {
      Node.writeFile("test.txt", "content")
      Node.mv("test.txt", "test2.txt")
      val exists = Node.exists("test2.txt")
      try {
        Node.rm("test2.txt")
      } catch {
        case e: Throwable => // ignore
      }
      assert(exists)
    }

  }
}

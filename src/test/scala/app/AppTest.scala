package app

import utest._

object TutorialTest extends TestSuite {

  def tests = Tests {
    test("HelloWorld") {
      assert(1 == 1)
    }


    test("Template processing") {
      val actual = Tempate.replaceAllOccurrences(Map(
        "TMPL_ID" -> "1",
        "TMPL_NAME" -> "2"
      ), "0 TMPL_ID TMPL_NAME 3")
      assert(actual == "0 1 2 3")
    }

  }
}

package mykafkamonitor

import org.scalatest.{FunSuite, Matchers}

class FormatterUtilsTest extends FunSuite with Matchers {

  import FormatterUtils._

  test("should pad the string with spaces") {
    pad20("").length shouldEqual 20
    pad20("hjskdgf").length shouldEqual 20
    pad20("73854738547385473854").length shouldEqual 20
  }

  test("should throw exception for extra long input") {
    intercept[IllegalArgumentException] {
      pad20("738547385473854738541").length shouldEqual 20
    }
  }
}

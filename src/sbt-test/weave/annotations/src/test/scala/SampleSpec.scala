package sample

import org.scalatest._

class SampleSpec extends FlatSpec with Matchers {
  "A test" should "be passed" in {
    assert(getClass().getResource("/main-res.txt") != null)
    assert(getClass().getResource("/test-res.txt") != null)
    assert(Sample.add(2,2) === 5)
    assert(addTest(2,2) === 3)
  }

  def addTest(a: Int, b: Int) = a + b
}

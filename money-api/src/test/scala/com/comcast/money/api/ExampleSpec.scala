package com.comcast.money.api

import java.util.concurrent.Executors

import org.scalatest.{Matchers, WordSpec}

class ExampleSpec extends WordSpec with Matchers {

  val executors = Executors.newFixedThreadPool(10)

  "The prototype core" should {
    "work" in {
      val handler = new DefaultSpanHandler()
      val monitor = new DefaultSpanMonitor(handler)
      val tracer = new DefaultTracer(handler, monitor)

      val iWillTimeout = tracer.newSpan("timeMeOut")

      for(i <- 1 to 2) {
        executors.submit(
          new Runnable {
            override def run(): Unit = {
              val fooSpan = tracer.newSpan("foo")
              fooSpan.record("iam", "foo")

              for (j <- 1 to 2) {
                val barSpan = tracer.newSpan("bar")

                barSpan.record("iam", "bar")
                barSpan.stop(true)
              }

              fooSpan.stop(true)
            }
          }
        )
      }


      Thread.sleep(2000)
    }
  }
}

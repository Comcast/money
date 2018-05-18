

import scala.util.{Failure, Success}

val moneyTraceHeader = "X-MoneyTrace: trace-id=de305d54-75b4-431b-adb2-eb6b9e546013;parent-id=3285573610483682037;span-id=3285573610483682037"
val x = Option(moneyTraceHeader)


if(!x.isEmpty) "hello"

val y = Option(null)
if(  y.isEmpty) y

Seq[Option[String]](
  Option("aaa"),
  Option(null),
  Option("bbb")
).flatten


val e = new { var id = 5; var name = "Prashant" }
e.id

val maybeMoney=y

val incomingTraceId = maybeMoney map { incTrcaceId =>
  // attempt to parse the incoming trace id (its a Try)
//  fromHttpHeader(incTrcaceId) match {
//    case Success(spanId) => SpanLocal.push(factory.newSpan(spanId, "servlet"))
//    case Failure(ex) => println("Unable to parse money trace for request header '{}'", incTrcaceId)
//  }
  incTrcaceId
}

val a = Option("aaa")
val b = None
val c = Option("ccc")

def test(s1: Option[String],s2: Option[String]) = (s1,s2) match {
  case (Some(x1),Some(x2)) if(s2.nonEmpty) => print("a ")
  case (Some(s),_) => print("b ")
  case _ => print("c ")
}

test(a,c)
b.getOrElse("xxx")



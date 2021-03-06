package examples.asciiweb.feed

import java.awt.image.BufferedImage

import akka.actor._
import akka.stream._
import akka.stream.scaladsl._
import com.jsuereth.image.Ascii2._
import com.jsuereth.image.Resizer
import examples.asciiweb.Settings

import scala.language.postfixOps

object AsciiImageProducer extends App {
  implicit val actorSystem = ActorSystem("StreamPublisher")
  implicit val materializer = ActorMaterializer()

  val settings = Settings(actorSystem)

  def resize(img: BufferedImage): BufferedImage = Resizer.forcedScale(img, 80, 60)

  var lastTime = System.currentTimeMillis

  def limitFramerate[T](t: T): Boolean = {
    val currentTime = System.currentTimeMillis
    if (currentTime - lastTime > 32) {
      lastTime = currentTime
      true
    } else false
  }

  val source = Source(com.jsuereth.video.WebCam.default(actorSystem))

  source
    .filter(limitFramerate)
    .map(_.image)
    .map(resize)
    .map(correctFormat)
    .map(asciify)
    .map(toJSON)
    .map(compress)
    .map(toBase64)
    .to(Kafka.kafkaSink(settings.kafka.kafkaProducerSettings))
    .run()
}

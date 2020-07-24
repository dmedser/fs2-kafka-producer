package app

import cats.effect._
import cats.syntax.flatMap._
import cats.syntax.functor._
import fs2.kafka._

import scala.io.StdIn

object Application extends IOApp {

  private val hostname = "127.0.0.1"
  private val port = 9092
  private val topic = "test-topic"

  type K = String
  type V = String
  type P = Unit

  type Producer[F[_]] = KafkaProducer[F, K, V]

  def run(args: List[String]): IO[ExitCode] =
    kafkaProducerResource[IO].use { loop(_) as ExitCode.Success }

  private def kafkaProducerResource[F[_] : ConcurrentEffect : ContextShift]: Resource[F, Producer[F]] =
    producerResource(ProducerSettings[F, K, V].withBootstrapServers(s"$hostname:$port"))

  private def loop[F[_] : Sync](producer: Producer[F]): F[Unit] =
    nextRecord.flatMap(producer.produce(_).flatten.void) >> loop(producer)

  private def nextRecord[F[_]](implicit F: Sync[F]): F[ProducerRecords[K, V, P]] =
    for {
      key   <- F.delay { print("Enter key: "); StdIn.readLine() }
      value <- F.delay { print("Enter value: "); StdIn.readLine() }
    } yield ProducerRecords.one(ProducerRecord(topic, key, value))
}

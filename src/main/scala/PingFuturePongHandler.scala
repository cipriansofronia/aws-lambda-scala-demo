import io.circe.generic.auto._
import io.github.mkotsur.aws.handler.Lambda._
import io.github.mkotsur.aws.handler.Lambda
import com.amazonaws.services.lambda.runtime.Context
import scala.concurrent.Future

case class Ping(inputMsg: String)

class PingFuturePongHandler extends Lambda[Ping, Future[Int]] {

  override def handle(ping: Ping, context: Context) =
    Right(Future.successful(ping.inputMsg.length))

}
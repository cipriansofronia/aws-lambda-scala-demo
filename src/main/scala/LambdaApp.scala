import com.amazonaws.services.dynamodbv2.{AmazonDynamoDB, AmazonDynamoDBAsyncClient}
import io.circe.generic.auto._
import io.github.mkotsur.aws.handler.Lambda._
import io.github.mkotsur.aws.handler.Lambda
import com.amazonaws.services.lambda.runtime.Context
import org.scanamo._
import org.scanamo.syntax._
import org.scanamo.auto._
import org.scanamo.joda.JodaFormats._
import Helpers._
import CostsHelper._
import com.redis.RedisClient
import io.github.mkotsur.aws.proxy.{ProxyRequest, ProxyResponse}
import org.joda.time.DateTime


case class In(calling: String, called: String, start: String, duration: Int)
sealed trait Res
case class ErrorOut(message: String) extends Res
case class Out(calling: String, startDate: DateTime, endDate: DateTime, called: String, cost: BigDecimal, duration: Int, price: BigDecimal, rounded: Int) extends Res

class LambdaApp extends Lambda[ProxyRequest[In], ProxyResponse[Res]] {

  private val redisEndpoint: String = Option(System.getenv("REDIS_URL")).getOrElse(throw new Exception("No REDIS_URL defined!"))
  private val tableName = "calls"
  private val contenttype = Some(Map("Content-Type" -> "application/json"))

  val redis = new RedisClient(redisEndpoint, 6379)
  val client: AmazonDynamoDB = AmazonDynamoDBAsyncClient.asyncBuilder().build()
  val scanamo = Scanamo(client)
  val table: Table[Out] = Table[Out](tableName)

  def computeCosts(in: In) = {
    val start = DateTime.parse(in.start)
    for {
      _ <- if (in.calling.exists(!_.isDigit)) None else Some(in)
      _ <- if (in.called.exists(!_.isDigit)) None else Some(in)
      _ <- if (in.duration < 0) None else Some(in)
      r <- bestRowFromRedis(redis, in.calling, start)
    } yield Out(
      calling = in.calling,
      startDate = start,
      endDate = start.plusSeconds(in.duration),
      called = in.called,
      cost = callCost(in, r),
      duration = in.duration,
      price = r.price,
      rounded = Math.round(r.price.toDouble).toInt
    )
  }

  override def handle(in: ProxyRequest[In], context: Context): Either[Throwable, ProxyResponse[Res]] =
    try {
      computeCosts(in.body.getOrElse(throw new Exception("body is required"))) match {
        case None =>
          Right(ProxyResponse(400, contenttype, Some(ErrorOut("Incorrect input"))))
        case Some(v) => Right {
          scanamo.exec(table.putAll(Set(v)))
          ProxyResponse(200, contenttype, Some(v))
        }
      }
    } catch {
      case t: Throwable =>
        Right(ProxyResponse(500, contenttype, Some(ErrorOut(t.getMessage))))
    }

}
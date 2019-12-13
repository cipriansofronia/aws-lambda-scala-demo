import com.amazonaws.services.dynamodbv2.{AmazonDynamoDBAsync, AmazonDynamoDBAsyncClient}
import io.circe.generic.auto._
import io.github.mkotsur.aws.handler.Lambda._
import io.github.mkotsur.aws.handler.Lambda
import com.amazonaws.services.lambda.runtime.Context
import org.scanamo._
import org.scanamo.syntax._
import org.scanamo.auto._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import Helpers._
import CostsHelper._
import com.redis.RedisClient
import io.github.mkotsur.aws.proxy.{ProxyRequest, ProxyResponse}
import org.joda.time.DateTime


case class In(calling: String, called: String, start: String, duration: Int)
sealed trait Res
case class ErrorOut(message: String) extends Res
case class Out(calling: String, start: String, called: String, cost: BigDecimal, duration: Int, price: BigDecimal, rounded: Int) extends Res

class LambdaApp extends Lambda[ProxyRequest[In], Future[ProxyResponse[Res]]] {

  private val redisEndpoint: String = Option(System.getenv("REDIS_URL")).getOrElse(throw new Exception("No REDIS_URL defined!"))
  private val _1: String = Option(System.getenv("AWS_ACCESS_KEY_ID")).getOrElse(throw new  Throwable("No AWS_ACCESS_KEY_ID defined!"))
  private val _2: String = Option(System.getenv("AWS_SECRET_KEY")).getOrElse(throw new  Throwable("No AWS_SECRET_KEY defined!"))
  private val tableName = "calls"
  private val `Content-Type` = Some(Map("Content-Type" -> "application/json"))

  val redis = new RedisClient(redisEndpoint, 6379)
  val client: AmazonDynamoDBAsync = AmazonDynamoDBAsyncClient.asyncBuilder().build()
  val scanamo = ScanamoAsync(client)
  val table: Table[Out] = Table[Out](tableName)

  def computeCosts(in: In): Future[Option[Out]] = Future {
    bestRowFromRedis(redis, in.calling, DateTime.parse(in.start)).map { r =>
      Out(
        calling = in.calling,
        start = in.start,
        called = in.called,
        cost = callCost(in, r),
        duration = in.duration,
        price = r.price,
        rounded = Math.round(r.price.toDouble).toInt
      )
    }
  }

  def app(in: In): Future[ProxyResponse[Res]] =
    computeCosts(in) flatMap {
      case None =>
        Future.successful(ProxyResponse(400, `Content-Type`, Some(ErrorOut("Incorrect input"))))
      case Some(v) =>
        scanamo.exec(table.putAll(Set(v)))
          .map(_ => ProxyResponse(200, `Content-Type`, Some(v)))
    }

  override def handle(in: ProxyRequest[In], context: Context) =
    Right(app(in.body.getOrElse(throw new Exception("body is required"))))

}
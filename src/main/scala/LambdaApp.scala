import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
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
//import org.scanamo.generic.auto._


case class In(calling: String, called: String, start: String, duration: String)
case class CallsTable(calling: String, start: String, called: String, cost: String, duration: String, price: String, rounded: String)

class LambdaApp extends Lambda[In, Future[CallsTable]] {

  private val tableName = "calls"
  private val endpoint: String = Option(System.getenv("DYNAMO_URL")).getOrElse(new Throwable("No DYNAMO_URL defined!"))
  private val redisEndpoint: String = Option(System.getenv("REDIS_URL")).getOrElse(new Throwable("No REDIS_URL defined!"))
  private val _1: String = Option(System.getenv("AWS_ACCESS_KEY_ID")).getOrElse(new Throwable("No AWS_ACCESS_KEY_ID defined!"))
  private val _2: String = Option(System.getenv("AWS_SECRET_KEY")).getOrElse(new Throwable("No AWS_SECRET_KEY defined!"))

  val client: AmazonDynamoDBAsync =
      AmazonDynamoDBAsyncClient
        .asyncBuilder()
        .withEndpointConfiguration(new EndpointConfiguration(endpoint, None.orNull))
        .build()

  val scanamo = ScanamoAsync(client)

  val table: Table[CallsTable] = Table[CallsTable](tableName)

  def inToCalls(in: In) = CallsTable(in.called, in.start, in.called, "2.368", in.duration, "0.4", "355")

  def app(in: In): Future[CallsTable] =
    for {
      v <- Future(inToCalls(in))
      _ <- scanamo.exec(table.putAll(Set(v)))
    } yield v

  override def handle(in: In, context: Context) =
    Right(app(in))

}
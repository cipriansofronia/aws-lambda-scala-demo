import com.redis.RedisClient
import org.joda.time.DateTime
import com.github.tototoshi.csv._

case class Request(number: String, time: String)

case class Response(prefix: String, price: String, from: String, initial: String, increment: String)

case class LambdaResponse(statusCode: Int, body: String)

case class Rule(prefix: String, price: Int, initial: Int, increment: Int, startDate: DateTime)

object Helpers {

  private def matchString(redis: RedisClient, phoneNumber: String): Seq[Rule] = {
    val combos = (phoneNumber.size to 0 by -1).map(phoneNumber.take(_).toString)
    def matchRedis(lst: Seq[String]): Seq[Rule] = {
      if (lst.isEmpty) Nil
      else {
        redis.lrange(lst.head, 0, 1) match {
          case None | Some(Nil) => matchRedis(lst.tail)
          case Some(r) => r.collect {
            case Some(x) =>
              val all = CSVParser.parse(x, '\\', ',', '"').get
              Rule(all(0), all(3).toInt, all(4).toInt, all(5).toInt, DateTime.parse(all(6)))
          }
        }
      }
    }
    matchRedis(combos)
  }

  private implicit val ord = new Ordering[DateTime] {
    override def compare(x: DateTime, y: DateTime): Int =  x.compareTo(y)
  }

  private def bestRow(lst: Seq[Rule], time: DateTime): Rule = {
    lst.filter(r => r.startDate.compareTo(time) < 0).maxBy(_.startDate)
  }

  def bestRowFromRedis(redis: RedisClient, phoneNumber: String, time:DateTime) =
    bestRow(matchString(redis, phoneNumber), time)
}
import com.redis.RedisClient
import org.joda.time.DateTime
import com.github.tototoshi.csv._

case class Response(prefix: String, price: BigDecimal, from: String, initial: Int, increment: Int)

case class Rule(prefix: String, price: BigDecimal, initial: Int, increment: Int, startDate: DateTime)

object Helpers {

  def matchString(redis: RedisClient, phoneNumber: String): Seq[Rule] = {
    val combos = (phoneNumber.size to 0 by -1).map(phoneNumber.take(_).toString)

    def matchRedis(lst: Seq[String]): Seq[Rule] = {
      if (lst.isEmpty) Nil
      else {
        redis.lrange(lst.head, 0, 1) match {
          case None | Some(Nil) => matchRedis(lst.tail)
          case Some(r) => r.collect {
            case Some(x) =>
              val all = CSVParser.parse(x, '\\', ',', '"').get
              Rule(all.head, BigDecimal(all(3)), all(4).toInt, all(5).toInt, DateTime.parse(all(6)))
          }
        }
      }
    }

    matchRedis(combos)
  }

  private implicit val ord = new Ordering[DateTime] {
    override def compare(x: DateTime, y: DateTime): Int = x.compareTo(y)
  }

  private def bestRow(lst: Seq[Rule], time: DateTime): Option[Rule] = {
    lst.filter(r => r.startDate.compareTo(time) < 0) match {
      case Nil => None
      case x :: Nil => Some(x)
      case x => Some(x.maxBy(_.startDate))
    }
  }

  def bestRowFromRedis(redis: RedisClient, phoneNumber: String, time: DateTime) =
    bestRow(matchString(redis, phoneNumber), time)

}


object CostsHelper {

  def effectiveDuration(in: In, r: Rule): Double =
    Math.ceil((r.increment + in.duration) / r.increment) * r.increment

  def callCost(in: In, r: Rule): BigDecimal = effectiveDuration(in, r) * (r.price / 60)

}
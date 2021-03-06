package app

import com.twitter.concurrent.AsyncMeter
import com.twitter.conversions.time._
import com.twitter.finagle.{ Failure, IndividualRequestTimeoutException, Service, SimpleFilter }
import com.twitter.finagle.http.{ Response, Request }
import com.twitter.finagle.service.TimeoutFilter
import com.twitter.util.{ Timer, Future, Throw, Duration }
import com.typesafe.config.ConfigFactory

/**
 * Utility values and functions.
 */
package object util {

  /**
   * Application configuration object.
   */
  final val config = ConfigFactory.load()

  /**
   * A simple filter tha adds timeouts to service calls
   */
  def timeoutFilter(duration: Duration = 250.millis)(implicit timer: Timer): TimeoutFilter[Request, Response] =
    new TimeoutFilter(duration, new IndividualRequestTimeoutException(duration), timer)

  /**
   * Rate limiter filter. Defaults to ~1000 req/sec.
   */
  def rateLimiter(size: Int = 10, duration: Duration = 1.millis, waiters: Int = 1000, permits: Int = 1)(implicit timer: Timer): SimpleFilter[Request, Response] = new SimpleFilter[Request, Response] {
    private val meter = AsyncMeter.newMeter(size, duration, waiters)
    def apply(request: Request, service: Service[Request, Response]) = meter.await(permits).transform {
      case Throw(noPermit) => noPermit match {
        case e: java.util.concurrent.RejectedExecutionException => Future.exception(Failure.rejected(noPermit))
        case e => Future.exception(e)
      }
      case _ => service(request)
    }
  }
}

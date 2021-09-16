package be.doeraene.services

import org.slf4j.{Logger, LoggerFactory}
import zio._

package object logging {

  type Logging = Has[Logging.Service]

  val log: HelperLogger = new HelperLogger

}

package uk.gov.homeoffice.mongo.casbah

import java.util.concurrent.TimeUnit
import com.mongodb.ServerAddress
import com.mongodb.casbah.MongoDB
import org.specs2.execute.{AsResult, Result}
import org.specs2.matcher.Scope
import grizzled.slf4j.Logging
import uk.gov.homeoffice.specs2.ComposableAround

/**
  * Mix in this trait (at example level) to provide a connection to an embedded Mongo for testing.
  * An embedded Mongo is started for each example.
  */
trait EmbeddedMongo extends Scope with ComposableAround with EmbeddedMongoExecutable with MongoClient with Logging {
  startMongo()

  def startMongo(): Unit = {
    def startMongo(attempt: Int, sleepTime: Int = 2): Unit = try {
      mongodExecutable.start()
      info(s"Started Mongo running on ${network.getPort}")
    } catch {
      case t: Throwable =>
        println(s"Failed to start Mongo on attempt number $attempt")
        val nextAttempt = attempt + 1

        if (nextAttempt <= 5) {
          TimeUnit.SECONDS.sleep(sleepTime)
          startMongo(nextAttempt, sleepTime + 1)
        } else {
          throw new Exception("Failed to start Mongo after 5 attempts", t)
        }
    }

    startMongo(1)
  }

  override def around[R: AsResult](r: => R): Result = {
    try {
      super.around(r)
    } finally {
      info(s"Stopping Mongo running on ${network.getPort}")
      mongodExecutable.stop()
    }
  }
}

trait MongoClient extends Mongo {
  self: EmbeddedMongo =>

  lazy val database = "embedded-database"

  lazy val mongoClient = com.mongodb.casbah.MongoClient(new ServerAddress(network.getServerAddress, network.getPort))

  lazy val db = mongoClient(database)

  trait TestMongo extends Mongo {
    lazy val db: MongoDB = self.db
  }
}
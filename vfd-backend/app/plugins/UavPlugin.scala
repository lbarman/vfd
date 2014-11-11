package plugins

import akka.actor.ActorRef
import akka.actor.Props
import play.api.Application
import play.api.Plugin
import play.api.libs.concurrent.Akka
import vfd.uav.MockConnection
import vfd.uav.SerialConnection

class UavPlugin(app: Application) extends Plugin {

  private lazy val config = app.configuration.getConfig("uav")

  lazy val systemId = config.flatMap(_.getInt("system_id")).getOrElse(1)

  private lazy val connection = {
    val conn = config.flatMap(_.getConfig("connection"))
    val tpe = conn.flatMap(_.getString("type")).getOrElse("mock")
    val heartbeat = conn.flatMap(_.getInt("heartbeat")).getOrElse(2000)
    val id = conn.flatMap(_.getInt("component_id")).getOrElse(99).toByte

    val props = tpe match {
      case "mock" =>
        MockConnection.apply

      case "serial" =>
        val serial = config.flatMap(_.getConfig("serial"))
        SerialConnection(
          id,
          heartbeat,
          serial.flatMap(_.getString("port")).getOrElse("/dev/ttyUSB0"),
          serial.flatMap(_.getInt("baud")).getOrElse(115200),
          serial.flatMap(_.getBoolean("two_stop_bits")).getOrElse(false),
          serial.flatMap(_.getInt("parity")).getOrElse(0))

      case unknown => throw new RuntimeException("Unsupported connection type '" + unknown + "'")
    }

    Akka.system(app).actorOf(props, name = "uav-connection")
  }

  def register(websocket: ActorRef): Props = Props(classOf[UavClientConnection], websocket, connection)

}
package vfd.uav

import java.util.concurrent.TimeUnit.MILLISECONDS
import scala.concurrent.duration.FiniteDuration
import scala.util.Random
import org.mavlink.Packet
import org.mavlink.enums.MavAutopilot
import org.mavlink.enums.MavModeFlag
import org.mavlink.enums.MavState
import org.mavlink.enums.MavType
import org.mavlink.messages.Heartbeat
import Connection.Received
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Props
import akka.util.ByteString
import scala.concurrent.duration._
import org.mavlink.messages.Message
import vfd.uav.mock.RandomFlightPlan

class MockConnection(localSystemId: Byte, localComponentId: Byte, remoteSystemId: Byte) extends Actor with ActorLogging with Connection with MavlinkUtil {
  import Connection._
  import context._ 

  override val systemId = remoteSystemId
  override val componentId = remoteSystemId
  
  val plan = new RandomFlightPlan
  
  def scheduleMessage(delay: FiniteDuration)(fct: => Message) = system.scheduler.schedule(delay, delay){
    sendAll(Received(assemble(fct)))
  }
  def scheduleBytes(delay: FiniteDuration)(fct: => Array[Byte]) = system.scheduler.schedule(delay, delay){
    sendAll(Received(ByteString(fct)))
  }

  override def preStart() = {
    //increment state
    system.scheduler.schedule(0.01.seconds, 0.01.seconds){plan.tick(0.01)}
    
    //send messages
    scheduleMessage(0.05.seconds)(plan.position)
    scheduleMessage(0.05.seconds)(plan.attitude)
    scheduleMessage(2.seconds)(plan.heartbeat)
    
    //simulate noisy line
    scheduleBytes(0.3.seconds)(MockPackets.invalidCrc)
    scheduleBytes(1.5.seconds)(MockPackets.invalidOverflow)
  }

  def receive = registration

}

object MockConnection {
  def apply(systemId: Byte, componentId: Byte, remoteSystemId: Byte) = Props(classOf[MockConnection], systemId, componentId, remoteSystemId)
}

object MockPackets {
  val invalidCrc = Array(254, 1, 123, 13, 13).map(_.toByte)
  val invalidOverflow = {
    val data = Array.fill[Byte](Packet.MaxPayloadLength + 100)(42)
    data(0) = -2
    data(1) = 2
    data(1) = -1
    data
  }
}
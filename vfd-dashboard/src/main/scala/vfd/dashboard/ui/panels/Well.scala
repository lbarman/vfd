package vfd.dashboard.ui.panels

import org.mavlink.messages.Attitude
import org.scalajs.dom.HTMLElement

import rx.core.Obs
import scalatags.JsDom.all.Tag
import scalatags.JsDom.all.`class`
import scalatags.JsDom.all.div
import scalatags.JsDom.all.stringAttr
import scalatags.JsDom.all.stringFrag
import vfd.dashboard.MavlinkSocket

class Well(title: String, content: Tag, socket: MavlinkSocket) {

  var isCritical = false

  def mainClasses() =
    if (!isCritical)
      "panel panel-default well"
    else
      " well-critical"

  val wellStruct = div(`class` := mainClasses)(
    div(`class` := "well-header")(
      div(`class` := "well-title")(title)),
    div(`class` := "panel-body well-inner")(
      content))

  def render: HTMLElement = {

    Obs(socket.message, skipInitial = true) {
      socket.message() match {
        case Attitude(roll, pitch, yaw) =>
          this.isCritical = (roll < 0)
          println("Changing the value, critical is " + this.isCritical)
        case _ => ()
      }
    }
    
    wellStruct.render

  }
}
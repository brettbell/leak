package controllers

import java.util.concurrent.TimeUnit

import play.api._
import play.api.libs._
import play.api.libs.concurrent._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee._
import play.api.libs.iteratee.Concurrent._
import play.api.libs.json._
import play.api.mvc._

object Application extends Controller {

  val timestep = 100

  val statusMonitor = Monitor.getStatus

  val (statusEnumerator: Enumerator[String], statusChannel: Broadcaster) = Concurrent.broadcast[String](statusMonitor)

  def index = Action {
    Ok(views.html.index("Leak"))
  }

  def status = WebSocket.using[String] { request =>
    val in = Iteratee.foreach[String](x => { println(x) }).mapDone { _ => }
    var out = statusEnumerator
    (in, out)
  }

  object Monitor {
    var id: Long = 0
    val runtime = Runtime.getRuntime()
    val getStatus = Enumerator.generateM {
      Promise.timeout({
        id = id + 1                   
        val jsonstatus = Json.obj(
          "id" -> id,
          "total" -> runtime.totalMemory(),
          "free" -> runtime.freeMemory()
        )
        Some(jsonstatus.toString)
      }, timestep, TimeUnit.MILLISECONDS)
    }

  }

}
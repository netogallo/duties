package org.duties

//web
import org.scalatra._
import scalate.ScalateSupport
import org.fusesource.scalate.{ TemplateEngine, Binding }
import org.fusesource.scalate.layout.DefaultLayoutStrategy
import javax.servlet.http.HttpServletRequest
import collection.mutable

//json
import org.json4s.{NoTypeHints,Formats, MappingException}
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.{read, write}
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException

//data structures
import scala.collection.JavaConversions._

//mongo
import Models._
import com.mongodb.casbah.Imports._
import Mongo.Collections
import Mongo.Collections._

//crypto
import javax.crypto.IllegalBlockSizeException

import com.roundeights.hasher.Implicits._
import scala.language.postfixOps

import Auth._

trait DutyStack extends ScalatraServlet with ScalateSupport with MongoClient {
  implicit val formats = Serialization.formats(NoTypeHints)
  
  /* error response */
  case class Error(error: String)
  def mkError(msg: String): String = {
    contentType = "application/json"
    write(Error(msg))
  }

  def maybeAuth: Option[String] = cookies.get(Auth.COOKIE)
                                  .flatMap(Auth.decrypt)
                                  .map(_.username)

 /* auth */ 
  def requireAuth: String = try {
    def require = halt(403, mkError("Please login"))
    maybeAuth.getOrElse(require)   
  } catch {
    case hack: IllegalBlockSizeException => {
      println("Warning. Got IllegalBlockSizeException " + cookies.get(Auth.COOKIE))
      halt(500, mkError("Error"))
    }
  }

  /* creates obj, redirects to / if successful. 422 Unprocessable Entity otherwise. */
  def mk[U](u: U, cols: Collections[U])(implicit formats: Formats, mf: Manifest[U]): Unit = try {
    db.getCollection(cols.name).insert(cols.toMongo(u))
    redirect("/")
  } catch renderUnprocessable
  
  def mk[U](json: String, cols: Collections[U])(implicit formats: Formats, mf: Manifest[U]): Unit = try {
    mk(read[U](json), cols)  
  } catch renderUnprocessable
  
  def mkAuth(json: String): Option[String] = {
    contentType = "application/json"
    val aReq: Auth = read[Auth](json)
    val hashedPw = aReq.password.sha256.hex
    val uQuery = MongoDBObject("username" -> aReq.username, "password" -> hashedPw)
    val auth: DBObject = db.getCollection(Users.name).findOne(uQuery)

    Option(auth).map(o => {
      val code = Auth.encrypt(aReq)
      cookies.set("rm", code)
      write(AuthCode(code))
    })
  }

  def find[U](col: Collections[U]) = {
    write(db.getCollection(col.name).find().toArray().map(col.fromMongo))
  }
  
  // handle and render unprocessable
  def renderUnprocessable : PartialFunction[Throwable, String] = { 
    case unprocessable: JsonParseException => halt(422, mkError("<h1>422: Unprocessable. </h1><p>" + unprocessable + "</p>"))
    case inexistentEntity: JsonMappingException => halt(415, mkError("<h1>415: Unsupported media type. </h1> <p>Submitting a json body will succeed</p>"))
    case invalid: MappingException => halt(400,mkError("<h1>400 Bad Request</h1> <p>" + invalid.msg +"</p>"))
    case e: Exception => {
      println(e)
      halt(500,mkError("Congrats. You discovered a bug."))
    }
  }

  /* wire up the precompiled templates */
  override protected def defaultTemplatePath: List[String] = List("/WEB-INF/templates/views")
  override protected def createTemplateEngine(config: ConfigT) = {
    val engine = super.createTemplateEngine(config)
    engine.layoutStrategy = new DefaultLayoutStrategy(engine,
      TemplateEngine.templateTypes.map("/WEB-INF/templates/layouts/default." + _): _*)
    engine.packagePrefix = "templates"
    engine
  }
  /* end wiring up the precompiled templates */

  override protected def templateAttributes(implicit request: HttpServletRequest): mutable.Map[String, Any] = {
    super.templateAttributes ++ mutable.Map.empty // Add extra attributes here, they need bindings in the build file
  }

  notFound {
    // remove content type in case it was set through an action
    contentType = null
    // Try to render a ScalateTemplate if no route matched
    findTemplate(requestPath) map { path =>
      contentType = "text/html"
      layoutTemplate(path)
    } orElse serveStaticResource() getOrElse resourceNotFound()
  }
}

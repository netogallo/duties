package org.duties

import org.scalatra._
import scalate.ScalateSupport
import org.fusesource.scalate.{ TemplateEngine, Binding }
import org.fusesource.scalate.layout.DefaultLayoutStrategy
import javax.servlet.http.HttpServletRequest
import collection.mutable

import org.json4s.{NoTypeHints,Formats, MappingException}
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.{read, write}

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException
import scala.collection.JavaConversions._

import Mongo.Collections
import Mongo.Collections._

trait DutyStack extends ScalatraServlet with ScalateSupport with MongoClient {
  implicit val formats = Serialization.formats(NoTypeHints)
  
  //returns 202 CREATED if successful. 422 Unprocessable Entity otherwise.
  def mk[U](json: String, cols: Collections[U])(implicit formats: Formats, mf: Manifest[U]) = try {
    val u = read[U](json)    
    db.getCollection(cols.name).insert(cols.toMongo(u))
    //halt(202, <h1>Created {u.toString()}</h1>)
    redirect("/")
  } catch renderUnprocessable
  
  def find[U](col: Collections[U]) = {
    write(db.getCollection(col.name).find().toArray().map(col.fromMongo))
  }
  
  // handle and render unprocessable
  def renderUnprocessable : PartialFunction[Throwable, Any] = { 
    case unprocessable: JsonParseException => halt(422, <h1>Unprocessable entity</h1><p>{unprocessable}</p>) 
    case inexistentEntity: JsonMappingException => halt(415, <h1>Unsupported media type. Submitting a json body will succeed.</h1>) 
    case invalid: MappingException => halt(400, <h1>Bad Request. {invalid.msg}</h1>)
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

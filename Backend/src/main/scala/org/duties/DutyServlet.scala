package org.duties

import org.scalatra._
import scalate.ScalateSupport

import org.json4s._
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.{read, write}

import org.json4s.MappingException
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException

import Mongo.Collections
import Mongo.Collections._

import scala.collection.JavaConversions._
import com.mongodb.DBObject

class DutyServlet extends DutyStack {
  implicit val formats = Serialization.formats(NoTypeHints)

  get("/") {
    implicit val formats = Serialization.formats(NoTypeHints)
    val duty = Duty("kmels", Seq("netogallo", "kmels"), Seq())
    val user = User("kmels")

    val users: Seq[User] = db.getCollection(Users.name).find().toArray().map(Users.fromMongo)
    val duties: Seq[Duty] = db.getCollection(Duties.name).find().toArray().map(Duties.fromMongo)    

    //val duties = 
    <html>
      <body>
        {duties.map(d => write(d))}

        <h1>Post duty!</h1>
        <form method="POST" action="/duty/form">
          <textarea name='json' rows='4' cols='35'>{write(duty)}</textarea>
          <input type='submit' value='Create duty'/>
        </form>

        {users.map(u => write(u))}

        <h1>Post user!</h1>
        <form method="POST" action="/user/form">
          <textarea name='json' rows='4' cols='35'>{write(user)}</textarea>
          <input type='submit' value='Create user'/>
        </form>
      </body>
    </html>
  }

  def unprocessable : PartialFunction[Throwable, Any] = { 
    case unprocessable: JsonParseException => halt(422, <h1>Unprocessable entity</h1>) 
    case inexistentEntity: JsonMappingException => halt(415, <h1>Unsupported media type. Submitting a json body will succeed.</h1>) 
    case invalid: MappingException => halt(400, <h1>Bad Request. {invalid.msg}</h1>)
  }

    //returns 202 CREATED if successful. 422 Unprocessable Entity otherwise.
  def mk[U](json: String, cols: Collections)(implicit formats: Formats, mf: Manifest[U]) = try {
    val u = read[U](json)

    db.getCollection(cols.name).insert(cols.toMongo(u))
    //halt(202, <h1>Created {u.toString()}</h1>)
    redirect("/")
  } catch unprocessable

  post("/duty/form") { 
    try {
      mk[Duty](params("json"), Duties)
    } catch unprocessable
  }

  post("/user/form") {
    try { mk[User](params("json"), Users) } catch unprocessable
  }

  post("/duty") {
    mk[Duty](request.body, Duties)
  }
  
  post("/user") { 
    mk[User](request.body, Users) 
  }

  get("/users") {
    
  }
}

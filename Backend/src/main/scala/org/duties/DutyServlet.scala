package org.duties

import org.scalatra._
import scalate.ScalateSupport

import org.json4s._
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.{read, write}
import com.fasterxml.jackson.core.JsonParseException

class DutyServlet extends DutiesStack {

  get("/") {
    implicit val formats = Serialization.formats(NoTypeHints)
    val duty = Duty("1", "kmels", Array("netogallo", "kmels"), Array())
    
    //val duties = 
    <html>
      <body>
        <h1>Post duty!</h1>
        <form method="POST" action="/duty">
          <textarea name='json' rows='4' cols='35'>{write(duty)}</textarea>
          <input type='submit' value='Create duty'/>
        </form>
      </body>
    </html>
  }

  //returns 202 CREATED if successful. 422 Unprocessable Entity otherwise.
  post("/duty") {
    implicit val formats = Serialization.formats(NoTypeHints)
    
    try {
      val json = params("json")   
      val duty = read[Duty](json)

      halt(202, <h1>Created {duty.toString()}</h1>)
    } catch {
      case unprocessableEntity: JsonParseException => halt(422, <h1>Unprocessable entity</h1>)
    }
  }
}

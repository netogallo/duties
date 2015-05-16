package org.duties

import org.scalatra._
import scalate.ScalateSupport

import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.{read, write}

import Models._
import Mongo.Collections
import Mongo.Collections._

import com.mongodb.DBObject

//hasher
import com.roundeights.hasher.Implicits._
import scala.language.postfixOps

class DutyServlet extends DutyStack {   
  get("/") {
    val duty = Duty("kmels", Seq("netogallo", "kmels"), Seq())
    val user = User("kmels", "pw")
    val auth = UtilObjects.Auth.fromUser(user)
    val hashedPw = "pw".sha256.hex
    val task = Task(penalty = 1.5d, name = "My task", recurrent = false,
                  description = Some("Optional description"), entrusted = Option(User("Optional entrusted/asignee", hashedPw)), votes = Seq(User("An optional user's vote", hashedPw)))

    //val duties = 
    <html>
      <body>
      <h1>Post /auth</h1>
        <form method="POST" action="/auth/form">
          <textarea name='json' rows='4' cols='45'>{write(auth)}</textarea>
          <input type='submit' value='Post authentication'/>
        </form>

        <h1>GET /users</h1>
        {find(Users)}

        <h1>Post /user</h1>
        <form method="POST" action="/user/form">
          <textarea name='json' rows='4' cols='45'>{write(user)}</textarea>
          <input type='submit' value='Create user'/>
        </form>
        
        <h1>GET /duties</h1>
        {find(Duties)}

        <h1>POST /duty</h1>
        <form method="POST" action="/duty/form">
          <textarea name='json' rows='4' cols='45'>{write(duty)}</textarea>
          <input type='submit' value='Create duty'/>
        </form>

        <h1>GET /tasks</h1>
        {find(Tasks)}

        <h1>Post /task</h1>
        <form method="POST" action="/task/form">
          <textarea name='json' rows='9' cols='45'>{write(task)}</textarea>
          <input type='submit' value='Create task'/>
        </form>
      </body>
    </html>
  }
    
  // create by forms
  post("/duty/form") { 
    mk[Duty](params("json"), Duties)
  }

  post("/user/form") {
    mk[User](params("json"), Users)
  }

  post("/task/form") {
    mk[Task](params("json"), Tasks)
  }
  
  post("/auth/form") {
    mkAuth(params("json"))
  }

  // create
  post("/duty") {
    mk[Duty](request.body, Duties)
  }
  
  post("/user") { 
    mk[User](request.body, Users) 
  }

  post("/task") {
    mk[Task](request.body, Tasks)
  }  
  
  post("/auth"){
    mkAuth(request.body)
  }

  post("/invite"){
    //requireAuth()
    //mk[Invite](request.body, Invites)
  }

  // list
  get("/duties") {
    find(Duties)
  }

  get("/users") {
    find(Users)
  }

  get("/tasks") {
    find(Tasks)
  }

  get("/invites") {
    //find(Invites(user))
  }
  
}

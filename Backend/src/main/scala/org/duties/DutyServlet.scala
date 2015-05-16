package org.duties

import org.scalatra._
import scalate.ScalateSupport

import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.{read, write}

import Models._
import Mongo.Collections
import Mongo.Collections._

import Auth._
import com.mongodb.DBObject

class DutyServlet extends DutyStack with Homepage with Captchas {
  get("/") { home }
  
  // create by forms
  post("/duty/form") {
    val in = params("json")
    val username = requireAuth
    val duty = read[Duty](in)
    val isAuthor = username.equals(duty.author)
    if (isAuthor) mk[Duty](duty, Duties)
    else mkError("Author must be logged")
  }

  post("/user/form") {
    mk[User](params("json"), Users)
  }

  post("/task/form") {
    mk[Task](params("json"), Tasks)
  }
  
  post("/auth/form") {
    val json_code: String = mkAuth(params("json"))
    val auth: AuthCode = read[AuthCode](json_code)
    cookies.set("rm",auth.code)
    redirect("/")
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
  
  post("/log-out"){
    cookies.delete(Auth.COOKIE)
    redirect("/")
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

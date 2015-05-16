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
  
  def mkDuty(in: String) = {
    val username = requireAuth
    val duty = read[Duty](in)
    val isAuthor = username.equals(duty.author)
    if (isAuthor) mk[Duty](duty, Duties)
    else mkError("Author must be logged")
  }

  def mkInvite(in: String) = {
    val username = requireAuth
    val invite = read[Invite](in)
    val isAuthor = username.equals(invite.author)
    if (isAuthor) mk[Invite](invite, Invites)
    else mkError("Author must be logged")
  }
  // create by forms
  post("/duty/form") {
    mkDuty(params("json"))
  }

  post("/user/form") {
    mk[User](params("json"), Users)
  }

  post("/task/form") {
    mk[Task](params("json"), Tasks)
  }
  
  post("/login/form") { 
    try {
      val authCode = mkAuth(params("json"))
      if (authCode.isDefined) redirect("/")
      else halt(404,mkError("<p>Not found</p>"))
    } catch renderUnprocessable
  }
  
  post("/invite/form") {
    mkInvite(request.body)
  }

  // create
  post("/duty") {
    mkDuty(request.body)
  }
  
  post("/user") { 
    mk[User](request.body, Users) 
  }

  post("/task") {
    mk[Task](request.body, Tasks)
  }  
  
  post("/login") {
    try {
      val authCode = mkAuth(request.body)
      if (authCode.isDefined) authCode.get
      else halt(404, "<p>Not Found</p>")
    } catch renderUnprocessable
  }
  
  post("/log-out"){
    cookies.delete(Auth.COOKIE)
    redirect("/")
  }

  post("/invite"){
    mkInvite(params("json"))
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

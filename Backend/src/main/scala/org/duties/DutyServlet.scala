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

//bitcoinj 
import org.bitcoinj.core.{Address, Coin, Sha256Hash}

class DutyServlet extends DutyStack with Homepage with Captchas {
  get("/") { home }
  
  def mkDuty(in: String) = {
    val username = requireAuth
    val duty = read[Duty](in)
    val isAuthor = username.equals(duty.author.username)
    if (isAuthor) mk[Duty](duty, Duties)
    else mkError("Author must be logged")
  }
  
  //author must be logged in
  //every task must exist
  def mkInvite(in: String) = {
    val username = requireAuth
    val invite = read[Invite](in)
    val isAuthor = username.equals(invite.author.username)
//    val refs = invite.tasks.map(t => TaskRefs.fromTask(t, Some(d)))
    val missingRefs: Seq[TaskRef] = invite.tasks.filter(r => !TaskRefs.exists(r.task_id))

    if (missingRefs.nonEmpty) mkError("There is no duty containing this task_id: " +missingRefs.head.task_id)
    else 
    if (!isAuthor) mkError("Author must be logged")
    else {
      invite.tasks.foreach(t => {
        val address = TaskAddress(t.task_id, Bithack.mkReceivingAddress)
        mk[TaskAddress](address, TaskAddresses)
      })
      
      mk[Invite](invite, Invites)
    }
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
    
  post("/invite") {
    mkInvite(request.body)
  }

  // create
  post("/duty") {
    mkDuty(request.body)
  }
  
  post("/user") { 
    mk[User](request.body, Users) 
  }
  
  post("/login") {
    try {
      val authCode = mkAuth(request.body)
      if (authCode.isDefined) authCode.get
      else halt(404, mkError("<p>Not Found</p>"))
    } catch renderUnprocessable
  }
  
  post("/log-out"){
    cookies.delete(Auth.COOKIE)
    redirect("/")
  }

  post("/invite/form"){
    mkInvite(params("json"))
  }

  // list
  get("/duties") {
    find(Duties)
  }

  get("/users") {
    find(Users)
  }

  get("/invites") {
    val user = requireAuth
    write(Invites.findAdvocate(user))
  }
  
}

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
  
  //creates task refs and generates addreses for each user
  def mkDuty(in: String) = {
    val username = requireAuth
    val duty = read[Duty](in)
    val isAuthor = username.equals(duty.author.username)
    val missingPpl = duty.participants.filter(p => !Users.existsIdent(p.username))
    
    if (missingPpl.nonEmpty) mkError("All participants must exist. There is no username " + missingPpl.head.username)
    else 
      if (!isAuthor) mkError("Author must be logged")
      else mk[Duty](duty, Duties)
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
      mk[Invite](invite, Invites)
    }
  }
  
  def taskOutput(json: String, username: String): String = try {    
    val ref = read[TaskRef](json)
    val task_id = ref.task_id
    val duty_id = ref.duty_id
    val taskRef: Option[TaskRef] = TaskRefs.find(task_id)
    val taskOutput = taskRef.map(ref => TaskOutputs.findOutput(ref, UserIdent(username)))

    if (!taskRef.isDefined) mkError("This task isn't referenced: " + ref.task_id)
    else
    if (!taskOutput.isDefined) mkError("Something's odd, task exists but cannot find output for username " +username)
    else write(taskOutput.get)
  } catch renderUnprocessable

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

  get("/me") {
    maybeAuth match {
      case None => mkError("Nobody logged.")
      case Some(u) => write(UserIdent(u))
    }
  }
  
  get("/address/form") { 
    val u = requireAuth
    taskOutput(params("json"), u) 
  }

  get("/address"){ 
    val u = requireAuth
    taskOutput(request.body, u) 
  }
}

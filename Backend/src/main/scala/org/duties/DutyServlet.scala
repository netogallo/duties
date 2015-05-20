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
import Bithack._

class DutyServlet extends DutyStack with Homepage with Captchas {
  get("/") { redirect("/Frontend") }
  get("/api") { home }
  get("/admin") { admin } 

  //creates task refs and generates addreses for each user
  def mkDuty(in: String) = try {
    val uid = requireAuth
    val duty = read[Duty](in)
    val isAuthor = uid.username == duty.author.username
    val missingPpl = duty.participants.filter(p => !Users.existsIdent(p.username))
    
    if (missingPpl.nonEmpty) mkError("All participants must exist. There is no username " + missingPpl.head.username)
    else 
      if (!isAuthor) mkError("Author must be logged")
      else mk[Duty](duty, Duties)
  } catch renderUnprocessable
  
  //author must be logged in
  //every task must exist
  def mkInvite(in: String) = {
    val uid = requireAuth
    val invite = read[Invite](in)
    val isAuthor = uid.username == (invite.author.username)
    val missingRefs: Seq[TaskRef] = invite.tasks.filter(r => !TaskRefs.exists(r.task_id))
    if (invite.tasks.isEmpty) mkError("You have 0 tasks in your invite.")
    else
    if (missingRefs.nonEmpty) mkError("There is no duty containing this task_id: " +missingRefs.head.task_id)
    else 
    if (!isAuthor) mkError("Author must be logged")
    else {
      val taskRefs = invite.tasks.map(t => TaskRefs.find(t.task_id)).flatten
      mk[Invite](invite.copy(tasks = taskRefs), Invites)
    }
  }
  
  def taskOutput(json: String, uid: UserIdent): String = try {    
    val ref = read[TaskRef](json)
    val task_id = ref.task_id
    println("Looking for testOutput's taskRef : "+task_id)
    val taskRef: Option[TaskRef] = TaskRefs.find(task_id)
    println("Task Ref: "+ taskRef)
    val taskOutput: Option[TaskOutput] = taskRef.flatMap(ref => TaskOutputs.findOutput(ref, uid))
    
    if (!taskRef.isDefined) mkError("This task isn't referenced: " + ref.task_id)
    else
    if (!taskOutput.isDefined) mkError("Something's odd, task exists but cannot find output for username " +uid.username)
    else write(taskOutput.get)
  } catch renderUnprocessable

  //validates that task_id exists
  def mkReport(json: String) = try {
    val rep: Report = read[Report](json)    
    val ref: Option[TaskRef] = TaskRefs.find(rep.task.task_id)   
    val task: Option[Task] = ref.flatMap(Tasks.fromRef)
    val taskReports: Seq[Report] = ref.map(Reports.findReports).getOrElse(Nil)
    val previousReport = taskReports.find(_.reporter == rep.reporter)

//task.map(t => t.reported_by.contains(rep.reporter))
    if (!task.isDefined) mkError("This task doesn't exist: " + rep.task.task_id)
    else
    if (previousReport.isDefined) {
      //mkError("This task is reported by you");
      Reports.remove(ref.get, rep.reporter)
      val reportsNow = ref.map(r => Reports.findReports(r)).getOrElse(Nil)
      write(reportsNow)
    }
    else
    if (!task.get.entrusted.isDefined) mkError("You can report a task only if it's entrusted.")
    else
    if (task.get.entrusted.get == rep.reporter) mkError("You can't report your entrusted task. Come on, it will eventually expire!")
    else {      
      mk[Report](rep.copy(task = ref.get), Reports)
      val reportsNow = ref.map(Reports.findReports).getOrElse(Nil)
      write(reportsNow)
    }
  } catch renderUnprocessable

  // create by forms
  post("/duty/form") {
    mkDuty(params("json"))
  }

  post("/user/form") {
    val json = params("json")
    val u = read[User](json)
    if (Users.exists(u.toIdent)) mkError("User already exists")
    else {
      try {
        new Address(OPERATING_NETWORK, u.btc_address) 
        mk[User](json, Users)
      } catch {
        case e: Exception => mkError("You entered an invalid bitcoin address.")
      }
    }
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
    val json = request.body
    val u = read[User](json)
    if (Users.exists(u.toIdent)) mkError("User already exists")
    else {
      try {
        new Address(OPERATING_NETWORK, u.btc_address) 
        mk[User](json, Users)
      } catch {
        case e: Exception => mkError("You entered an invalid bitcoin address.")
      }
    }
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
  
  post("/logout"){
    cookies.delete(Auth.COOKIE)
    redirect("/")
  }

  post("/invite/form"){
    mkInvite(params("json"))
  }
    
  post("/report/form"){
    val u = requireAuth
    mkReport(params("json"))
  }

  post("/report"){
    val u = requireAuth
    mkReport(request.body)
  }

  def mapTasks(json: String) = try {
      val taskrefs = read[Seq[TaskRef]](json)            
      val tasks: Seq[(TaskRef,Option[Task])] = taskrefs.map(r => (r, Tasks.fromRef(r)))
      val inexistent = tasks.find(t => !t._2.isDefined)           
      
      if (inexistent.isDefined) mkError("This task is inexistent: " + inexistent.get._1.task_id)
      else write(tasks.map(_._2).flatten)
    } catch renderUnprocessable

  post("/tasks/form"){
    mapTasks(params("json"))
  }

  post("/tasks"){
    mapTasks(request.body)
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
    write(Invites.findAdvocate(user.username))
  }

  get("/me") {
    maybeAuth match {
      case None => mkError("Nobody logged.")
      case Some(u) => write(u)
    }
  }
  
  post("/address/form") { 
    val uid = requireAuth
    println("Required auth " + uid.username)
    taskOutput(params("json"), uid) 
  }

  post("/address"){ 
    val uid = requireAuth
    taskOutput(request.body, uid)
  }  
}

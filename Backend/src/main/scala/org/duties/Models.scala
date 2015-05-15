package org.duties

import com.mongodb.casbah.Imports._

object Models {
  case class Task(name: String, description: Option[String] = None, penalty: Double, entrusted: Option[User] = None, votes: Seq[User] = Nil, recurrent: Boolean, task_id: String = (new ObjectId().toString()))

  case class Duty(author: String, participants: Seq[String], tasks: Seq[Task], id: String = (new ObjectId().toString()))
  
  case class User(username: String, password: String, id: String = (new ObjectId().toString()))
}


object UtilObjects  {
  import java.util.Date
  case class Auth(username: String, password: String, date: Date)
}

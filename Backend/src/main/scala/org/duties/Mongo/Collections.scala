package org.duties

import com.mongodb.casbah.Imports._

import scala.reflect.api.TypeTags
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._
import scala.reflect._
import java.lang.reflect.Constructor

//mongo
import Models._

//data structures
import scala.collection.JavaConversions._

object Mongo {
  trait Collections[T] {
    def name: String 
    
    // converts case classes with primitive members
    def toMongo[U](u: U)(implicit utag: TypeTag[U]): MongoDBObject = {
      val clazz = u.getClass()
      val members: List[MethodSymbol] = typeOf[U].members.collect { case m :MethodSymbol if m.isCaseAccessor => m}.toList       
      val elems: List[(String, AnyRef)] = members.map(m => {
        val member = m.name.toString()
        val property = if (member == "id") "_id" else member        
        val value = clazz.getMethod(member).invoke(u)
        (property,value)
      })
      MongoDBObject(elems)
    }

    def fromMongo(o: DBObject): T
  }
  
  object Collections {
    object Tasks extends Collections[Task] {
      override def name = "tasks"
        
      override def fromMongo(o: DBObject): Task = {
        val n = o.as[String]("name")
        val d = Option(o.as[String]("description"))
        val p = o.as[Double]("penalty")
        val e = Option(o.as[String]("entrusted"))
        val rs: Seq[String] = o.as[BasicDBList]("reports").toSeq.map(_.asInstanceOf[String])
        val r = o.as[Boolean]("recurrent")
        val tid = o.as[String]("_id")

        new Task(
          name = n,
          description = d,
          penalty = p,
          entrusted = e,
          reports = rs,
          recurrent = r,
          id = tid
        )
      }
    }

    object Duties extends Collections[Duty] with MongoClient { 
      override def name = "duties" 

      override def toMongo[U](u: U)(implicit tag: TypeTag[U]): MongoDBObject = {
        val duty = super.toMongo(u)
        val d = u.asInstanceOf[Duty]
        duty.update("participants", MongoDBList(d.participants.map(ui => UserIdents.toMongo(ui)) : _*))
        duty.update("author", UserIdents.toMongo(d.author))
        duty.update("tasks", MongoDBList(d.tasks.map(t => Tasks.toMongo(t)) : _*))
        duty
      }

      override def fromMongo(o: com.mongodb.casbah.Imports.DBObject): Duty = { 
        val ps: Seq[DBObject] = o.as[BasicDBList]("participants").map(_.asInstanceOf[DBObject])

        val ts: Seq[Task] = o.as[BasicDBList]("tasks").map{ o => Tasks.fromMongo(o.asInstanceOf[DBObject])}

        Duty(
          author = UserIdents.fromMongo(o.as[MongoDBObject]("author")),
          participants = ps.map(UserIdents.fromMongo),
          tasks = ts,
          id = o.as[String]("_id")
        )
      }
      def findId(id: String): Option[Duty] = {
        val q = MongoDBObject("_id" -> id)
        val d = db.getCollection(name).findOne(q)
        Option(fromMongo(d))
      }
    }

    implicit object Users extends Collections[User] with MongoClient { 
      import com.roundeights.hasher.Implicits._
      import scala.language.postfixOps
      override def name = "users"
      override def toMongo[U](u: U)(implicit tag: TypeTag[U]): MongoDBObject = {
        val unencrypted = super.toMongo(u)
        unencrypted.update("password", u.asInstanceOf[User].password.sha256.hex)
        unencrypted 
      }

      override def fromMongo(m: DBObject) = {
        User(
          username = m.as[String]("username"),
          password = "<hidden>",
          id = m.as[String]("_id")
        )
      }

      def findUsername(ident: String): Option[User] = {
        val q = MongoDBObject("username" -> ident)
        val u = db.getCollection(name).findOne(q)
        Option(u).map(fromMongo)
      }
    }

    implicit object Invites extends Collections[Invite] with MongoClient{
      override def name = "invites"

      def findAdvocate(a: String): Seq[Invite] = { 
        val q = MongoDBObject("advocate" -> UserIdents.toMongo(UserIdent(a)))
        db.getCollection(name).find(q).toArray().map(fromMongo)
      }

      override def toMongo[U](u: U)(implicit tag: TypeTag[U]): MongoDBObject = {
        val invite = super.toMongo(u)
        val i = u.asInstanceOf[Invite]
        invite.update("author", UserIdents.toMongo(i.author))
        invite.update("advocate", UserIdents.toMongo(i.advocate))
        invite.update("tasks", MongoDBList(i.tasks.map(t => TaskRefs.toMongo(t)) : _*))      
        invite
      }
      
      override def fromMongo(o: DBObject) = {
        Invite(
          author = UserIdents.fromMongo(o.as[DBObject]("author")),
          advocate = UserIdents.fromMongo(o.as[DBObject]("advocate")),
          tasks = o.as[BasicDBList]("tasks").toSeq.map(t => TaskRefs.fromMongo(t.asInstanceOf[DBObject])),
          duty = Option(o.as[String]("duty"))
        )
      }
    }

    implicit object UserIdents extends Collections[UserIdent] {
      def name = "unpersisted"      
      def fromMongo(o: DBObject) = UserIdent(username = o.as[String]("username"))
    }

    implicit object TaskRefs extends Collections[TaskRef] {
      def name = "unpersisted"
      def fromMongo(o: DBObject): TaskRef = TaskRef(task_id = o.as[String]("task_id"))
    }
  }
}

trait MongoClient { 
  val mongo = MongoClient("localhost", 27017)   
  val db: MongoDB = mongo("bithack")
} 

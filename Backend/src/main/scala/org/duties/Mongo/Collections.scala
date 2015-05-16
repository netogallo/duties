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

    def toMongo[U](u: U)(implicit utag: TypeTag[U]): DBObject = {
      val clazz = u.getClass()
      val members: List[MethodSymbol] = typeOf[U].members.collect { case m :MethodSymbol if m.isCaseAccessor => m}.toList
       
      val elems: List[(String, AnyRef)] = members.map(m => {
        val member = m.name.toString()
        val property = if (member == "id" || member == "task_id") "_id" else member
        
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
        val rs: Seq[String] = o.as[BasicDBList]("votes").toSeq.map(_.asInstanceOf[String])
        val r = o.as[Boolean]("recurrent")
        val id = o.as[String]("_id")

        new Task(
          name = n,
          description = d,
          penalty = p,
          entrusted = e,
          reports = rs,
          recurrent = r,
          task_id = id
        )
      }
    }

    object Duties extends Collections[Duty] with MongoClient { 
//      type T = Duty
      override def name = "duties" 
      override def fromMongo(o: com.mongodb.casbah.Imports.DBObject): Duty = { 
        val ps: Seq[String] = o.as[BasicDBList]("participants").map(_.toString)

        val ts: Seq[Task] = o.as[BasicDBList]("tasks").map{ o => 
          {
            println("DBOBJECTLIST: "+ o.toString())
            null
          //Tasks.fromMongo(o)
          }
        }

        Duty(
          author = o.as[String]("author"),
          participants = ps,
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
      override def toMongo[U](u: U)(implicit tag: TypeTag[U]): DBObject = {
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
        val q = MongoDBObject("advocate" -> a)
        db.getCollection(name).find(q).toArray().map(fromMongo)
      }     
      
      override def fromMongo(o: DBObject) = {
        Invite(
          author = o.as[String]("author"),
          advocate = o.as[String]("advocate"),
          tasks = o.as[BasicDBList]("tasks").toSeq.map(t => Tasks.fromMongo(t.asInstanceOf[DBObject])),
          duty = Option(o.as[String]("duty"))
        )
      }
    }
  }
}

trait MongoClient { 
  val mongo = MongoClient("localhost", 27017)   
  val db: MongoDB = mongo("bithack")
} 

package org.duties

import com.mongodb.casbah.Imports._

import scala.reflect.api.TypeTags
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._
import scala.reflect._
import java.lang.reflect.Constructor

class Task

case class Duty(author: String, participants: Seq[String], tasks: Seq[Task], id: String = (new ObjectId().toString()))

case class User(username: String, id: String = (new ObjectId().toString()))

object Mongo {
  trait Collections {
    type T
    def name: String 

    def toMongo[U : TypeTag](u: U): DBObject = {
      val clazz = u.getClass()
      val members: List[MethodSymbol] = typeOf[U].members.collect { case m :MethodSymbol if m.isCaseAccessor => m}.toList
       
      val elems: List[(String, AnyRef)] = members.map(m => {
        val member = m.name.toString()
        val property = if (member == "id") "_id" else member

        val field = clazz.getMethod(member) 
        (property,field.invoke(u))
      })

      MongoDBObject(elems)
    }
    def fromMongo(o: DBObject): T
    /*def fromMongo[U: ClassTag](o: DBObject): U = { 
      val clazz = classTag[U].runtimeClass
      val construct: Constructor[_] = clazz.getConstructors()(0)
      
      val types = construct.getGenericParameterTypes().toList
      //val params: Array[Object] = o.keys.toArray.reverse.map(k => o(k))
      
      val params: List[Object] = (types,o.keys.toArray.reverse).zipped.map( { case (t,k) => {
        println("Instanceando " + k + " como un " + t.getClass)
        o(k)
      }})

      println("Clazz: " + clazz.toString())
      println("Constructs: " + clazz.getConstructors().mkString(","))
      println("Construct: " + construct.toString)
      println("Types: " + types.mkString(","))
      println("Params: " + params.mkString(",") + "\n\n"

            )
      construct.newInstance(params : _*).asInstanceOf[U]
    }*/
  }
  
  object Collections {
    object Tasks extends Collections {
      type T = Task
      override def name = "tasks"
      override def fromMongo(o: DBObject): T = {
        new Task
      }
    }

    object Duties extends Collections { 
      type T = Duty
      override def name = "duties" 
      override def fromMongo(o: com.mongodb.casbah.Imports.DBObject): T = { 
        println("Object: " + o.toString);
        val ps: Seq[String] = o.as[BasicDBList]("participants").map(_.toString)

        val ts: Seq[Task] = o.as[BasicDBList]("tasks").map{ o => 
          new Task
          //Tasks.fromMongo(o)
        }

        println(ps.mkString(","))
        println(ps.mkString(","))

        Duty(
          author = o.as[String]("author"),
          participants = ps,
          tasks = ts,
          id = o.as[String]("_id")
        )
      }
    }

    implicit object Users extends Collections { 
      type T = User
      override def name = "users"
//      override def name = "users" 
      //override def fromMongo[T](o: DBObject): T = fromMongo
      override def fromMongo(m: DBObject): T = User(
        username = m.as[String]("username"),
        id = m.as[String]("_id")
      )
    }
  }
}

trait MongoClient { 
  val mongo = MongoClient("localhost", 27017)   
  val db: MongoDB = mongo("bithack")
} 

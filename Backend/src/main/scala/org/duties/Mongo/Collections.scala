package org.duties

import com.mongodb.casbah.Imports._

import scala.reflect.api.TypeTags
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._
import scala.reflect._
import java.lang.reflect.Constructor

import Models._

object Mongo {
  trait Collections[T] {
    def name: String 

    def toMongo[U : TypeTag](u: U): DBObject = {
      val clazz = u.getClass()
      val members: List[MethodSymbol] = typeOf[U].members.collect { case m :MethodSymbol if m.isCaseAccessor => m}.toList
       
      val elems: List[(String, AnyRef)] = members.map(m => {
        val member = m.name.toString()
        val property = if (member == "id" || member == "task_id") "_id" else member

        println("MEMBER: "+member)        
        val field = clazz.getMethod(member) 
        println("FIELD: "+field)        
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
    object Tasks extends Collections[Task] {
      override def name = "tasks"
      override def fromMongo(o: DBObject): Task = {
        val n = o.as[String]("name")
        val d = Option(o.as[String]("description"))
        val p = o.as[Double]("penalty")
        val e = Option(o.as[User]("entrusted"))
        val vs: Seq[DBObject] = o.as[BasicDBList]("votes").toSeq.map(_.asInstanceOf[DBObject])
        val r = o.as[Boolean]("recurrent")
        val id = o.as[String]("_id")

        new Task(
          name = n,
          description = d,
          penalty = p,
          entrusted = e,
          votes = Nil, //vs.map(o => Users.fromMongo(o)),
          recurrent = r,
          task_id = id
        )
      }
    }

    object Duties extends Collections[Duty] { 
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
    }

    implicit object Users extends Collections[User] { 
      override def name = "users"
      override def fromMongo(m: DBObject) = {
        println("GOT: "+m)
        User(
          username = m.as[String]("username"),
          id = m.as[String]("_id")
        )
      }
    }
  }
}

trait MongoClient { 
  val mongo = MongoClient("localhost", 27017)   
  val db: MongoDB = mongo("bithack")
} 

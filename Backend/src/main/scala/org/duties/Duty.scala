package org.duties

import com.mongodb.casbah.Imports._

import scala.reflect.api.TypeTags
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._
import scala.reflect._
import java.lang.reflect.Constructor

class Task

case class Duty(id: String, author: String, participants:Array[String], tasks: Array[Task])

case class User(username: String)

object Mongo {
  trait Collections[U] {
    def name: String 
    def toMongo[U: TypeTag](u: U): DBObject = {
      val clazz = u.getClass()
      val members: List[MethodSymbol] = typeOf[U].members.collect { case m :MethodSymbol if m.isCaseAccessor => m}.toList
       
      val elems: List[(String, AnyRef)] = members.map(m => {
        val member = m.name.toString()
        val field = clazz.getMethod(member) 
        (member,field.invoke(u))
      })

      MongoDBObject(elems)
    }
    def fromMongo[U: ClassTag](o: DBObject): U = { 
      val clazz = classTag[U].getClass()
      val params: Array[Object] = o.values.toArray
      val construct: Constructor[_] = clazz.getConstructors()(0)
      construct.newInstance(params : _*).asInstanceOf[U]
    } 
  }
  
  object Collections{
    implicit object Duties extends Collections[Duty] { override def name = "duties" }
    implicit object Users extends Collections[User] { override def name = "users" }
  }
}

trait MongoClient { 
  val mongo = MongoClient("localhost", 27017)   
  val db: MongoDB = mongo.getDB("bithack")
} 



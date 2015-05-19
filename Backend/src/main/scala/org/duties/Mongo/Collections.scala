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

import org.bitcoinj.core.Address

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
    object Tasks extends Collections[Task] with MongoClient {
      val Q: TaskRef => DBObject = ref => MongoDBObject("tasks._id" -> ref.task_id)
      override def name = "tasks"      
      def bountyIncrease(taskRef: TaskRef, value: Double): Int = {  
        val t = fromRef(taskRef)
        if (!t.isDefined) 0
        else {
          val task = t.get
          val dutyTask = Q(taskRef)
          val newBounty: Double = task.total_bounty.getOrElse(0d) + value
          println("Setting total bounty to "+newBounty)
          val nextBounty = MongoDBObject("$ref" -> MongoDBObject("tasks.$.total_bounty" -> newBounty))
          val result = db.getCollection(Duties.name).update(dutyTask, nextBounty)
          result.getN
        }
      }        
      def rewardIncrease(taskRef: TaskRef, value: Double): Int ={
        val t = fromRef(taskRef)
        if (!t.isDefined) 0
        else {
          val task = t.get
          val dutyTask = Q(taskRef)
          val newReward = (task.total_bounty.getOrElse(0d) + value)
          println("Setting reward to "+newReward)
          val nextReward = MongoDBObject("$ref" -> MongoDBObject("tasks.$.reward_bounty" -> newReward))
          val result = db.getCollection(Duties.name).update(dutyTask, nextReward)
          result.getN
        }
      }
      //todo: move previous payments to rewards
      def setEntrusted(task: Task, owner: UserIdent): Int = {
        val dutyTask = MongoDBObject("tasks._id" -> task.id)
        val taskEntrusted = MongoDBObject("$set" -> MongoDBObject("tasks.$.entrusted" -> owner.username))
        val taskState = MongoDBObject("$set" -> MongoDBObject("tasks.$.state" -> "Entrusted"))
        val result = db.getCollection(Duties.name).update(dutyTask, taskEntrusted)
        val result2 = db.getCollection(Duties.name).update(dutyTask, taskState)
        result.getN
      }
      def setRewarded(task: Task, owner: UserIdent): Int = {
        val dutyTask = MongoDBObject("tasks._id" -> task.id)
        val taskState = MongoDBObject("$set" -> MongoDBObject("tasks.$.state" -> "Rewarded"))
        val result = db.getCollection(Duties.name).update(dutyTask, taskState)
        result.getN
      }
      def fromRef(r: TaskRef): Option[Task] = {
        val q = "tasks" $elemMatch MongoDBObject("_id" -> r.task_id)
        val o = Option(db.getCollection(Duties.name).findOne(q))
        val d: Option[Duty] = o.map(Duties.fromMongo)
        val tasks: Option[Seq[Task]] = d.map(duty => duty.tasks)
        val t = tasks.flatMap(t => t.find(task => task.id == r.task_id))
        t
      }      
      override def fromMongo(o: DBObject): Task = {
        val tid = o.as[String]("_id")        
        val ref = TaskRefs.find(tid)
        val reports: Seq[Report] = ref.map(Reports.findReports).getOrElse(Nil)
        val uids = reports.map(_.reporter)
        
        new Task(
          name = o.as[String]("name"),
          description = Option(o.as[String]("description")),
          state = Option(o.as[String]("state")),
          penalty = o.as[Double]("penalty"),
          entrusted = Option(o.as[String]("entrusted")),
          recurrent = o.as[Boolean]("recurrent"),
          reported_by = uids,
          id = tid,
          total_bounty = Option(o.as[Double]("total_bounty")),
          reward_bounty = Option(o.as[Double]("reward_bounty")),
          payments = {
            val xx = ref.map(Payments.findPayments).getOrElse(Nil)
            println("Found payments: " +xx.mkString(","))
            xx
          },
          expiry_epoch = o.as[Long]("expiry_epoch")
        )
      }
    }

    object Duties extends Collections[Duty] with MongoClient { 
      override def name = "duties" 
      
      def find(duty_id: String): Option[Duty] = {
        val q = MongoDBObject("_id" -> duty_id)
        val o = Option(db.getCollection(name).findOne(q))
        o.map(fromMongo)
      }

      override def toMongo[U](u: U)(implicit tag: TypeTag[U]): MongoDBObject = {
        val duty = super.toMongo(u)
        val d = u.asInstanceOf[Duty]
        val tasks = d.tasks
        val members = d.participants
        duty.update("participants", MongoDBList(d.participants.map(ui => UserIdents.toMongo(ui)) : _*))
        duty.update("author", UserIdents.toMongo(d.author))
        duty.update("tasks", MongoDBList(tasks.map(t => Tasks.toMongo(t)) : _*))
        
        //insert task refs too.
        val refs = tasks.map( t => {
          val ref = TaskRefs.fromTask(t, Some(d))
          db.getCollection(TaskRefs.name).insert(TaskRefs.toMongo(ref))
          ref 
        })
        
        //generate task outputs too
        val outputs: Seq[Seq[TaskOutput]] = refs.map(ref => {
          members.map(m => {
            val address = TaskOutput(m, ref, Bithack.mkReceivingAddress.toString)
            db.getCollection(TaskOutputs.name).insert(TaskOutputs.toMongo(address))
            address
          })
        })
        
        duty
      }

      override def fromMongo(o: com.mongodb.casbah.Imports.DBObject): Duty = { 
        val ps: Seq[DBObject] = o.as[BasicDBList]("participants").map(_.asInstanceOf[DBObject])

        val ts: Seq[Task] = o.as[BasicDBList]("tasks").map{ o => Tasks.fromMongo(o.asInstanceOf[DBObject])}

        Duty(
          author = UserIdents.fromMongo(o.as[MongoDBObject]("author")),
          name = o.as[String]("name"),
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
      def find(uid: UserIdent): Option[User] = {
        val q = UserIdents.toMongo(uid)
        val o = Option(db.getCollection(name).findOne(q))
        o.map(fromMongo)
      }
      
      def exists(uid: UserIdent) = find(uid).isDefined

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
      
      def existsIdent(ident: String) = findUsername(ident).isDefined
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
        val trefs: Seq[TaskRef] = o.as[BasicDBList]("tasks").toSeq.map(t => TaskRefs.fromMongo(t.asInstanceOf[DBObject]))
        
        val duty_id: Option[String] = trefs.headOption.flatMap(taskRef => {
          val ref: Option[TaskRef] = TaskRefs.find(taskRef.task_id)
          ref.flatMap(_.duty_id)
        })

        val d: Option[Duty] = duty_id.flatMap(Duties.find)

        Invite(
          author = UserIdents.fromMongo(o.as[DBObject]("author")),
          advocate = UserIdents.fromMongo(o.as[DBObject]("advocate")),
          tasks = trefs,
          duty = d,
          id = o.as[String]("_id")
        )
      }
    }

    implicit object UserIdents extends Collections[UserIdent] {
      def name = "unpersisted"       
      def fromMongo(o: DBObject) = UserIdent(username = o.as[String]("username"))
      def toMongo(uid: UserIdent) = DBObject("username" -> uid.username)
    }

    implicit object TaskRefs extends Collections[TaskRef] with MongoClient {
      def name = "task_refs"

      def find(task_id: String): Option[TaskRef] = {
        val q = MongoDBObject("task_id" -> task_id)
        val o = Option(db.getCollection(name).findOne(q))
        o.map(fromMongo)
      }

      def exists(task_id: String): Boolean = find(task_id).isDefined
      
      override def toMongo[U](u: U)(implicit tag: TypeTag[U]): MongoDBObject = {
        val tref = super.toMongo(u)
        val t = u.asInstanceOf[TaskRef]
        //don't search for null, don't persist null
        if (t.duty_id.isEmpty) tref.remove("duty_id")
        tref
      }

      override def fromMongo(o: DBObject): TaskRef = TaskRef(
        task_id = o.as[String]("task_id"),
        duty_id = Option(o.as[String]("duty_id"))
      )

      def fromTask(t: Task, d: Option[Duty] = None): TaskRef = 
        TaskRef(task_id = t.id, duty_id = d.map(_.id))
    }

    implicit object TaskOutputs extends Collections[TaskOutput] with MongoClient {
      def name = "task_outputs"
      def findAddress(adr: Address): Option[TaskOutput] = try {
        println("#findAddress: " + adr.toString)
        val q = MongoDBObject("btc_address" -> adr.toString)
        val o = Option(db.getCollection(name).findOne(q))
        o.map(fromMongo)
      } catch {
        case e: Exception => {
          println(e)
          e.printStackTrace()
          println("Could not find, returning None as TaskOutput for address " +adr.toString)
          None
        }
      }
      def findOutput(ref: TaskRef, uid: UserIdent): Option[TaskOutput] = {
        val q = MongoDBObject("owner" -> UserIdents.toMongo(uid), "task" -> TaskRefs.toMongo(ref))       
        val o = Option(db.getCollection(name).findOne(q))
        o.map(fromMongo)
      }

      override def toMongo[U](u: U)(implicit tag: TypeTag[U]): MongoDBObject = {
        val ta = super.toMongo(u)
        val t = u.asInstanceOf[TaskOutput]
        ta.update("btc_address", t.btc_address.toString)
        ta.update("owner", UserIdents.toMongo(t.owner))        
        ta.update("task", TaskRefs.toMongo(t.task))
        ta
      }
      override def fromMongo(o: DBObject) = {       
        val address = new Address(Bithack.OPERATING_NETWORK, o.as[String]("btc_address"))
        TaskOutput(
          task = TaskRefs.fromMongo(o.as[MongoDBObject]("task")), 
          owner = UserIdents.fromMongo(o.as[MongoDBObject]("owner")),
          btc_address = address.toString
        )
      }
    }

    implicit object Reports extends Collections[Report] with MongoClient {
      override def name = "reports"
      def findReports(taskRef: TaskRef): Seq[Report] = {
        val q = MongoDBObject("task" -> TaskRefs.toMongo(taskRef))        
        db.getCollection(name).find(q).toArray().map(fromMongo)        
      }
      def remove(taskRef: TaskRef, uid: UserIdent): Int = {
        val q = MongoDBObject("task" -> TaskRefs.toMongo(taskRef))
        val result = db.getCollection(name).remove(q)
        result.getN
      }
      override def toMongo[U](u: U)(implicit tag: TypeTag[U]): MongoDBObject = {
        val report = super.toMongo(u)
        val r = u.asInstanceOf[Report]
        report.update("reporter", UserIdents.toMongo(r.reporter))
        report.update("task", TaskRefs.toMongo(r.task))
        report
      }      
      override def fromMongo(o: DBObject): Report = 
        Report(
          reporter = UserIdents.fromMongo(o.as[MongoDBObject]("reporter")),
          task = TaskRefs.fromMongo(o.as[MongoDBObject]("task"))
        )       
    }

    implicit object Payments extends MongoClient {
      def name = "payments"
      
      def findPayments(task: TaskRef): Seq[TaskPayment] = {
        val q = MongoDBObject("output.task" -> TaskRefs.toMongo(task))
        println("Looking for payments on task " + q)
        db.getCollection(name).find(q).toArray().map(Payments.fromMongo)
      }
      
      def toMongo(p: TaskPayment):DBObject = MongoDBObject(
        "tx_hash" -> p.tx_hash.toString,
//        "task" -> TaskRefs.toMongo(p.task_ref),
        "output" -> TaskOutputs.toMongo(p.taskOutput),
//MongoDBObject("owner" -> UserIdents.toMongo(p.paid_by)),
        "value" -> p.value)

      def addPayment(payment: TaskPayment): Int = {
        val taskRef = payment.task_ref
        val result = db.getCollection(name).insert(toMongo(payment))
        if (result.getN > 0) {
          Tasks.bountyIncrease(taskRef, payment.value)          
          result.getN 
        } else 0                
      }

      def fromMongo(o: DBObject): TaskPayment = TaskPayment(
          tx_hash = o.as[String]("tx_hash"),
          taskOutput = TaskOutputs.fromMongo(o.as[MongoDBObject]("output")),
          value = o.as[Double]("value")
      )
    }

    implicit object Rewards extends MongoClient {
      def name = "rewards"
      def toMongo(reward: TaskReward): DBObject = MongoDBObject(
        "tx_hash" -> reward.tx_hash.toString,
        "task" -> MongoDBObject("task_id" -> TaskRefs.toMongo(reward.task_ref)),
        "output" -> MongoDBObject("owner" -> UserIdents.toMongo(reward.rewarded_by)),
        "value" -> reward.value)

      def addReward(reward: TaskReward): Int = {
        val taskRef = reward.task_ref
        val result = db.getCollection(name).insert(toMongo(reward))
        if (result.getN > 0) {
          Tasks.bountyIncrease(taskRef, reward.value)
          Tasks.rewardIncrease(taskRef, reward.value)
        } else 0
      }
    }
  }
}

trait MongoClient { 
  val mongo = MongoClient("localhost", 27017)   
  val db: MongoDB = mongo("bithack")
} 

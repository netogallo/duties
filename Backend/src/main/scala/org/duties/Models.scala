package org.duties

import com.mongodb.casbah.Imports._

import Models._
import Mongo.Collections._

import java.util.Date
import java.util.Calendar
import java.security.Key
import javax.crypto.Cipher
import javax.crypto.spec.{IvParameterSpec, SecretKeySpec}
import javax.xml.bind.DatatypeConverter

import org.bitcoinj.core.{Address, Sha256Hash}

object Models {
  case class Task(name: String, description: Option[String] = None, penalty: Double, expiry_epoch: Long, state: String = "Free", total_bounty: Option[Double] = None, reward_bounty: Option[Double] = None, entrusted: Option[UserIdent] = None, payments: Seq[TaskPayment] = Seq(), reported_by: Seq[UserIdent] = Nil, recurrent: Boolean, id: String = (new ObjectId().toString())) {
    def is_paid = entrusted.isDefined
  }
  case class Duty(author: UserIdent, name: String, participants: Seq[UserIdent], tasks: Seq[Task], id: String = (new ObjectId().toString())) 
  case class User(username: String, password: String, btc_address: String, id: String = (new ObjectId().toString())) {
    def toIdent: UserIdent = UserIdent(username)
  }
  case class UserIdent(username: String)
  case class TaskRef(task_id: String, duty_id: Option[String] = None)  
  case class TaskOutput(owner: UserIdent, task: TaskRef, btc_address: String)
  case class TaskPayment(tx_hash: String, taskOutput: TaskOutput, value: Double){
    def task_ref = taskOutput.task
    def paid_by = taskOutput.owner
  }
  case class TaskReward(tx_hash: String, taskOutput: TaskOutput, value: Double){
    def task_ref = taskOutput.task
    def rewarded_by = taskOutput.owner
  }
  
  case class Report(reporter: UserIdent, task: TaskRef)
  case class Invite(author: UserIdent, advocate: UserIdent, tasks: Seq[TaskRef] = Seq(), duty: Option[Duty] = None, id: String = (new ObjectId().toString()))
}

object Auth {         
  case class AuthCode(code: String)
  case class Auth(username: String, password: String)

  final val COOKIE = "rm"
  final val CBC_SPEC: IvParameterSpec = { new IvParameterSpec(Array[Byte](23, 9, 106, -98, -112, 110, -31, -109, 93, -81, 79, -62, 111, 34, -37, 52)) }
  final val ALGORITHM = Cipher.getInstance("AES/CBC/PKCS5Padding")
  final val key: Key= {
    val keyBytes = "1234567890ABCDEF1234567890DUTIES".getBytes("UTF-8").take(16)
    new SecretKeySpec(keyBytes, "AES")
  }
  def SEPARATOR = ":"
  
  // Cookie-code format.
  def debase64(string: String): Array[Byte] = DatatypeConverter.parseBase64Binary(string)
  def enbase64(bytes: Array[Byte]): String = DatatypeConverter.printBase64Binary(bytes)

  def decrypt(encoded: String): Option[User] = {
    val encrypted_bytes = debase64(encoded)
    ALGORITHM.init(Cipher.DECRYPT_MODE, key, CBC_SPEC)
    val decoded_ttl = new String(ALGORITHM.doFinal(encrypted_bytes)).split(":")
    def fetchUser(ident: String): Option[User] = Users.findUsername(ident)
    val user : Option[User] = decoded_ttl.headOption.flatMap(fetchUser)  
    user
  }
  
  def encrypt(auth: Auth): String = {
    val ttl = Array(auth.username, Calendar.getInstance().hashCode()).mkString(SEPARATOR)
    ALGORITHM.init(Cipher.ENCRYPT_MODE, key, CBC_SPEC)
    val encrypted_bytes = ALGORITHM.doFinal(ttl.getBytes("UTF-8"))
    new String(enbase64(encrypted_bytes))
  }

  def fromUser(u: Models.User) = Auth(u.username, u.password) 
}

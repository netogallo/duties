package org.duties

import com.mongodb.casbah.Imports._

object Models {
  case class Task(name: String, description: Option[String] = None, penalty: Double, entrusted: Option[User] = None, votes: Seq[User] = Nil, recurrent: Boolean, task_id: String = (new ObjectId().toString()))

  case class Duty(author: String, participants: Seq[String], tasks: Seq[Task], id: String = (new ObjectId().toString()))
  
  case class User(username: String, password: String, id: String = (new ObjectId().toString()))
}


object UtilObjects  {
  import java.util.Date
  import java.util.{Base64, Calendar}
  import java.security.Key
  import javax.crypto.Cipher
  import javax.crypto.spec.{IvParameterSpec, SecretKeySpec}

  case class AuthCode(code: String)
  case class Auth(username: String, password: String)
  
  object Auth { 
    def fromUser(u: Models.User) = Auth(u.username, u.password) 

    final val key: Key= {
      val keyBytes = "1234567890ABCDEF1234567890DUTIES".getBytes("UTF-8").take(16)
      new SecretKeySpec(keyBytes, "AES")
    }
    
    final val CBC_SPEC: IvParameterSpec = { new IvParameterSpec(Array[Byte](23, 9, 106, -98, -112, 110, -31, -109, 93, -81, 79, -62, 111, 34, -37, 52)) }
    final val ALGORITHM = Cipher.getInstance("AES/CBC/PKCS5Padding")    
    def SEPARATOR = ":"
    // Cookie-code format.
    def debase64(string: String): Array[Byte] = Base64.getDecoder.decode(string)
    def enbase64(bytes: Array[Byte]): String = new String(Base64.getEncoder.encode(bytes))
    def encrypt(auth: Auth): String = {
      val ttl = Array(auth.username, Calendar.getInstance().hashCode()).mkString(SEPARATOR)
      ALGORITHM.init(Cipher.ENCRYPT_MODE, key, CBC_SPEC)
      val encrypted_bytes = ALGORITHM.doFinal(ttl.getBytes("UTF-8"))
      new String(enbase64(encrypted_bytes))
    }
  }
}

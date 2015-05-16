package org.duties

import org.scalatra._
import nl.captcha._
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;

trait Captchas {
  this: DutyStack =>

  def mkCaptcha = new Captcha.Builder(200, 50)
    .addText()
    .build(); // Required! Always!
  
  get("/captcha") {
    contentType = "image/jpg"
    val captcha = session.get("captcha").getOrElse(mkCaptcha).asInstanceOf[Captcha]
    val image = captcha.getImage()

    val out = new ByteArrayOutputStream()
    ImageIO.write(image, "jpg", out )
    out.flush();
    val imageInByte: Array[Byte] = out.toByteArray()
    out.close();
    imageInByte
  }

  post("/captcha") { 
    try {
      contentType = "application/json"
      val text = params("text")
      val answer = session.get("captcha").get.asInstanceOf[Captcha].getAnswer
      if (!text.equals(answer)) mkError("Invalid captcha")
      else "{}"
    } catch {
      case e: NoSuchElementException => mkError("Missing text argument or you didn't get a captcha first")
    } 
  }
}

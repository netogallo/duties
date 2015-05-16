package org.duties

import org.json4s.jackson.Serialization.{read, write}
import Models._
import Mongo.Collections._

//hasher
import com.roundeights.hasher.Implicits._
import scala.language.postfixOps

trait Homepage {
  this: DutyStack with Captchas => 
  def home = {
    val auth = request.cookies.get(Auth.COOKIE)
    def isLogged = auth.isDefined
    val user = User("kmels", "pw")
    val authRequest = Auth.fromUser(user)
    val captcha = mkCaptcha
    session.put("captcha", captcha)    
    
    val hashedPw = "pw".sha256.hex
    val duty = Duty("kmels", Seq("netogallo", "kmels"), Seq())
    val task = Task(penalty = 1.5d, name = "My task", recurrent = false,
                  description = Some("Optional description"), entrusted = Some("An optional asignee"), votes = Seq(User("An optional user's vote", hashedPw)))

    def loggedUser = maybeAuth
    val invites: Seq[Invite] = if (isLogged) (Invites.findAdvocate(loggedUser.get)) else Nil

    val invite = loggedUser.map(author => Invite(author, "netogallo", Seq(task)))
    <html>
      <body>
        {
        if (auth.isDefined)
          <p>Last auth: {if (auth.isDefined) auth.get else null }
          </p>

          <form method="POST" action="/log-out">
          <input type='submit' value='Logout'/>
          </form>
        }            

        <h1>POST /auth :: JSON -> JSON </h1>
        <form method="POST" action="/auth/form">
          <textarea name='json' rows='4' cols='45'>{write(authRequest)}</textarea>
          <input type='submit' value='Login'/>
        </form>
              
        <h1>GET /captcha :: () -> Image</h1>
        <img src="/captcha"/>
    
        <h1>POST /captcha :: Text -> JSON </h1>
        <form method="POST" action="/captcha">
          <textarea name='text' rows='4' cols='45'>{captcha.getAnswer}</textarea>
          <input type='submit' value='Validate'/>
        </form>
        
        <h1>GET /users :: JSON </h1>
        {find(Users)}

        <h1>POST /user :: JSON -> JSON</h1>
        <form method="POST" action="/user/form">
          <textarea name='json' rows='4' cols='45'>{write(user)}</textarea>
          <input type='submit' value='Create user'/>
        </form>
        
        <h1>GET /duties :: JSON</h1>
        {find(Duties)}

        <h1>POST /duty :: JSON -> JSON</h1>
        <form method="POST" action="/duty/form">
          <textarea name='json' rows='4' cols='45'>{write(duty)}</textarea>
          <input type='submit' value='Create duty'/>
        </form>

        <h1>GET /tasks :: JSON</h1>
        {find(Tasks)}

        <h1>POST /task :: JSON -> JSON</h1>
        <form method="POST" action="/task/form">
          <textarea name='json' rows='9' cols='45'>{write(task)}</textarea>
          <input type='submit' value='Create task'/>
        </form>

        <h1>GET /invites/$user :: JSON -> JSON</h1>        
        {if (!isLogged) 
           <p>"Login to see invites"</p>
         else 
           <p>
             {if (invites.isEmpty) "You don't have invites." else write(invites)}
          </p>
       }

        <form method="POST" action="/invite/form">
          <textarea name='json' rows='9' cols='45'>{write(invite)}</textarea>
          <input type='submit' value='Send invite'/>
        </form>
      </body>
    </html>
  }
}

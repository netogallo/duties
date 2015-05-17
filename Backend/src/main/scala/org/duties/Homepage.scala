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
    val user = User("kmels", "pw")
    val kmels = UserIdent("kmels")
    val netogallo = UserIdent("netogallo")
    val authRequest = Auth.fromUser(user)
    val captcha = mkCaptcha
    session.put("captcha", captcha)    
    
    val hashedPw = "pw".sha256.hex
    val task = Task(penalty = 1.5d, name = "My task", recurrent = false)
    val duty = Duty(kmels, Seq(netogallo, kmels), Seq(task))

    def loggedUser = maybeAuth
    def isLogged = loggedUser.isDefined
    val invites: Seq[Invite] = if (isLogged) (Invites.findAdvocate(loggedUser.get)) else Nil

    val invite = loggedUser.map(author => Invite(UserIdent(author), netogallo, Seq(TaskRef("a task id"))))

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

        <h1>POST /login :: JSON -> JSON </h1>
        <form method="POST" action="/login/form">
          <textarea name='json' rows='4' cols='45'>{write(authRequest)}</textarea>
          <input type='submit' value='Login'/>
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

        <h1>GET /invites :: JSON -> JSON</h1>        
        {if (!isLogged) 
           <p>"Login to see invites"</p>
         else 
           <p>
             {if (invites.isEmpty) "You don't have invites." else write(invites)}
          </p>
       }

        <h1>POST /invite :: JSON -> JSON</h1>
        <form method="POST" action="/invite/form">
          <textarea name='json' rows='9' cols='45'>{write(invite)}</textarea>
          <input type='submit' value='Post invite'/>
        </form>

        <h1>GET /captcha :: () -> Image</h1>
        <img src="/captcha"/>
    
        <h1>POST /captcha :: Text -> JSON </h1>
        <form method="POST" action="/captcha">
          <textarea name='text' rows='4' cols='45'>{captcha.getAnswer}</textarea>
          <input type='submit' value='Validate'/>
        </form>                
      </body>
    </html>
  }
}

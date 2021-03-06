package org.duties

import org.json4s.jackson.Serialization.{read, write}
import Models._
import Mongo.Collections._

//hasher
import com.roundeights.hasher.Implicits._
import scala.language.postfixOps
import java.util.Calendar

trait Homepage {
  this: DutyStack with Captchas => 
  def home = {
    val auth = request.cookies.get(Auth.COOKIE)
    val user = User("kmels", "pw", btc_address = null)
    val kmels = UserIdent("kmels")
    val gg = UserIdent("gg")
    val netogallo = UserIdent("netogallo")
    val authRequest = Auth.fromUser(user)
    val captcha = mkCaptcha
    session.put("captcha", captcha)    
    
    val hashedPw = "pw".sha256.hex
    val now = Calendar.getInstance.getTimeInMillis()
    val tenMinutes = 4 * 60 * 1000;
    val task = Task(penalty = 0.000155d, name = "My task", expiry_epoch = now + tenMinutes, recurrent = false)
    val duty = Duty(kmels, "Our big duty", Seq(netogallo, kmels, gg), Seq(task))

    def loggedUser = maybeAuth
    def isLogged = loggedUser.isDefined
    val invites: Seq[Invite] = if (isLogged) (Invites.findAdvocate(loggedUser.get.username)) else Nil
    val ref = TaskRefs.fromTask(task)
    val refs = Seq(ref)
    val invite = loggedUser.map(author => Invite(kmels, netogallo, Seq(ref)))
    val report = Report(kmels, ref)

    println("loggedUser: " +loggedUser)
    <html>
      <body>
        {
        if (maybeAuth.isDefined)
          {loggedUser}
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

        <h1>POST /tasks :: JSON -> JSON</h1>
        <form method="POST" action="/tasks/form">
          <textarea name='json' rows='9' cols='45'>{write(refs)}</textarea>
          <input type='submit' value='Map refs'/>
        </form>

        <h1>POST /invite :: JSON -> JSON</h1>
        <form method="POST" action="/invite/form">
          <textarea name='json' rows='9' cols='45'>{write(invite)}</textarea>
          <input type='submit' value='Post invite'/>
        </form>

        <h1>POST /report :: JSON -> JSON</h1>
        <form method="POST" action="/report/form">
          <textarea name='json' rows='9' cols='45'>{write(report)}</textarea>
          <input type='submit' value='Post report'/>
        </form>

        <h1>POST /address :: JSON </h1>
        <form method="POST" action="/address/form">
          <textarea name='json' rows='4' cols='45'>{write(ref)}</textarea>
          <input type='submit' value='Get address'/>
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

  def admin = {
    import org.bitcoinj.core._
    import Wallet._
    
    <html>
    <body>
      <h1>Wallet balance: {Bithack.wallet.getBalance(BalanceType.ESTIMATED).toFriendlyString}</h1>
      <h1>Total incoming transactions</h1>
    </body>
    </html>
  }
}

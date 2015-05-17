import org.duties._
import org.scalatra._
import javax.servlet.ServletContext

class ScalatraBootstrap extends LifeCycle {    
  override def init(context: ServletContext) {
    Bithack.connect()
    context.mount(new DutyServlet, "/*")
  }
}

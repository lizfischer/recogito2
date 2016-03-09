package controllers.myrecogito

import controllers.{ AbstractController, Security }
import controllers.Roles._
import javax.inject.Inject
import jp.t2v.lab.play2.auth.AuthElement
import models.user.UserService
import models.content.DocumentService
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import storage.DB

class MyRecogitoController @Inject() (implicit val db: DB) extends AbstractController with AuthElement with Security {

  def index = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    val username = loggedIn.getUsername
    DocumentService.findByUser(username).map(documents => {
      Ok(views.html.myrecogito.index(loggedIn, UserService.getUsedDiskspaceKB(username), documents))
    })
  }

}

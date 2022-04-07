package muffin.webhooks

trait IncomingWebhooks[F[_]] {
  def createIncomingWebhook : Unit


  def incomingWebhooks : Unit

  def incomingWebhook : Unit

  def deleteIncomingWebhook : Unit


  def updateIncomingWebhook : Unit

}


trait OutgoingWebhooks[F[_]] {
  def createOutgoingWebhook : Unit


  def outgoingWebhooks : Unit

  def outgoingWebhook : Unit

  def deleteOutgoingWebhook : Unit


  def updateOutgoingWebhook : Unit

  def regenerateToken : Unit
}

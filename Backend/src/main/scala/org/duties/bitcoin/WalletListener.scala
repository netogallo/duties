package org.duties.bitcoin

import org.bitcoinj.core._
import org.duties._
import Mongo._
import Collections._
import Models._
import scala.collection.JavaConversions._

object WalletListener extends AbstractWalletEventListener with MongoClient {

  def findTaskOutput(adr: Address): Option[TaskOutput] = TaskOutputs.findAddress(adr)
 
  def paymentExists(tx_hash: String): Boolean = {
    Payments.existsTx(tx_hash) || Rewards.existsTx(tx_hash)
  }

  //rewards bounty in bitcoins to entrusted (if defined)
  def rewardEntrusted(task: Task) {        
    println("Rewarding entrusted");

    if (task.state == "Expired"){
      val ref = TaskRefs.find(task.id)
      val btc_address: Option[String] = task.entrusted.flatMap(Users.find).map(_.btc_address)

      if (btc_address.isDefined) {
        val entrustedAddress = new Address(Bithack.OPERATING_NETWORK, btc_address.get)
        if (!task.total_bounty.isDefined) println("ERROR: TOTAL BOUNTY NOT DEFINED: TASK ID = "+ task.id)
        else {
          val df = new java.text.DecimalFormat("#.000000000");
          val totalBounty = task.total_bounty.get
          val coin = Coin.parseCoin(df.format(totalBounty))
          Bithack.sendMoney(coin, entrustedAddress)
          Tasks.setRewarded(task.id)
        }                
      } else {
        println("ERROR: ADDRESS FOR ENTRUSTED IS NOT DEFINED. EXPIRATION WILL NOT CAUSE PAYMENT")
      }
    
    }
  }
  
  //finds tasks that are not reported and distribuites the given task reward equally
  def collectBounty(task: Task, duty: Duty) {
    //entrusted should be existent and other tasks in state entrusted
    
    val entrustedTasks = duty.tasks.filter(_.state == "Entrusted")
    println("Distributing among " + entrustedTasks)
    
    
  }

  //insert payment, mark as paid or add rewards
  def mkPayment(payment: TaskPayment): Either[TaskPayment, TaskReward] = {
    val taskOutput = payment.taskOutput
    val t: Option[Task] = Tasks.fromRef(taskOutput.task)
    val txHash = payment.tx_hash
    val value = payment.value
    //if we didn't find any task, return silently
    if (!t.isDefined) Right(TaskReward(txHash, taskOutput, 0))
    else {
      val task = t.get
      val is_paid = task.entrusted.isDefined
      val paymentValue = payment.value
      val outputOwner = taskOutput.owner

      if (!is_paid) {
        val penalty = task.penalty
        val owned_by_owner = penalty 
        
        if (value >= penalty){
          println("Detected entrusted. ")
          Payments.addPayment(payment)
          Tasks.setEntrusted(task, outputOwner)
          Left(payment)
        } else  { 
          println("Detected payment! ")
          Payments.addPayment(payment); Left(payment) 
        }
      } else {
        println("Detected reward!!! ")
        val reward = TaskReward(payment.tx_hash, payment.taskOutput, payment.value)
        println("Adding")
        Rewards.addReward(reward)
        Right(reward)
      }
    }
 }
            
  override def onCoinsReceived(w: Wallet, tx: Transaction, prevBalance: Coin, newBalance: Coin): Unit = {    
    val value = tx.getValue(w).toPlainString.toDouble //BTC

    val btc_outputs = tx.getWalletOutputs(w)
    def getAddress: TransactionOutput => Address = out =>  out.getAddressFromP2PKHScript(Bithack.OPERATING_NETWORK)
    val btc_addresses = btc_outputs map getAddress
    
    val payments = btc_addresses.flatMap(btc_adr => {      
      println("Recibimos en address: " + btc_adr.toString + "; buscando ...")
      val taskOutput = findTaskOutput(btc_adr)
      
      if (taskOutput.isDefined){        
        val out = taskOutput.get        
        //val task = Tasks.fromRef(taskOutput)
        //val penalty = task.map(_.penalty)
        val taskPayment = TaskPayment(tx.getHash.toString, out, value)
        val deposit = mkPayment(taskPayment)
        Some(taskPayment)
      } else {
        println("WE RECEIVED A TASK PAYMENT TO THE WALLET FROM NO WHERE: " + btc_addresses.mkString(",") + "; " + tx.getHash.toString)
        None
      }
    })
  }
}

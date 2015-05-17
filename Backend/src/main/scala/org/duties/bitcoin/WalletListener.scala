package org.duties.bitcoin

import org.bitcoinj.core._
import org.duties._
import Mongo._
import Collections._
import Models._
import scala.collection.JavaConversions._

object WalletListener extends AbstractWalletEventListener with MongoClient {

  
  override def onCoinsReceived(w: Wallet, tx: Transaction, prevBalance: Coin, newBalance: Coin): Unit = {
    
    val value = tx.getValue(w)    
    val outputs = tx.getWalletOutputs(w)
    val addresses = outputs.map(_.getAddressFromP2PKHScript(Bithack.OPERATING_NETWORK))

    val refs: Seq[Option[TaskRef]] = addresses.map(adr => TaskAddresses.findAddress(adr))
    
    println(refs)

    val tasks: Seq[Option[Task]] = refs.flatMap(r => {
      r.map(Tasks.fromRef)
    })
    
    println(tasks)
    
  }
}

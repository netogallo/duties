package org.duties.bitcoin

import org.bitcoinj.core._
import org.duties._
import Mongo._
import Collections._
import Models._
import scala.collection.JavaConversions._

object WalletListener extends AbstractWalletEventListener with MongoClient {

  def findTaskOutput(adr: Address): Option[TaskPayment] = ???
  
  override def onCoinsReceived(w: Wallet, tx: Transaction, prevBalance: Coin, newBalance: Coin): Unit = {
    
    val value = tx.getValue(w)    
    val btc_outputs = tx.getWalletOutputs(w)
    def getAddress: TransactionOutput => Address = out =>  out.getAddressFromP2PKHScript(Bithack.OPERATING_NETWORK)

    val btc_addresses = btc_outputs map getAddress
    
    val payments = btc_addresses.flatMap(btc_adr => {      
      findTaskOutput(btc_adr)
    })

    
  }
}

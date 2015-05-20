package org.duties

import org.bitcoinj.core._
import org.bitcoinj.net.discovery.DnsDiscovery
import org.bitcoinj.params._
import org.bitcoinj.store.PostgresFullPrunedBlockStore
import org.bitcoinj.wallet.KeyChain
import java.util.concurrent.TimeUnit
import java.io.File
import java.net.InetAddress

object Bithack {
  final val OPERATING_NETWORK = MainNetParams.get
  final val BLOCKSTORE = new PostgresFullPrunedBlockStore(OPERATING_NETWORK, 1000, "localhost", "bithacks_mainnet", "postgres", "postgres")
  final val BLOCKCHAIN = new BlockChain(OPERATING_NETWORK, BLOCKSTORE)
  final val NETWORK: PeerGroup = new PeerGroup(OPERATING_NETWORK, BLOCKCHAIN)
  final val walletFile = new File("bithack_main.wallet")
  final val wallet: Wallet = if (!walletFile.exists()) new Wallet(OPERATING_NETWORK) else Wallet.loadFromFile(walletFile)    
  
  addWallet(wallet, walletFile)

  // Adds a wallet to the network by reading the given file
  //def addWallet(file: File): Unit = {
  //  addWallet(WalletStore.parseWalletFromFile(file), file)
  //}

  // Connect to the bitcoin network as a node
  def connect(): Unit = {
    NETWORK.addAddress(new PeerAddress(InetAddress.getLocalHost()))
    NETWORK.addPeerDiscovery(new DnsDiscovery(OPERATING_NETWORK))
    NETWORK.start()
    new Thread(new Runnable {
      def run() {
        //NETWORK.downloadBlockChain()
      }
    }).start
  }

  // Adds a wallet to the network
  def addWallet(wallet: Wallet, file: File): Unit = {
    BLOCKCHAIN.addWallet(wallet)
    NETWORK.addWallet(wallet)
    wallet.autosaveToFile(file, 2, TimeUnit.SECONDS, null)
    wallet.addEventListener(bitcoin.WalletListener)
  }

  def mkReceivingAddress = wallet.freshAddress(KeyChain.KeyPurpose.RECEIVE_FUNDS)

  def sendMoney(btc: Coin, adr: Address){
  }
}

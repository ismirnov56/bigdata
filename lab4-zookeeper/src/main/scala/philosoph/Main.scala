package philosoph

import java.util.concurrent.Semaphore

import org.apache.zookeeper.{CreateMode, ZooDefs, ZooKeeper}

import scala.util.Random

object Main {
  def main(args: Array[String]): Unit = {
    val philosophersCount = 5
    val Seq(hostPort) = args.toSeq
    val seats = philosophersCount - 1

    val zk = new ZooKeeper(hostPort, 3000, (event)=>{})
    zk.create("/philosophers", Array.emptyByteArray,
      ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT)
    val forks = new Array[Semaphore](philosophersCount)
    for (j <- 0 until philosophersCount){
      forks(j) = new Semaphore(1)
    }

    val threads = new Array[Thread](philosophersCount)
    for (id <- 0 until philosophersCount){
      threads(id) = new Thread(
        new Runnable {
          def run(): Unit = {
            val rightId = (id + 1) % philosophersCount
            val philosopher = Philosopher(id, hostPort, "/philosophers", forks(id), forks(rightId), seats)
            for (j <- 1 to (Random.nextInt(3)+2)) {
              philosopher.eat()
              philosopher.think()
            }
          }
        }
      )
      threads(id).setDaemon(false)
      threads(id).start()
    }
    for (id <- 0 until philosophersCount){
      threads(id).join()
    }
  }
}

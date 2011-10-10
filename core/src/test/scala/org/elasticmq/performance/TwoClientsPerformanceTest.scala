package org.elasticmq.performance

import org.elasticmq._

object TwoClientsPerformanceTest {
  // Slows down, useful for debugging:
  // org.apache.log4j.BasicConfigurator.configure();

  val node = NodeBuilder.withMySQLStorage("elasticmq", "root", "").build()
  val client = node.nativeClient

  val testQueueName = "twoClientsPerformanceTest"
  val testQueue = {
    client.queueClient.lookupQueue(testQueueName) match {
      case Some(queue) => queue
      case None => client.queueClient.createQueue(Queue(testQueueName, MillisVisibilityTimeout(10000)))
    }
  }

  def shutdown() {
    node.shutdown()
  }

  def timeWithOpsPerSecond(name: String, block: Int => Int) {
    val start = System.currentTimeMillis()
    val ops = block(0)
    val end = System.currentTimeMillis()

    val seconds = (end - start) / 1000

    println(name+" took: "+seconds)
    println(name+" ops/second: "+(ops/seconds))
    println(name+" ops: "+ops)
  }

  object Receiver {
    def run() {
      def receiveLoop(count: Int): Int = {
        client.messageClient.receiveMessage(testQueue, DefaultVisibilityTimeout) match {
          case Some(message) => {
            client.messageClient.deleteMessage(message)
            receiveLoop(count+1)
          }
          case None => count
        }
      }

      timeWithOpsPerSecond("Receive", receiveLoop _)

      shutdown()
    }
  }

  object Sender {
    def run(iterations: Int) {
      timeWithOpsPerSecond("Send", _ => {
        for (i <- 1 to iterations) {
          client.messageClient.sendMessage(Message(testQueue, "message"+i))
        }

        iterations
      })

      shutdown()
    }
  }
}

object TwoClientsPerformanceTestReceiver {
  def main(args: Array[String]) {
    println("Press any key to start ...")
    readLine()
    TwoClientsPerformanceTest.Receiver.run()
  }
}

object TwoClientsPerformanceTestSender {
  def main(args: Array[String]) {
    TwoClientsPerformanceTest.Sender.run(1000)
  }
}

object TwoClientsPerformanceTestSendAndReceive {
  def main(args: Array[String]) {
    TwoClientsPerformanceTest.Sender.run(1000)
    TwoClientsPerformanceTest.Receiver.run()

    println()
    println("---")
    println()

    TwoClientsPerformanceTest.Sender.run(10000)
    TwoClientsPerformanceTest.Receiver.run()
  }
}
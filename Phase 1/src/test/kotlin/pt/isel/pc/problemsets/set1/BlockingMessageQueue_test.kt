package PC.serie1
import BlockingMessageQueue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.*
import kotlin.concurrent.thread
import kotlin.time.Duration.Companion.seconds

class BlockingMessageQueue {
    @Test
    fun filling_the_queue(){
        //simple feeling the queue with no pendentEnqueues as a simple test
        var boolean: Boolean? = null
        val queue = BlockingMessageQueue<String>(5)
        val thread1 = thread {
            for (i in 1..5) {
                boolean = queue.tryEnqueue("AA", 2.seconds)
            }
        }
        thread1.join()
        assertEquals(true, boolean)
        val thread2 = thread {
            boolean = queue.tryEnqueue("AA", 2.seconds)
        }
        thread2.join()
        assertEquals(false, boolean)
    }
    @Test
    fun removing_from_queue(){
        //simple test to remove from queue
        var boolean: Boolean? = null
        val number = 0
        var list: List<String>? = null
        val queue = BlockingMessageQueue<String>(5)
        val thread1 = thread {
            for (i in 1..5) {
                boolean = queue.tryEnqueue((number+i).toString(), 2.seconds)
            }
        }
        thread1.join()
        val thread2 = thread {
            list = queue.tryDequeue(3, 2.seconds)
        }
        thread2.join()
        assertEquals(listOf("1","2","3"), list)
    }
    @Test
    fun PendingEnqueues_test(){
        //longer test checking pendentEnques being added to queue after a Dequeue etc...
        var boolean: Boolean? = null
        val number = 0
        var list: List<String>? = null
        val queue = BlockingMessageQueue<String>(5)
        val thread1 = thread {
            for (i in 1..8) {
                boolean = queue.tryEnqueue((number+i).toString(), 10.seconds)
                println("numero $i inserido")
            }
        }
        val thread2 = thread {
            println("                          à espera")
            Thread.sleep(5000)
            list = queue.tryDequeue(3, 2.seconds)
            println("                          peguei 3")
            println()
            println()
        }
        thread1.join()
        thread2.join()
        thread2.interrupt()
        thread1.interrupt()

        assertEquals(listOf("1","2","3"), list) // queue -> 4,5,6,7,8

        val thread3 = thread {
            Thread.sleep(2000)
            list = queue.tryDequeue(2, 2.seconds)
            println("                          peguei 2")
            println()
            println()
        }
        thread3.join()
        thread3.interrupt()

        assertEquals(listOf("4","5"), list) // queue -> 6,7,8
        assertEquals(true, boolean)

        val thread4 = thread {
            Thread.sleep(2000)
            for (i in 9..10) {
                boolean = queue.tryEnqueue((number+i).toString(), 1.seconds)
                println("numero $i inserido")
            }
        }
        val thread5 = thread {
                println("                          em espera")
                list = queue.tryDequeue(5, 10.seconds)
                println("                          peguei 5")
        }
        thread4.join()
        thread5.join()
        thread4.interrupt()
        thread5.interrupt()
        assertEquals(listOf("6","7","8","9","10"), list) // queue -> 6,7,8
        assertEquals(true, boolean)

        val thread6 = thread {
            for (i in 11..15) {
                boolean = queue.tryEnqueue((number+i).toString(), 1.seconds)
                println("numero $i inserido")
            }
        }
        val thread7 = thread {
            thread6.interrupt()
        }
        thread7.join()
        thread6.join()
        val thread8 = thread {
            Thread.sleep(1000)
            println("                          em espera")
            list = queue.tryDequeue(1, 10.seconds)
            println("                          peguei 1")
        }
        thread8.join()
        assertEquals(listOf("11"), list)
        assertEquals(true, boolean)
    }
}
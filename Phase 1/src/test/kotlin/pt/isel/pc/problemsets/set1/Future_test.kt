package PC.serie1
import BlockingMessageQueue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.*
import java.util.concurrent.Callable
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.concurrent.thread
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class Future_test{
    @Test
    fun future_both_get(){
        val executer = ThreadPoolExecutor(3, 5.seconds)
        val callable1 = Callable {
            Thread.sleep(1000)
            return@Callable 1
        }
        val callable2 = Callable {
            Thread.sleep(3000)
            return@Callable 2
        }

        val result1 = executer.execute(callable1)
        val result2 = executer.execute(callable2)
        assertEquals(1, result1.get())
        assertEquals(2, result2.get())
        assertEquals(2, result2.get(10000, TimeUnit.MILLISECONDS))
    }
    @Test
    fun cancels_Tests(){
        val executer = ThreadPoolExecutor(1, 2.seconds)
        val callable1 = Callable {
            Thread.sleep(6000)
            println("nao deve dar print")
            return@Callable 1
        }
        val callable2 = Callable {
            Thread.sleep(1000)
            println("deve dar print")
            return@Callable 2
        }
        val result1 = executer.execute(callable1)
        val result2 = executer.execute(callable2)
        val thread1 = thread {
            Thread.sleep(2000)
            assertTrue {result1.cancel(false)}
        }
        val thread2 = thread {
            Thread.sleep(2000)
            assertFalse{result2.cancel(false)}
        }
        thread1.join();thread2.join()
        assertTrue{result1.isCancelled}
        assertFalse{result2.isCancelled}
    }
}
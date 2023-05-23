package PC.serie1
import BlockingMessageQueue
import ThreadPoolExecutor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.*
import java.util.concurrent.RejectedExecutionException
import kotlin.concurrent.thread
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.seconds

class ThreadPoolExecutor_test{
    @Test
    fun executing_tasks(){
        //we can see both first being executed and than waiting the 5 seconds of keep alive time for the next one,
        //the assert expected result from the awaittermination was true since we have 15 seconds for 5+5 seconds of
        //keepalivetime plus the runningtime itself
        val executer = ThreadPoolExecutor(2, 5.seconds)
        executer.execute {
            println("first")
        }
        executer.execute{
            println("second")
        }
        executer.execute{
            println("third")

        }
        assertEquals(true, executer.awaitTermination(15.seconds))
    }
    @Test
    fun expiring_time(){ // using 1 poolsize, 2 keepalivetime and "println", we can see that as expected it fully
        //execute the first one only, starting the second but not finishing it up because of the 3 seconds of the
        //keep alive time
        //também nao acontece a ultima execuçao sendo o shutdown implicito no awaittermination
        val executer = ThreadPoolExecutor(1, 2.seconds)
        executer.execute {
            println("first")
        }
        executer.execute{
            print("second")
        }
        executer.execute{
            print("third")
        }
        assertEquals(false, executer.awaitTermination(3.seconds))
        executer.execute{
            print("nao deve dar porque deu shutdown")
        }
    }
}
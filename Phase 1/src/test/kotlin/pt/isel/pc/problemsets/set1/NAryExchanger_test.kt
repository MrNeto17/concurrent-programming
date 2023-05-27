package PC.serie1
import org.junit.jupiter.api.Assertions.assertEquals
import kotlin.time.Duration
import org.junit.jupiter.api.*
import kotlin.concurrent.thread
import kotlin.time.Duration.Companion.seconds

class NAryExchanger_test {
    private fun <T> exchanging(msgs: List<T>, tmout: List<Duration>, size: Int): List<T>? {
        val exchanger = NAryExchanger<T>(size)
        var list: List<T>? = null
        for (m in msgs) {
            val thread = thread {
                list = exchanger.exchange(m, tmout[msgs.indexOf(m)])
            }
            thread.join()
        }
        return list
    }

    @Test
    fun returning_all_values_sucess(): Unit {
        val msgs = mutableListOf("hello", "world", "testing")
        val tmouts = mutableListOf(3.seconds, 3.seconds, 3.seconds)
        assertEquals(listOf("hello", "world", "testing"), exchanging(msgs, tmouts, 3))

        msgs.add("retesting")
        tmouts.add(3.seconds)
        assertEquals(listOf("hello", "world", "testing", "retesting"), exchanging(msgs, tmouts, 4))

        msgs.removeAt(0)
        tmouts.removeAt(0)
        assertEquals(listOf("world", "testing", "retesting"), exchanging(msgs, tmouts, 3))
    }

    @Test
    fun timeout_is_zero(): Unit {
        val msgs = mutableListOf("hello", "world", "testing")
        val tmouts = mutableListOf(3.seconds, 3.seconds, 0.seconds)
        assertEquals(null, exchanging(msgs, tmouts, 2))
    }
    @Test
    fun waiting_until_timeout(): Unit {
        val msgs = mutableListOf("hello", "world", "testing")
        val tmouts = mutableListOf(3.seconds, 3.seconds, 2.seconds)
        assertEquals(null, exchanging(msgs, tmouts, 4))

        msgs.add("retesting")
        tmouts.add(2.seconds)
        assertEquals(null, exchanging(msgs, tmouts, 5))

        msgs.removeAt(0)
        tmouts.removeAt(0)
        assertEquals(null, exchanging(msgs, tmouts, 4))
    }
}
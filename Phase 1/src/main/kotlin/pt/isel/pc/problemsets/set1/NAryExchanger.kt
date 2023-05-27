package PC.serie1
import kotlin.time.Duration
import isel.leic.pc.utils.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

//monitor style
class NAryExchanger<T>(private var groupSize: Int) {
    private val monitor = ReentrantLock()
    private val waiters = monitor.newCondition()
    private val list = mutableListOf<T>()
    @Throws(InterruptedException::class)
    fun exchange(value: T, timeout: Duration): List<T>? {
        monitor.withLock {
            //fast path checking if it is last thread or timeout is passed as 0
            if (timeout.isZero) return null
            if (groupSize == 1){ // last thread?
                groupSize--
                list.add(value)
                return list
            }
            val tmWrapper = MutableTimeout(timeout)
            do {
                try {
                    waiters.await(tmWrapper)
                    if (groupSize > 0) { // space in groupsize?
                        groupSize-- //reduce groupsize and adds the value to list
                        list.add(value)
                        if (groupSize == list.size) return list //if not last thread dont return list,
                        // this way it keeps being blocked until there is no groupsize or timeout elapse
                    }
                    if (tmWrapper.elapsed) return null
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    if (groupSize > 0) waiters.signalAll()
                    throw e
                }
            }
                while (true)
        }
    }
}



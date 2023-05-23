package PC.serie1
import kotlin.time.Duration
import isel.leic.pc.utils.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class NAryExchanger<T>(private var groupSize: Int) {
    private val monitor = ReentrantLock()
    private val waiters = monitor.newCondition()
    private val list = mutableListOf<T>()
    @Throws(InterruptedException::class)
    fun exchange(value: T, timeout: Duration): List<T>? {
        monitor.withLock {
            if (timeout.isZero) return null
            if (groupSize == 1){ // ultima thread?
                groupSize--
                list.add(value)
                return list
            }
            val tmWrapper = MutableTimeout(timeout)
            do {
                try {
                    waiters.await(tmWrapper)
                    if (groupSize > 0) { // threads em falta?
                        groupSize-- // reduz groupsize de threads à espera
                        list.add(value) // adiciona à lista o value tratado pela thread atual
                        if (groupSize == list.size) return list
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


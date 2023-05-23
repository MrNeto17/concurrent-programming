import isel.leic.pc.utils.MutableTimeout
import isel.leic.pc.utils.await
import java.util.*
import java.util.concurrent.Callable
import kotlin.time.Duration
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock

class ThreadPoolExecutor(
    private val maxThreadPoolSize: Int,
    private val keepAliveTime: Duration,
) {
    private var shutdown = false
    private val monitor = ReentrantLock()
    private val waiters = monitor.newCondition()
    private val pendingWork = LinkedList<Runnable>()
    private val workersList = LinkedList<Runnable>()

    @Throws(RejectedExecutionException::class)
    private fun safeRun(work: Runnable) {
        try {
            if (shutdown) throw RejectedExecutionException()
            work.run()
        } catch (t: RejectedExecutionException) {
            throw RejectedExecutionException()
        }
    }

    private fun workerFunction(work: Runnable) {
        do {
            safeRun(work) //corre a thread e verif ica RejectedExecutionException para caso de shutdown (por exemplo)
            monitor.withLock {
                val tmWrapper = MutableTimeout(keepAliveTime)
                waiters.await(tmWrapper)
                if (pendingWork.size > 0 && tmWrapper.elapsed) { //caso haja pending work e acabe keepalivetime
                    workerFunction(pendingWork.poll()) //corre-se dentro da mesma thread um novo runnable
                }
                if (tmWrapper.elapsed && pendingWork.size == 0) {
                    workersList.remove(work)
                    return
                }
            }

        }
        while(true)

    }
    fun execute(runnable: Runnable): Unit {
        monitor.withLock {
            if (workersList.size < maxThreadPoolSize) { // se houver espaço para mais workers
                workersList.add(runnable) // acrescenta runnable na lista de workers e inicializa a thread
                thread {
                    workerFunction(runnable)
                }
            } else { // caso nao haja
                pendingWork.add(runnable) // runnable adicionado a pending work
            }
        }
    }
        fun shutdown(): Unit {
            shutdown = true
        }

        @Throws(InterruptedException::class)
        fun awaitTermination(timeout: Duration): Boolean {
            monitor.withLock {
                val tmWrapper = MutableTimeout(timeout)
                do {
                    try {
                        waiters.await(tmWrapper)
                        if (workersList.isEmpty()) {
                            return true
                        }
                        if (tmWrapper.elapsed) {
                            return false
                        }
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                        throw e
                    }
                }
                    while(true)
            }
        }
}


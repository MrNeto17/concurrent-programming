package PC.serie1

import PC.serie1.Future
import isel.leic.pc.utils.MutableTimeout
import isel.leic.pc.utils.await
import java.util.*
import java.util.concurrent.Callable
import kotlin.time.Duration
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock

//monitor style
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
        try { //run the new runnable inside the thread only if executer ins't shutted down
            if (shutdown) throw RejectedExecutionException()
            work.run()
        } catch (t: RejectedExecutionException) {
            throw RejectedExecutionException()
        }
    }

    private fun workerFunction(work: Runnable) {
        do {
            safeRun(work)
            monitor.withLock {
                val tmWrapper = MutableTimeout(keepAliveTime)
                waiters.await(tmWrapper)
                //if the keepalive time is elapsed we either call the function again to the next pending work
                //if exists or we remove the Runnable from the workerList
                if (pendingWork.size > 0 && tmWrapper.elapsed) {
                    workerFunction(pendingWork.poll())
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
            //if there is space in ThreadPool add a runnable to workersList and run it on the Thread
            //else add it to pendingWork
            if (workersList.size < maxThreadPoolSize) {
                workersList.add(runnable)
                thread {
                    workerFunction(runnable)
                }
            } else {
                pendingWork.add(runnable)
            }
        }
    }
        fun shutdown(): Unit { //just turns the shutdownflag true
            shutdown = true
        }

        @Throws(InterruptedException::class)
        fun awaitTermination(timeout: Duration): Boolean {
            monitor.withLock {
                val tmWrapper = MutableTimeout(timeout)
                do {
                    try {
                        //wait a certain "timeout" returning true if every running executable finishes
                        //or false if the timeout elapsed
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
                } while (true)
            }
        }
    fun <T> execute(callable: Callable<T>) = Future.execute(callable)
}



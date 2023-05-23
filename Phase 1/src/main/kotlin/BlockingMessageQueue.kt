import kotlin.time.Duration
import isel.leic.pc.utils.*
import java.util.*
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


class BlockingMessageQueue<T>(private val capacity: Int) {
    private val monitor = ReentrantLock()
    private val waiters = monitor.newCondition()
    private val queue = LinkedList<T>()

    private class PendingEnqueue<T>(val message: T, val cond: Condition) {
        var isDone = false
    }

    private val pendingEnqueues = LinkedList<PendingEnqueue<T>>()

    private fun doneTransfers() {
        while (pendingEnqueues.size > 0 && queue.size < capacity) {
            with(pendingEnqueues.poll()) {
                isDone = true
                cond.signal()
            }
        }
    }
        @Throws(InterruptedException::class)
        fun tryEnqueue(message: T, timeout: Duration): Boolean {
            monitor.withLock {
                if (pendingEnqueues.isEmpty() && queue.size < capacity) {
                    queue.addLast(message)
                    return true
                }
                if (timeout.isZero) return false
                val tmWrapper = timeout.dueTime()
                val lastPending = PendingEnqueue(message, monitor.newCondition())
                pendingEnqueues.add(lastPending)
                do {
                    try {
                        lastPending.cond.await(tmWrapper)
                        doneTransfers()
                        if (lastPending.isDone) {
                            queue.addLast(lastPending.message)
                            return true
                        }
                        if (tmWrapper.isPast) {
                            pendingEnqueues.remove(lastPending)
                            return false
                        }
                    } catch (e: InterruptedException) {
                        if (lastPending.isDone) {
                            Thread.currentThread().interrupt()
                        return true
                        }
                        pendingEnqueues.remove(lastPending)
                    }
                } while (true)
            }
        }

        @Throws(InterruptedException::class)
        fun tryDequeue(nOfMessages: Int, timeout: Duration): List<T>? {
            monitor.withLock {
                if (nOfMessages <= queue.size) {
                    val list = mutableListOf<T>()
                    for (i in 0 until nOfMessages) {
                        list.add(queue.poll())
                    }
                    return list
                }
                if (timeout.isZero) return null
                val timeWrapper = MutableTimeout(timeout)
                do {
                    try {
                        waiters.await(timeWrapper)
                        if (nOfMessages <= queue.size) {
                            val list = mutableListOf<T>()
                            for (i in 0 until nOfMessages) {
                                list.add(queue.poll())
                            }
                            return list
                        }
                        if (timeWrapper.elapsed) {
                            return null
                        }
                    } catch (e: InterruptedException) {
                        if (nOfMessages <= queue.size) {
                            Thread.currentThread().interrupt()
                            waiters.signalAll()
                            throw e
                        }
                    }
                } while (true)
            }


        }
    }


import kotlin.time.Duration
import isel.leic.pc.utils.*
import java.util.*
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

//monitor and kernel style
class BlockingMessageQueue<T>(private val capacity: Int) {
    private val monitor = ReentrantLock()
    private val waiters = monitor.newCondition()
    private val queue = LinkedList<T>()

    private class PendingEnqueue<T>(val message: T, val cond: Condition) {
        var isDone = false
    }
    //pending queue linkedlist stores whats waiting for space in the queue
    private val pendingEnqueues = LinkedList<PendingEnqueue<T>>()

    private fun doneTransfers() {
        //if there is any pendingEnqueue and enough capacity in queue isDone turns true
        //and it removes it from the head, this way we keep it FIFO as asked
        //isDone will later have impact on the condition to tryEnque turn True
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
                //fast path
                //if there isn't any pending Enquees and enough space in queue add message
                if (pendingEnqueues.isEmpty() && queue.size < capacity) {
                    queue.addLast(message)
                    return true
                }
                if (timeout.isZero) return false
                val tmWrapper = timeout.dueTime()
                //adding a request of Enqueue to PendingEnqueue
                val lastPending = PendingEnqueue(message, monitor.newCondition())
                pendingEnqueues.add(lastPending)
                do {
                    try {
                        lastPending.cond.await(tmWrapper)
                        //making isDone of lastPending true with the condition in doneTransfers
                        doneTransfers()
                        if (lastPending.isDone) { //if its done add it and return true
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
                //fast path checking if we have enough messages to dequeue
                if (nOfMessages <= queue.size) {
                    val list = mutableListOf<T>() //list of taken messages
                    for (i in 0 until nOfMessages) {
                        list.add(queue.poll())
                    }
                    return list
                }
                if (timeout.isZero) return null
                val timeWrapper = MutableTimeout(timeout)
                do {
                    try {
                        //waiting the timeout while checking if there is enough messages to be taken meanwhile
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


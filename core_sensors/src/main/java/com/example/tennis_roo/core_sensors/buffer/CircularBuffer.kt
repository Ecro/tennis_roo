package com.example.tennis_roo.core_sensors.buffer

import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * A thread-safe circular buffer implementation for storing sensor data.
 * This buffer maintains a fixed-size window of the most recent data points.
 *
 * @param T The type of data to be stored in the buffer
 * @param capacity The maximum number of elements the buffer can hold
 */
class CircularBuffer<T>(private val capacity: Int) {
    private val buffer = ArrayList<T>(capacity)
    private var head = 0
    private var size = 0
    private val lock = ReentrantReadWriteLock()
    
    /**
     * Adds an element to the buffer. If the buffer is full, the oldest element is overwritten.
     *
     * @param element The element to add
     */
    fun add(element: T) {
        lock.write {
            if (size < capacity) {
                buffer.add(element)
                size++
            } else {
                buffer[head] = element
                head = (head + 1) % capacity
            }
        }
    }
    
    /**
     * Gets a copy of all elements in the buffer in chronological order (oldest to newest).
     *
     * @return A list containing all elements in chronological order
     */
    fun getAll(): List<T> {
        return lock.read {
            if (size == 0) {
                emptyList()
            } else {
                val result = ArrayList<T>(size)
                for (i in 0 until size) {
                    val index = (head + i) % size
                    result.add(buffer[index])
                }
                result
            }
        }
    }
    
    /**
     * Gets the most recent n elements from the buffer.
     *
     * @param n The number of elements to retrieve
     * @return A list containing the n most recent elements, or all elements if n > size
     */
    fun getRecent(n: Int): List<T> {
        return lock.read {
            if (size == 0 || n <= 0) {
                emptyList()
            } else {
                val count = minOf(n, size)
                val result = ArrayList<T>(count)
                for (i in 0 until count) {
                    val index = (head - 1 - i + size) % size
                    result.add(buffer[index])
                }
                result
            }
        }
    }
    
    /**
     * Clears all elements from the buffer.
     */
    fun clear() {
        lock.write {
            buffer.clear()
            head = 0
            size = 0
        }
    }
    
    /**
     * Gets the current number of elements in the buffer.
     *
     * @return The number of elements
     */
    fun size(): Int {
        return lock.read { size }
    }
    
    /**
     * Checks if the buffer is empty.
     *
     * @return true if the buffer is empty, false otherwise
     */
    fun isEmpty(): Boolean {
        return lock.read { size == 0 }
    }
    
    /**
     * Checks if the buffer is full.
     *
     * @return true if the buffer is full, false otherwise
     */
    fun isFull(): Boolean {
        return lock.read { size == capacity }
    }
}

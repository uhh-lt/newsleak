package uhh_lt.newsleak.util;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * AtomicCounter to concurrently count up indexes.
 */
public class AtomicCounter {

	/** A concurrent integer. */
	private AtomicInteger c = new AtomicInteger(0);

	/**
	 * Increment.
	 */
	public void increment() {
		c.incrementAndGet();
	}

	/**
	 * Decrement.
	 */
	public void decrement() {
		c.decrementAndGet();
	}

	/**
	 * The value of the counter.
	 *
	 * @return the int
	 */
	public int value() {
		return c.get();
	}
}
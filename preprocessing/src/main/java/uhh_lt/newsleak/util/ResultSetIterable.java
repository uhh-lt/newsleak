package uhh_lt.newsleak.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * The Class ResultSetIterable provides stream processing of result sets.
 *
 * @param <T> the generic type
 */
public class ResultSetIterable<T> implements Iterable<T> {

	/** The rs. */
	private final ResultSet rs;
	
	/** The process next. */
	private final Function<ResultSet, T> processNext;

	/**
	 * Instantiates a new result set iterable.
	 *
	 * @param rs the rs
	 * @param processNext the process next
	 */
	public ResultSetIterable(ResultSet rs, Function<ResultSet, T> processNext){
		this.rs = rs;
		// processNext is the mapper function to handle the fetched resultSet
		this.processNext = processNext;
	}


	/* (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<T> iterator() {

		try {
			return new Iterator<T>() {

				// the iterator state is initialized by calling next() to 
				// know whether there are elements to iterate
				boolean hasNext = rs.next();

				@Override
				public boolean hasNext() {
					return hasNext;
				}

				@Override
				public T next() {

					T result = processNext.apply(rs);
					//after each get, we need to update the hasNext info
					try {
						hasNext = rs.next();
					} catch (SQLException e) {
						throw new RuntimeException(e);
					}
					return result;
				}
			};
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Stream.
	 *
	 * @return the stream
	 */
	public Stream<T> stream() {
		return StreamSupport.stream(this.spliterator(), false);
	}
}
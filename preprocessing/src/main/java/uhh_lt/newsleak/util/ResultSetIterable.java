package uhh_lt.newsleak.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ResultSetIterable<T> implements Iterable<T> {

	private final ResultSet rs;
	private final Function<ResultSet, T> processNext;

	public ResultSetIterable(ResultSet rs, Function<ResultSet, T> processNext){
		this.rs = rs;
		// processNext is the mapper function to handle the fetched resultSet
		this.processNext = processNext;
	}


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

	public Stream<T> stream() {
		return StreamSupport.stream(this.spliterator(), false);
	}
}
package uhh_lt.newsleak.util;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Allows for sorting of maps by its contained values in increasing and
 * decreasing order.
 */
public class MapUtil {

	/**
	 * Sort by value (increasing).
	 *
	 * @param <K>
	 *            the key type
	 * @param <V>
	 *            the value type
	 * @param map
	 *            the map
	 * @return the map
	 */
	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
		return sortByValue(map, 1);
	}

	/**
	 * Sort by value (decreasing).
	 *
	 * @param <K>
	 *            the key type
	 * @param <V>
	 *            the value type
	 * @param map
	 *            the map
	 * @return the map
	 */
	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValueDecreasing(Map<K, V> map) {
		return sortByValue(map, -1);
	}

	/**
	 * Internal sorting mechanism.
	 *
	 * @param <K>
	 *            the key type
	 * @param <V>
	 *            the value type
	 * @param map
	 *            the map
	 * @param multiplier
	 *            the multiplier
	 * @return the map
	 */
	private static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map, Integer multiplier) {
		List<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>(map.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
			public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
				return (o1.getValue()).compareTo(o2.getValue()) * multiplier;
			}
		});

		Map<K, V> result = new LinkedHashMap<K, V>();
		for (Map.Entry<K, V> entry : list) {
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}
}
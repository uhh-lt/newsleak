package models

/**
 * Created by toa on 21.11.16.
 */
case class IteratorSession(hits: Long, hitIterator: Iterator[Document], hash: Long)

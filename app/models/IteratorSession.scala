/*
 * Copyright (C) 2016 Language Technology Group and Interactive Graphics Systems Group, Technische Universität Darmstadt, Germany
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package models

/**
 * Storage representation for the document iterator.
 *
 * This representation is used to store the current document iterator matching a search query
 * between different user requests.
 *
 * @param hits the number of documents matching the search query.
 * @param hitIterator the document iterator consisting of documents matching the search query.
 * @param hash the hash code of the search query i.e. [[models.Facets#hashCode]].
 *
 * @see For usage see [[controllers.DocumentController]].
 */
case class IteratorSession(hits: Long, hitIterator: Iterator[Document], hash: Long)

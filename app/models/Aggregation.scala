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

/** Abstract representation for the bucketing aggregation. */
sealed abstract class Bucket

/**
 * Aggregated result representation for generic metadata such as "Sender_name" or "Classification_level".
 *
 * @param key the name of the aggregated value.
 * @param occurrence the number of times this instance occurs in the background collection.
 */
case class MetaDataBucket(key: String, occurrence: Long) extends Bucket

/**
 * Aggregated result representation for entities.
 *
 * @param id the id of the entity.
 * @param occurrence the number of times the entity occurs in the background collection.
 */
case class NodeBucket(id: Long, occurrence: Long) extends Bucket

/**
 * Top-level aggregation representation.
 *
 * @param key the name of the aggregation.
 * @param buckets a list of [[models.Bucket]] representing the distinct aggregated result values.
 */
case class Aggregation(key: String, buckets: List[Bucket])

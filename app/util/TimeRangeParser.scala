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

package util

import org.joda.time.LocalDateTime
import org.joda.time.format.DateTimeFormat

/**
 * Created by flo on 7/13/2016.
 */

case class TimeRange(from: Option[LocalDateTime], to: Option[LocalDateTime])

object TimeRangeParser {
  private val YearRange = "(\\d{4})-(\\d{4})".r
  private val Year = "^(\\d{4})$".r
  private val Month = "(\\d{4})-(\\d{2})".r
  private val Date = "(\\d{4})-(\\d{1,2})-(\\d{1,2})".r

  def parseTimeRange(range: String): TimeRange = range match {
    case YearRange(from, to) =>
      TimeRange(Some(LocalDateTime.parse(from, DateTimeFormat.forPattern("yyyy"))), Some(LocalDateTime.parse(s"31.12.${to}", DateTimeFormat.forPattern("dd.MM.yyyy"))))
    case Year(year) =>
      val yearTimestamp = LocalDateTime.parse(year, DateTimeFormat.forPattern("yyyy"))
      TimeRange(Some(yearTimestamp), Some(yearTimestamp.dayOfYear().withMaximumValue()))
    case Month(year, month) =>
      val monthYear = LocalDateTime.parse(s"$month-$year", DateTimeFormat.forPattern("MM-yyyy"))
      TimeRange(Some(monthYear), Some(monthYear.dayOfMonth().withMaximumValue()))
    case Date(year, month, day) =>
      val date = Some(LocalDateTime.parse(s"$day-$month-$year", DateTimeFormat.forPattern("dd-MM-yyyy")))
      TimeRange(date, date)
    case _ => TimeRange(None, None)
  }
}

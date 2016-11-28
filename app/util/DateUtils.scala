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

/** Common helper related to time and date operations. */
class DateUtils {

  /** Date format representation consisting of a year, month and a day e.g. 2014-12-01. */
  val yearMonthDayPattern = "yyyy-MM-dd"
  /** Date format representation consisting of a year and a month e.g. 2014-12. */
  val yearMonthPattern = "yyyy-MM"
  /** Date format representation consisting of a year only e.g. 2014. */
  val yearPattern = "yyyy"

  /** Date format parser according to the following pattern ''yyyy-MM-dd''. */
  val yearMonthDayFormat = DateTimeFormat.forPattern(yearMonthDayPattern)
  /** Date format parser according to the following pattern ''yyyy-MM''. */
  val yearMonthFormat = DateTimeFormat.forPattern(yearMonthPattern)
  /** Date format parser according to the following pattern ''yyyy''. */
  val yearFormat = DateTimeFormat.forPattern(yearPattern)

  // Regular expression to match time ranges
  private val YearRange = "(\\d{4})-(\\d{4})".r
  private val Year = "^(\\d{4})$".r
  private val Month = "(\\d{4})-(\\d{2})".r
  private val Date = "(\\d{4})-(\\d{1,2})-(\\d{1,2})".r

  /**
   * Converts string date range formats to valid [[LocalDateTime]] instances.
   *
   * Valid string ranges are:
   *   (1) a year range e.g. ''2014-2016'' mapped to (2014-01-01, 2016-12-31).
   *   (2) a single year range e.g. ''2014'' mapped to (2014-01-01, 2014-12-31).
   *   (3) a year-month range e.g. ''2014-01'' mapped to (2014-01-01, 2014-01-31).
   *   (4) a complete date e.g. ''2014-01-01'' mapped to (2014-01-01, 2014-01-01).
   *
   * @param range the date range.
   * @return a valid [[LocalDateTime]] representing the given string range.
   */
  def parseTimeRange(range: String): (Option[LocalDateTime], Option[LocalDateTime]) = range match {
    case YearRange(from, to) =>
      (
        Some(LocalDateTime.parse(from, yearFormat)),
        Some(LocalDateTime.parse(s"$to-12-31", yearMonthDayFormat))
      )
    case Year(year) =>
      val yearTimestamp = LocalDateTime.parse(year, yearFormat)
      (Some(yearTimestamp), Some(yearTimestamp.dayOfYear().withMaximumValue()))
    case Month(year, month) =>
      val monthYear = LocalDateTime.parse(s"$year-$month", yearMonthFormat)
      (Some(monthYear), Some(monthYear.dayOfMonth().withMaximumValue()))
    case Date(year, month, day) =>
      val date = Some(LocalDateTime.parse(s"$year-$month-$day", yearMonthDayFormat))
      (date, date)
    case _ => (None, None)
  }
}

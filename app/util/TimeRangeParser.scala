/*
 * Copyright 2015 Technische Universitaet Darmstadt
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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
  private val Date = "(\\d{1,2}).(\\d{1,2}).(\\d{4})".r

  def parseTimeRange(range: String): TimeRange = range match {
    case YearRange(from, to) =>
      TimeRange(Some(LocalDateTime.parse(from, DateTimeFormat.forPattern("yyyy"))), Some(LocalDateTime.parse(s"31.12.${to}", DateTimeFormat.forPattern("dd.MM.yyyy"))))
    case Year(year) =>
      val yearTimestamp = LocalDateTime.parse(year, DateTimeFormat.forPattern("yyyy"))
      TimeRange(Some(yearTimestamp), Some(yearTimestamp.dayOfYear().withMaximumValue()))
    case Month(year, month) =>
      val monthYear = LocalDateTime.parse(s"$month-$year", DateTimeFormat.forPattern("MM-yyyy"))
      TimeRange(Some(monthYear), Some(monthYear.dayOfMonth().withMaximumValue()))
    case Date(day, month, year) =>
      val date = Some(LocalDateTime.parse(s"$day-$month-$year", DateTimeFormat.forPattern("dd-MM-yyyy")))
      TimeRange(date, date)
    case _ => TimeRange(None, None)
  }
}

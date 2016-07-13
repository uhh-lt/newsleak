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

  def parseTimeRange(range: String): TimeRange = {
    if (range.isEmpty) {
      TimeRange(None, None)
    } else if (range.matches("[0-9]{4}-[0-9]{4}")) {
      val yearRange = range.split("-")
      TimeRange(Some(LocalDateTime.parse(yearRange(0), DateTimeFormat.forPattern("yyyy"))), Some(LocalDateTime.parse(s"31.12.${yearRange(1)}", DateTimeFormat.forPattern("dd.MM.yyyy"))))
    } else if (range.matches("[0-9]{4}")) {
      TimeRange(Some(LocalDateTime.parse(range, DateTimeFormat.forPattern("yyyy"))), Some(LocalDateTime.parse(s"31.12.${range}", DateTimeFormat.forPattern("dd.MM.yyyy"))))
    } else if (range.matches("[A-Z][a-z]* [0-9]{4}")) {
      val month = LocalDateTime.parse(range, DateTimeFormat.forPattern("MMMMM yyyy"))
      TimeRange(Some(month), Some(month.dayOfMonth().withMaximumValue()))
    } else if (range.matches("[0-9]{1,2}.[0-9]{1,2}.[0-9]{4}")) {
      val date = Some(LocalDateTime.parse(range, DateTimeFormat.forPattern("dd.MM.yyyy")))
      TimeRange(date, date)
    } else {
      TimeRange(None, None)
    }
  }
}

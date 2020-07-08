// Copyright 2020 Aurimas Liutikas
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package net.liutikas.sensorsfetch

import java.time.LocalDate

class DateIterator(
        private val startDate: LocalDate,
        private val endDateInclusive: LocalDate
): Iterator<LocalDate> {
    private var currentDate = endDateInclusive

    override fun hasNext() = currentDate >= startDate

    override fun next(): LocalDate {
        val next = currentDate
        currentDate = currentDate.minusDays(1)
        return next

    }
}

class DateProgression(
        override val start: LocalDate,
        override val endInclusive: LocalDate
) : Iterable<LocalDate>, ClosedRange<LocalDate> {
    override fun iterator(): Iterator<LocalDate> = DateIterator(start, endInclusive)
}

operator fun LocalDate.rangeTo(other: LocalDate) = DateProgression(this, other)

package tickets4sale.stagekeeper.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

final case class Show(title: String, openingDay: LocalDate, genre: Genre):
  def isRunningOn(date: LocalDate): Boolean =
    !date.isBefore(openingDay) && !date.isAfter(openingDay.plusDays(99))

  def priceOn(date: LocalDate): Int =
    val base = Genre.basePrice(genre)
    val daysRunning = ChronoUnit.DAYS.between(openingDay, date) + 1
    if daysRunning > 80 then (base * 0.8).toInt else base

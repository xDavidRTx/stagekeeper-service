package tickets4sale.stagekeeper.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

// I will assume from now on that the title is unique and there is no possibility of having
// the same title with a different date or genre. I would use UUID's here instead
final case class Show(title: String, openingDay: LocalDate, genre: Genre):
  val initialAvailableTickets = 100
  def isRunningOn(date: LocalDate): Boolean =
    !date.isBefore(openingDay) && !date.isAfter(openingDay.plusDays(99))

  def priceOn(date: LocalDate): Int =
    val basePrice = Genre.basePrice(genre)
    val daysRunning = ChronoUnit.DAYS.between(openingDay, date) + 1
    if daysRunning > 80 then (basePrice * 0.8).toInt else basePrice

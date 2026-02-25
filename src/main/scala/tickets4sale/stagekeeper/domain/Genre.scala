package tickets4sale.stagekeeper.domain

enum Genre:
  case Musical, Comedy, Drama

object Genre:
  def fromString(value: String): Either[String, Genre] = value.trim.toLowerCase match
    case "musical" => Right(Genre.Musical)
    case "comedy"  => Right(Genre.Comedy)
    case "drama"   => Right(Genre.Drama)
    case other     => Left(s"An unknown genre: $other")

  def basePrice(genre: Genre): Int =
    genre match
      case Genre.Musical => 70
      case Genre.Comedy  => 50
      case Genre.Drama   => 40

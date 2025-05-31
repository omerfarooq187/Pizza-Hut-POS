package data.utils

object PizzaValidator {

    fun validateName(name: String) : Boolean{
        return name.length in 3..50
    }

    fun validatePrice(price: String) : Boolean{
        return try {
            val value = price.toDouble()
            value > 0
        } catch (e: NumberFormatException) {
            false
        }
    }
}
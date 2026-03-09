package ru.netology.nework.util

object FormatNumber {
    fun formatNumber(count: Int): String {
        return when {
            count < 1000 -> count.toString()
            count < 10000 -> {
                val hundreds = (count % 1000) / 100
                if (hundreds == 0) {
                    "${count / 1000}K"
                } else {
                    "${count / 1000}.${hundreds}K"
                }
            }

            count < 1000000 -> "${count / 1000}K"
            else -> {
                val hundredThousands = (count % 1000000) / 100000
                if (hundredThousands == 0) {
                    "${count / 1000000}M"
                } else {
                    "${count / 1000000}.${hundredThousands}M"
                }
            }
        }
    }
}
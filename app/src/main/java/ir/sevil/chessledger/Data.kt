package ir.sevil.chessledger

import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

object Category {
    const val EXTRA = "extra"
    const val BOOK = "book"
    const val NEW = "new"

    fun label(c: String) = when (c) {
        BOOK -> "کتاب"
        EXTRA -> "جانبی"
        else -> "جدید"
    }
}

data class Session(val id: Long, val number: Int, val date: String, val weekday: String, val note: String)
data class Payment(val id: Long, val date: String, val amount: Long)
data class Expense(
    val id: Long,
    val date: String,
    val title: String,
    val amount: Long?,
    val paid: Boolean,
    val category: String
)

data class LedgerData(
    val sessionPrice: Long,
    val sessions: List<Session>,
    val payments: List<Payment>,
    val expenses: List<Expense>
) {
    val sessionCost: Long get() = sessions.size * sessionPrice
    val tuitionPaid: Long get() = payments.sumOf { it.amount }
    val tuitionRemaining: Long get() = sessionCost - tuitionPaid
    val paidExpenses: Long get() = expenses.filter { it.paid }.sumOf { it.amount ?: 0L }
    val unpaidExpenses: Long get() = expenses.filter { !it.paid }.sumOf { it.amount ?: 0L }
    val expensesWithoutAmount: Int get() = expenses.count { it.amount == null }
    val totalPaid: Long get() = tuitionPaid + paidExpenses

    fun toJson(): String {
        val o = JSONObject()
        o.put("sessionPrice", sessionPrice)
        o.put("sessions", JSONArray().also { arr ->
            sessions.forEach {
                arr.put(JSONObject().put("id", it.id).put("number", it.number)
                    .put("date", it.date).put("weekday", it.weekday).put("note", it.note))
            }
        })
        o.put("payments", JSONArray().also { arr ->
            payments.forEach {
                arr.put(JSONObject().put("id", it.id).put("date", it.date).put("amount", it.amount))
            }
        })
        o.put("expenses", JSONArray().also { arr ->
            expenses.forEach {
                arr.put(JSONObject().put("id", it.id).put("date", it.date).put("title", it.title)
                    .put("amount", it.amount ?: JSONObject.NULL)
                    .put("paid", it.paid).put("category", it.category))
            }
        })
        return o.toString(2)
    }

    companion object {
        private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

        fun fromJson(text: String): LedgerData {
            val o = JSONObject(text)
            return LedgerData(
                sessionPrice = o.getLong("sessionPrice"),
                sessions = o.getJSONArray("sessions").objects().map {
                    Session(it.getLong("id"), it.getInt("number"), it.getString("date"),
                        it.getString("weekday"), it.getString("note"))
                },
                payments = o.getJSONArray("payments").objects().map {
                    Payment(it.getLong("id"), it.getString("date"), it.getLong("amount"))
                },
                expenses = o.getJSONArray("expenses").objects().map {
                    Expense(it.getLong("id"), it.getString("date"), it.getString("title"),
                        if (it.isNull("amount")) null else it.getLong("amount"),
                        it.getBoolean("paid"), it.getString("category"))
                }
            )
        }
    }
}

// ---------- Formatting helpers ----------

fun toPersianDigits(s: String): String = s.map {
    when (it) {
        in '0'..'9' -> '۰' + (it - '0')
        ',' -> '٬'
        else -> it
    }
}.joinToString("")

fun Long.toMoney(): String = toPersianDigits(String.format(Locale.US, "%,d", this))
fun Int.toFa(): String = toPersianDigits(toString())

/** Accepts Persian/Arabic/Latin digits and thousand separators. Returns null if invalid. */
fun parseAmount(text: String): Long? {
    val cleaned = text.trim().map {
        when (it) {
            in '۰'..'۹' -> '0' + (it - '۰')
            in '٠'..'٩' -> '0' + (it - '٠')
            else -> it
        }
    }.filter { it.isDigit() }.joinToString("")
    return cleaned.toLongOrNull()
}

// ---------- Seed data (سال ۱۴۰۵) ----------

object Seed {
    private fun s(n: Int, d: String, w: String = "", note: String = "") =
        Session(n.toLong(), n, d, w, note)

    fun data(): LedgerData {
        val sessions = listOf(
            s(5, "۹ اردیبهشت", "", "کتاب مسواک + کتاب استراتژی"),
            s(6, "۱۲ اردیبهشت", "شنبه"),
            s(7, "۱۳ اردیبهشت", "یکشنبه"),
            s(8, "۱۴ اردیبهشت", "دوشنبه"),
            s(9, "۱۵ اردیبهشت", "سه‌شنبه"),
            s(10, "۱۶ اردیبهشت", "چهارشنبه"),
            s(11, "۲۱ اردیبهشت", "دوشنبه"),
            s(12, "۲۲ اردیبهشت", "سه‌شنبه"),
            s(13, "۲۴ اردیبهشت", "پنجشنبه"),
            s(14, "۲۷ اردیبهشت", "دوشنبه"),
            s(15, "۲۸ اردیبهشت", "دوشنبه"),
            s(16, "۲۹ اردیبهشت", "سه‌شنبه"),
            s(17, "۴ خرداد"),
            s(18, "۵ خرداد", "سه‌شنبه"),
            s(19, "۱۱ خرداد", "دوشنبه"),
            s(20, "۱۲ خرداد", "سه‌شنبه"),
            s(21, "۱۴ خرداد"),
            s(22, "۱۷ خرداد", "یکشنبه"),
            s(23, "۱۸ خرداد", "دوشنبه"),
            s(24, "۱۹ خرداد", "سه‌شنبه"),
            s(25, "۲۱ خرداد", "پنجشنبه"),
            s(26, "۲ تیر", "سه‌شنبه"),
            s(27, "۸ تیر", "دوشنبه"),
            s(28, "۱۱ تیر", "پنجشنبه"),
            s(29, "۱۵ تیر", "دوشنبه", "کتاب مسواک جدید"),
            s(30, "۱۸ تیر", "پنجشنبه"),
            s(31, "۲۲ تیر", "دوشنبه"),
            s(32, "۲۵ تیر", "پنجشنبه"),
            s(33, "۲۹ تیر", "دوشنبه"),
            s(34, "۱ مرداد", "پنجشنبه"),
            s(35, "۵ مرداد", "دوشنبه"),
            s(36, "۱۲ مرداد", "دوشنبه"),
            s(37, "۱۵ مرداد", "پنجشنبه"),
            s(38, "۱۹ مرداد", "دوشنبه"),
            s(39, "۲۲ مرداد", "پنجشنبه"),
            s(40, "۲۶ مرداد", "دوشنبه", "کتاب یوسوپوف ۳"),
            s(41, "۱۲ شهریور", "پنجشنبه"),
            s(42, "۱۶ شهریور", "دوشنبه"),
            s(43, "۱۹ شهریور", "پنجشنبه"),
            s(44, "۲۶ شهریور", "پنجشنبه"),
            s(45, "۹ مهر", "پنجشنبه", "کتاب مسواک جدید + ۱۰۰۱ روش مات"),
            s(46, "۱۶ مهر", "پنجشنبه")
        )
        val payments = listOf(
            Payment(1, "۲ خرداد ۱۴۰۵", 4_000_000),
            Payment(2, "۸ تیر ۱۴۰۵", 5_000_000),
            Payment(3, "۶ مرداد ۱۴۰۵", 5_000_000),
            Payment(4, "۱ شهریور ۱۴۰۵", 5_000_000),
            Payment(5, "۳۱ شهریور ۱۴۰۵", 5_000_000)
        )
        val expenses = listOf(
            Expense(1, "", "تی‌شرت", 700_000, true, Category.EXTRA),
            Expense(2, "", "مسابقه باشگاه", 200_000, true, Category.EXTRA),
            Expense(3, "۹ اردیبهشت", "مسواک", null, false, Category.BOOK),
            Expense(4, "۹ اردیبهشت", "استراتژی", null, false, Category.BOOK),
            Expense(5, "۱۵ تیر", "مسواک جدید", null, false, Category.BOOK),
            Expense(6, "۲۶ مرداد", "یوسوپوف ۳", null, false, Category.BOOK),
            Expense(7, "۹ مهر", "مسواک جدید", null, false, Category.BOOK),
            Expense(8, "۹ مهر", "۱۰۰۱ روش مات", null, false, Category.BOOK)
        )
        return LedgerData(850_000, sessions, payments, expenses)
    }
}

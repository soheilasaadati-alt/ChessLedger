package ir.sevil.chessledger

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import java.io.File

class LedgerViewModel(app: Application) : AndroidViewModel(app) {

    private val file = File(app.filesDir, "ledger.json")

    var data by mutableStateOf(load())
        private set

    private fun load(): LedgerData {
        if (!file.exists()) return Seed.data().also { write(it) }
        return try {
            LedgerData.fromJson(file.readText())
        } catch (e: Exception) {
            // فایل خراب: نسخه پشتیبان نگه می‌داریم و داده اولیه را می‌سازیم
            file.renameTo(File(file.parentFile, "ledger.corrupt.${System.currentTimeMillis()}.json"))
            Seed.data().also { write(it) }
        }
    }

    private fun write(d: LedgerData) {
        file.writeText(d.toJson())
    }

    private fun update(transform: (LedgerData) -> LedgerData) {
        data = transform(data)
        write(data)
    }

    fun setPrice(price: Long) = update { it.copy(sessionPrice = price) }

    fun upsertSession(s: Session) = update { d ->
        d.copy(sessions = if (d.sessions.any { it.id == s.id }) d.sessions.map { if (it.id == s.id) s else it } else d.sessions + s)
    }

    fun deleteSession(id: Long) = update { d -> d.copy(sessions = d.sessions.filter { it.id != id }) }

    fun upsertPayment(p: Payment) = update { d ->
        d.copy(payments = if (d.payments.any { it.id == p.id }) d.payments.map { if (it.id == p.id) p else it } else d.payments + p)
    }

    fun deletePayment(id: Long) = update { d -> d.copy(payments = d.payments.filter { it.id != id }) }

    fun upsertExpense(e: Expense) = update { d ->
        d.copy(expenses = if (d.expenses.any { it.id == e.id }) d.expenses.map { if (it.id == e.id) e else it } else d.expenses + e)
    }

    fun deleteExpense(id: Long) = update { d -> d.copy(expenses = d.expenses.filter { it.id != id }) }

    fun togglePaid(id: Long) = update { d ->
        d.copy(expenses = d.expenses.map { if (it.id == id) it.copy(paid = !it.paid) else it })
    }
}

@file:OptIn(ExperimentalMaterial3Api::class)

package ir.sevil.chessledger

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

sealed interface DialogState {
    data object EditPrice : DialogState
    data class EditSession(val item: Session?) : DialogState
    data class EditPayment(val item: Payment?) : DialogState
    data class EditExpense(val item: Expense?) : DialogState
}

@Composable
fun LedgerApp(vm: LedgerViewModel = viewModel()) {
    val d = vm.data
    var tab by remember { mutableIntStateOf(0) }
    var dialog by remember { mutableStateOf<DialogState?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("♟️ دفتر حساب شطرنج سویل — ۱۴۰۵") }) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = tab == 0, onClick = { tab = 0 },
                    icon = { Icon(Icons.Default.Home, null) }, label = { Text("خلاصه") })
                NavigationBarItem(selected = tab == 1, onClick = { tab = 1 },
                    icon = { Icon(Icons.Default.DateRange, null) }, label = { Text("جلسات") })
                NavigationBarItem(selected = tab == 2, onClick = { tab = 2 },
                    icon = { Icon(Icons.Default.Check, null) }, label = { Text("پرداخت‌ها") })
                NavigationBarItem(selected = tab == 3, onClick = { tab = 3 },
                    icon = { Icon(Icons.Default.ShoppingCart, null) }, label = { Text("هزینه‌ها") })
            }
        },
        floatingActionButton = {
            if (tab != 0) {
                FloatingActionButton(onClick = {
                    dialog = when (tab) {
                        1 -> DialogState.EditSession(null)
                        2 -> DialogState.EditPayment(null)
                        else -> DialogState.EditExpense(null)
                    }
                }) { Icon(Icons.Default.Add, "افزودن") }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                0 -> SummaryScreen(d) { dialog = DialogState.EditPrice }
                1 -> SessionsScreen(d) { dialog = DialogState.EditSession(it) }
                2 -> PaymentsScreen(d) { dialog = DialogState.EditPayment(it) }
                else -> ExpensesScreen(d, onToggle = { vm.togglePaid(it) }) { dialog = DialogState.EditExpense(it) }
            }
        }
    }

    when (val s = dialog) {
        null -> {}
        is DialogState.EditPrice -> PriceDialog(d.sessionPrice,
            onSave = { vm.setPrice(it); dialog = null }, onDismiss = { dialog = null })
        is DialogState.EditSession -> SessionDialog(s.item, nextNumber = (d.sessions.maxOfOrNull { it.number } ?: 0) + 1,
            onSave = { vm.upsertSession(it); dialog = null },
            onDelete = s.item?.let { item -> { vm.deleteSession(item.id); dialog = null } },
            onDismiss = { dialog = null })
        is DialogState.EditPayment -> PaymentDialog(s.item,
            onSave = { vm.upsertPayment(it); dialog = null },
            onDelete = s.item?.let { item -> { vm.deletePayment(item.id); dialog = null } },
            onDismiss = { dialog = null })
        is DialogState.EditExpense -> ExpenseDialog(s.item,
            onSave = { vm.upsertExpense(it); dialog = null },
            onDelete = s.item?.let { item -> { vm.deleteExpense(item.id); dialog = null } },
            onDismiss = { dialog = null })
    }
}

// ---------------- Screens ----------------

@Composable
fun Line(label: String, value: String, bold: Boolean = false, color: Color = Color.Unspecified, onClick: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable { onClick() } else Modifier).padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        Text(value, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, color = color)
    }
}

@Composable
fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Column(Modifier.padding(top = 8.dp), content = content)
        }
    }
}

@Composable
fun SummaryScreen(d: LedgerData, onEditPrice: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard("شهریه جلسات") {
            Line("هزینه هر جلسه (برای تغییر لمس کن)", "${d.sessionPrice.toMoney()} تومان", onClick = onEditPrice)
            Line("تعداد جلسات ثبت‌شده", "${d.sessions.size.toFa()} جلسه")
            Line("جمع هزینه جلسات", "${d.sessionCost.toMoney()} تومان")
            Line("جمع پرداخت شهریه", "${d.tuitionPaid.toMoney()} تومان")
            Line("مانده شهریه جلسات", "${d.tuitionRemaining.toMoney()} تومان", bold = true,
                color = if (d.tuitionRemaining > 0) MaterialTheme.colorScheme.error else Color.Unspecified)
        }
        SectionCard("هزینه‌های جانبی، کتاب و جدید") {
            Line("جمع پرداخت‌شده", "${d.paidExpenses.toMoney()} تومان")
            Line("پرداخت‌نشده (دارای مبلغ)", "${d.unpaidExpenses.toMoney()} تومان")
            if (d.expensesWithoutAmount > 0) {
                Line("موارد بدون مبلغ ثبت‌شده", "${d.expensesWithoutAmount.toFa()} مورد")
            }
        }
        SectionCard("جمع کل") {
            Line("جمع کل پرداخت‌های ثبت‌شده", "${d.totalPaid.toMoney()} تومان", bold = true)
        }
        Text(
            "نکته: مانده شهریه فقط مربوط به جلسات است. جلسات ۱ تا ۴ در فهرست نیست؛ اگر حساب جداگانه دارند، از تب جلسات اضافه کن.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun SessionsScreen(d: LedgerData, onEdit: (Session) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(d.sessions.sortedByDescending { it.number }, key = { it.id }) { s ->
            Card(Modifier.fillMaxWidth().clickable { onEdit(s) }) {
                Column(Modifier.padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("جلسه ${s.number.toFa()}", fontWeight = FontWeight.Bold)
                        Text(listOf(s.weekday, s.date).filter { it.isNotBlank() }.joinToString(" - "))
                    }
                    if (s.note.isNotBlank()) Text(s.note, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item { androidx.compose.foundation.layout.Spacer(Modifier.padding(40.dp)) }
    }
}

@Composable
fun PaymentsScreen(d: LedgerData, onEdit: (Payment) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Line("جمع پرداخت شهریه", "${d.tuitionPaid.toMoney()} تومان", bold = true)
        }
        items(d.payments, key = { it.id }) { p ->
            Card(Modifier.fillMaxWidth().clickable { onEdit(p) }) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(p.date)
                    Text("${p.amount.toMoney()} تومان", fontWeight = FontWeight.Bold)
                }
            }
        }
        item { androidx.compose.foundation.layout.Spacer(Modifier.padding(40.dp)) }
    }
}

@Composable
fun ExpensesScreen(d: LedgerData, onToggle: (Long) -> Unit, onEdit: (Expense) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(d.expenses, key = { it.id }) { e ->
            Card(Modifier.fillMaxWidth().clickable { onEdit(e) }) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = e.paid, onCheckedChange = { onToggle(e.id) })
                    Column(Modifier.weight(1f)) {
                        Text(e.title, fontWeight = FontWeight.Bold)
                        Text(
                            listOf(Category.label(e.category), e.date).filter { it.isNotBlank() }.joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(if (e.amount != null) "${e.amount.toMoney()} تومان" else "مبلغ ثبت نشده")
                }
            }
        }
        item { androidx.compose.foundation.layout.Spacer(Modifier.padding(40.dp)) }
    }
}

// ---------------- Dialogs ----------------

@Composable
fun FormDialog(
    title: String,
    canSave: Boolean,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    var confirmDelete by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp), content = content) },
        confirmButton = { TextButton(onClick = onSave, enabled = canSave) { Text("ذخیره") } },
        dismissButton = {
            Row {
                TextButton(onClick = onDismiss) { Text("انصراف") }
                if (onDelete != null) {
                    TextButton(onClick = { if (confirmDelete) onDelete() else confirmDelete = true }) {
                        Text(if (confirmDelete) "تأیید حذف" else "حذف", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    )
}

@Composable
fun Field(value: String, onChange: (String) -> Unit, label: String, number: Boolean = false) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true,
        keyboardOptions = if (number) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun PriceDialog(current: Long, onSave: (Long) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(current.toString()) }
    val amount = parseAmount(text)
    FormDialog("هزینه هر جلسه (تومان)", canSave = amount != null && amount > 0,
        onSave = { onSave(amount!!) }, onDismiss = onDismiss) {
        Field(text, { text = it }, "مبلغ", number = true)
    }
}

@Composable
fun SessionDialog(initial: Session?, nextNumber: Int, onSave: (Session) -> Unit, onDelete: (() -> Unit)?, onDismiss: () -> Unit) {
    var number by remember { mutableStateOf((initial?.number ?: nextNumber).toString()) }
    var date by remember { mutableStateOf(initial?.date ?: "") }
    var weekday by remember { mutableStateOf(initial?.weekday ?: "") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    val n = parseAmount(number)?.toInt()
    FormDialog(if (initial == null) "جلسه جدید" else "ویرایش جلسه", canSave = n != null,
        onSave = { onSave(Session(initial?.id ?: System.currentTimeMillis(), n!!, date.trim(), weekday.trim(), note.trim())) },
        onDismiss = onDismiss, onDelete = onDelete) {
        Field(number, { number = it }, "شماره جلسه", number = true)
        Field(date, { date = it }, "تاریخ (مثلاً ۲۰ مهر)")
        Field(weekday, { weekday = it }, "روز هفته")
        Field(note, { note = it }, "توضیحات")
    }
}

@Composable
fun PaymentDialog(initial: Payment?, onSave: (Payment) -> Unit, onDelete: (() -> Unit)?, onDismiss: () -> Unit) {
    var date by remember { mutableStateOf(initial?.date ?: "") }
    var amountText by remember { mutableStateOf(initial?.amount?.toString() ?: "") }
    val amount = parseAmount(amountText)
    FormDialog(if (initial == null) "پرداخت جدید" else "ویرایش پرداخت", canSave = amount != null,
        onSave = { onSave(Payment(initial?.id ?: System.currentTimeMillis(), date.trim(), amount!!)) },
        onDismiss = onDismiss, onDelete = onDelete) {
        Field(date, { date = it }, "تاریخ پرداخت (مثلاً ۳۱ مهر ۱۴۰۵)")
        Field(amountText, { amountText = it }, "مبلغ (تومان)", number = true)
    }
}

@Composable
fun ExpenseDialog(initial: Expense?, onSave: (Expense) -> Unit, onDelete: (() -> Unit)?, onDismiss: () -> Unit) {
    var date by remember { mutableStateOf(initial?.date ?: "") }
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var amountText by remember { mutableStateOf(initial?.amount?.toString() ?: "") }
    var paid by remember { mutableStateOf(initial?.paid ?: false) }
    var category by remember { mutableStateOf(initial?.category ?: Category.NEW) }
    val amount = parseAmount(amountText)
    val valid = title.isNotBlank() && (amountText.isBlank() || amount != null)
    FormDialog(if (initial == null) "هزینه جدید" else "ویرایش هزینه", canSave = valid,
        onSave = {
            onSave(Expense(initial?.id ?: System.currentTimeMillis(), date.trim(), title.trim(),
                if (amountText.isBlank()) null else amount, paid, category))
        },
        onDismiss = onDismiss, onDelete = onDelete) {
        Field(title, { title = it }, "شرح هزینه")
        Field(amountText, { amountText = it }, "مبلغ (تومان) — اختیاری", number = true)
        Field(date, { date = it }, "تاریخ")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(Category.NEW, Category.BOOK, Category.EXTRA).forEach { c ->
                FilterChip(selected = category == c, onClick = { category = c }, label = { Text(Category.label(c)) })
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = paid, onCheckedChange = { paid = it })
            Text("پرداخت‌شده")
        }
    }
}

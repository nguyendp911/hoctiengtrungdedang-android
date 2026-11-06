@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.nguyendp.hantu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HanTuApp(words: List<Word>, progress: Progress, speaker: Speaker) {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Học" to "📖", "Lộ trình" to "🗺️", "Ôn tập" to "🔁", "Cài đặt" to "⚙️")
    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { i, (label, emoji) ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Text(emoji) },
                        label = { Text(label) },
                    )
                }
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> LearnScreen(words, progress, speaker)
                1 -> PathScreen(words, progress, speaker)
                2 -> ReviewScreen(words, progress, speaker)
                else -> SettingsScreen(words, progress)
            }
        }
    }
}

@Composable
fun WordCard(w: Word, progress: Progress, speaker: Speaker) {
    var open by remember(w.id) { mutableStateOf(false) }
    val fav = progress.isFav(w.id)
    val learned = progress.isLearned(w.id)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable { open = !open },
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(w.hanzi, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Text(w.pinyin, color = MaterialTheme.colorScheme.primary)
                    if (w.readingVi.isNotEmpty()) Text("Bồi: ${w.readingVi}", fontSize = 13.sp)
                    Text(w.meaningVi)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TextButton(onClick = { speaker.speak(w.hanzi) }) { Text("🔊", fontSize = 22.sp) }
                    TextButton(onClick = { progress.toggleFav(w.id) }) {
                        Text(if (fav) "★" else "☆", fontSize = 22.sp)
                    }
                }
            }
            if (open) {
                Spacer(Modifier.height(6.dp))
                if (w.hanViet.isNotEmpty()) Text("Hán Việt: ${w.hanViet}")
                val lv = w.level
                if (lv != null) {
                    val unitText = if (w.unit != null) " · Bài ${w.unitNo}: ${w.unit}" else ""
                    Text(levelName(lv) + unitText)
                }
                val exZh = w.exZh
                if (!exZh.isNullOrEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(exZh, fontSize = 18.sp, modifier = Modifier.weight(1f))
                        TextButton(onClick = { speaker.speak(exZh) }) { Text("🔊") }
                    }
                    val exPy = w.exPy
                    if (!exPy.isNullOrEmpty()) Text(exPy, color = MaterialTheme.colorScheme.primary)
                    val exVi = w.exVi
                    if (!exVi.isNullOrEmpty()) Text(exVi)
                }
                Spacer(Modifier.height(6.dp))
                OutlinedButton(onClick = { progress.setLearned(w.id, !learned) }) {
                    Text(if (learned) "Bỏ đánh dấu đã thuộc" else "Đánh dấu đã thuộc")
                }
            }
        }
    }
}

@Composable
fun WordList(words: List<Word>, progress: Progress, speaker: Speaker) {
    if (words.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Không có từ nào") }
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(words, key = { it.id }) { w -> WordCard(w, progress, speaker) }
    }
}

@Composable
fun LearnScreen(words: List<Word>, progress: Progress, speaker: Speaker) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableIntStateOf(0) } // 0 = tất cả, -1 = yêu thích, 1..7 = cấp
    val q = fold(query.trim())
    val shown = words.filter { w ->
        val okLevel = when (filter) {
            0 -> true
            -1 -> progress.isFav(w.id)
            else -> w.level == filter
        }
        okLevel && (q.isEmpty() || w.searchText.contains(q))
    }
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Tìm chữ Hán, pinyin, nghĩa...") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(12.dp),
        )
        FlowRow(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = filter == 0, onClick = { filter = 0 }, label = { Text("Tất cả") })
            FilterChip(selected = filter == -1, onClick = { filter = -1 }, label = { Text("★ Yêu thích") })
            for (l in 1..7) {
                FilterChip(selected = filter == l, onClick = { filter = l }, label = { Text(levelName(l)) })
            }
        }
        Text("${shown.size} từ", Modifier.padding(horizontal = 16.dp, vertical = 4.dp), fontSize = 12.sp)
        WordList(shown, progress, speaker)
    }
}

@Composable
fun PathScreen(words: List<Word>, progress: Progress, speaker: Speaker) {
    var level by remember { mutableIntStateOf(1) }
    var unitNo by remember { mutableStateOf<Int?>(null) }
    val inLevel = remember(words, level) { words.filter { it.level == level } }
    val units = remember(inLevel) { inLevel.groupBy { it.unitNo ?: 0 }.toSortedMap() }
    Column(Modifier.fillMaxSize()) {
        FlowRow(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (l in 1..7) {
                FilterChip(
                    selected = level == l,
                    onClick = { level = l; unitNo = null },
                    label = { Text(levelName(l)) },
                )
            }
        }
        val current = unitNo
        if (current == null) {
            val learned = inLevel.count { progress.isLearned(it.id) }
            Text(
                "${levelName(level)}: ${inLevel.size} từ, ${units.size} bài, đã thuộc $learned",
                Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            LazyColumn(Modifier.fillMaxSize()) {
                items(units.entries.toList(), key = { it.key }) { entry ->
                    val no = entry.key
                    val list = entry.value
                    val done = list.count { progress.isLearned(it.id) }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .clickable { unitNo = no },
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                "Bài $no: ${list.first().unit ?: "Từ vựng"}",
                                fontWeight = FontWeight.Bold,
                            )
                            Text("${list.size} từ · đã thuộc $done/${list.size}", fontSize = 13.sp)
                        }
                    }
                }
            }
        } else {
            TextButton(onClick = { unitNo = null }) { Text("← Danh sách bài") }
            val list = units[current].orEmpty()
            Text(
                "Bài $current: ${list.firstOrNull()?.unit ?: ""}",
                Modifier.padding(horizontal = 16.dp),
                fontWeight = FontWeight.Bold,
            )
            WordList(list, progress, speaker)
        }
    }
}

@Composable
fun ReviewScreen(words: List<Word>, progress: Progress, speaker: Speaker) {
    var level by remember { mutableIntStateOf(0) } // 0 = mọi cấp
    var queue by remember { mutableStateOf<List<Word>>(emptyList()) }
    var idx by remember { mutableIntStateOf(0) }
    var flipped by remember { mutableStateOf(false) }
    var started by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (!started) {
            val today = progress.today()
            val pool = if (level == 0) words else words.filter { it.level == level }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = level == 0, onClick = { level = 0 }, label = { Text("Mọi cấp") })
                for (l in 1..7) {
                    FilterChip(selected = level == l, onClick = { level = l }, label = { Text(levelName(l)) })
                }
            }
            val dueCount = pool.count { progress.seen(it.id) && progress.due(it.id) <= today }
            Spacer(Modifier.height(24.dp))
            Text("Đến hạn ôn: $dueCount từ", fontSize = 18.sp)
            Text("Từ mới mỗi buổi: ${progress.newPerDay}")
            Spacer(Modifier.height(16.dp))
            Button(onClick = {
                val due = pool.filter { progress.seen(it.id) && progress.due(it.id) <= today }
                    .shuffled().take(30)
                val fresh = pool.filter { !progress.seen(it.id) }.take(progress.newPerDay)
                queue = due + fresh
                idx = 0
                flipped = false
                started = true
            }) { Text("Bắt đầu ôn") }
        } else if (idx >= queue.size) {
            Spacer(Modifier.height(48.dp))
            Text(
                if (queue.isEmpty()) "Hôm nay không còn từ nào cần ôn 🎉" else "Hoàn thành ${queue.size} từ 🎉",
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = { started = false }) { Text("Quay lại") }
        } else {
            val w = queue[idx]
            Text("${idx + 1}/${queue.size}", fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            Card(Modifier.fillMaxWidth().weight(1f).clickable { flipped = true }) {
                Column(
                    Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(w.hanzi, fontSize = 56.sp, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { speaker.speak(w.hanzi) }) { Text("🔊", fontSize = 26.sp) }
                    if (flipped) {
                        Text(w.pinyin, fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
                        if (w.readingVi.isNotEmpty()) Text("Bồi: ${w.readingVi}")
                        if (w.hanViet.isNotEmpty()) Text("Hán Việt: ${w.hanViet}")
                        Spacer(Modifier.height(8.dp))
                        Text(w.meaningVi, fontSize = 20.sp, textAlign = TextAlign.Center)
                        val exZh = w.exZh
                        if (!exZh.isNullOrEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            Text(exZh, fontSize = 18.sp, textAlign = TextAlign.Center)
                            val exVi = w.exVi
                            if (!exVi.isNullOrEmpty()) Text(exVi, textAlign = TextAlign.Center)
                        }
                    } else {
                        Text("Chạm để xem đáp án", fontSize = 13.sp)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            if (flipped) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = {
                        progress.record(w.id, false)
                        idx++
                        flipped = false
                    }) { Text("Chưa nhớ") }
                    Button(onClick = {
                        progress.record(w.id, true)
                        idx++
                        flipped = false
                    }) { Text("Nhớ rồi") }
                }
            } else {
                Button(onClick = { flipped = true }) { Text("Xem đáp án") }
            }
        }
    }
}

@Composable
fun SettingsScreen(words: List<Word>, progress: Progress) {
    var confirm by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Cài đặt", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text("Số từ mới mỗi buổi ôn: ${progress.newPerDay}")
        Slider(
            value = progress.newPerDay.toFloat(),
            onValueChange = { progress.newPerDay = it.toInt() },
            valueRange = 5f..50f,
            steps = 8,
        )
        Spacer(Modifier.height(16.dp))
        Text("Đã học: ${progress.seenCount(words)} từ")
        Text("Đã thuộc: ${progress.learnedCount(words)} từ")
        Text("Tổng số từ trong app: ${words.size}")
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = { confirm = true }) { Text("Xoá toàn bộ tiến độ") }
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Xoá tiến độ?") },
            text = { Text("Toàn bộ từ đã học, đã thuộc và yêu thích sẽ bị xoá.") },
            confirmButton = {
                TextButton(onClick = { progress.resetAll(); confirm = false }) { Text("Xoá") }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Huỷ") } },
        )
    }
}

package id.biojelan.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.biojelan.app.ui.icons.BioIcons
import id.biojelan.app.ui.strings.BioText
import id.biojelan.app.ui.theme.BioTheme

/**
 * Timeline vertikal lima langkah penjemputan: Ditugaskan → Dalam perjalanan → Tiba di lokasi →
 * Menunggu konfirmasi → Selesai. Dipakai Agen (tab Pickup) dan Driver (detail stop) supaya kedua sisi
 * menampilkan pickup yang sama dengan bahasa visual yang sama.
 *
 * [activeIndex]: indeks langkah aktif (0..4), `5` = semua selesai, `-1` = tidak ada yang menyala.
 * [currentNote]: penjelasan singkat, tampil di bawah langkah aktif (atau di bawah langkah terakhir kalau
 * semua selesai). Null = tanpa penjelasan.
 */
@Composable
fun PickupTimeline(activeIndex: Int, currentNote: String?, modifier: Modifier = Modifier) {
    val c = BioTheme.colors
    val s = BioText.current
    val steps = listOf(s.stepAssigned, s.stepOnTheWay, s.stepArrived, s.stepAwaiting, s.stepCompleted)
    val noteAt = if (activeIndex >= steps.size) steps.lastIndex else activeIndex
    Column(modifier.fillMaxWidth().bioCard(22.dp).padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 18.dp)) {
        steps.forEachIndexed { i, label ->
            val done = i < activeIndex
            val now = i == activeIndex
            val last = i == steps.lastIndex
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(Modifier.fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                    TimelineNode(done = done, now = now)
                    if (!last) {
                        Box(
                            Modifier
                                .padding(vertical = 4.dp)
                                .width(2.dp)
                                .weight(1f)
                                .background(if (done) c.primary else c.line),
                        )
                    }
                }
                Column(Modifier.padding(top = 2.dp, bottom = if (last) 0.dp else 14.dp)) {
                    Text(
                        label,
                        style = BioTheme.type.label.copy(fontSize = 14.sp),
                        color = if (done || now) c.ink else c.muted,
                    )
                    if (i == noteAt && !currentNote.isNullOrBlank()) {
                        Text(currentNote, style = BioTheme.type.small, color = c.muted, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineNode(done: Boolean, now: Boolean) {
    val c = BioTheme.colors
    when {
        done -> Box(Modifier.size(24.dp).background(c.primary, CircleShape), contentAlignment = Alignment.Center) {
            Icon(BioIcons.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
        }
        now -> Box(
            Modifier.size(24.dp).background(c.amberTint, CircleShape).border(2.dp, c.amberDeep, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Box(Modifier.size(8.dp).background(c.amberDeep, CircleShape)) }
        else -> Box(Modifier.size(24.dp).border(2.dp, c.line, CircleShape))
    }
}

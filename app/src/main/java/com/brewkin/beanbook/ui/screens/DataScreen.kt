package com.brewkin.beanbook.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.brewkin.beanbook.data.AppState
import com.brewkin.beanbook.data.BackupManager
import com.brewkin.beanbook.data.DatabaseImportMode
import com.brewkin.beanbook.data.TransferOutcome
import com.brewkin.beanbook.ui.components.CheckIcon
import com.brewkin.beanbook.ui.components.Overline
import com.brewkin.beanbook.ui.components.PrimaryButton
import com.brewkin.beanbook.ui.components.SurfaceCard
import com.brewkin.beanbook.ui.theme.BrewColors
import com.brewkin.beanbook.ui.theme.DMMono

private const val MIME_JSON = "application/json"
private val importMimeTypes = arrayOf("application/json", "text/plain", "application/octet-stream")

/**
 * 「数据与备份」设置区（1.3 从原数据页迁入设置页）。
 * 整块 UI 由调用方嵌在设置页的列表里；单杯配方分享仍在配方详情。
 */
@Composable
fun DataTransferSection(
    state: AppState,
    onExportDatabase: (Uri, (Result<Unit>) -> Unit) -> Unit,
    onImportDatabase: (Uri, DatabaseImportMode, (Result<TransferOutcome>) -> Unit) -> Unit,
    onImportRecipe: (Uri, (Result<TransferOutcome>) -> Unit) -> Unit
) {
    val context = LocalContext.current
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var statusIsError by remember { mutableStateOf(false) }
    var showReplaceConfirm by remember { mutableStateOf(false) }
    var pendingBackupMode by remember { mutableStateOf(DatabaseImportMode.MERGE) }

    fun finish(result: Result<*>, successText: (Any?) -> String = { "操作完成" }) {
        busy = false
        result.onSuccess {
            statusIsError = false
            status = when (it) {
                is TransferOutcome -> it.message
                else -> successText(it)
            }
            Toast.makeText(context, status, Toast.LENGTH_SHORT).show()
        }.onFailure {
            statusIsError = true
            status = it.message ?: "操作失败，请检查文件后重试。"
            Toast.makeText(context, status, Toast.LENGTH_LONG).show()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(MIME_JSON)
    ) { uri ->
        if (uri != null) {
            busy = true
            onExportDatabase(uri) { result -> finish(result) { "数据库已导出" } }
        }
    }
    val importBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            busy = true
            onImportDatabase(uri, pendingBackupMode) { result -> finish(result) }
        }
    }
    val importRecipeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            busy = true
            onImportRecipe(uri) { result -> finish(result) }
        }
    }

    Column {
        SurfaceCard(Modifier.fillMaxWidth(), color = BrewColors.Card) {
            Row(Modifier.padding(vertical = 16.dp)) {
                DataStat("${state.beans.size}", "支豆子", Modifier.weight(1f))
                DataDivider()
                DataStat("${state.recipes.size}", "杯配方", Modifier.weight(1f))
                DataDivider()
                DataStat("${state.customIngredients.size}", "个材料", Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(12.dp))
        SurfaceCard(Modifier.fillMaxWidth(), color = BrewColors.InkSolid, border = null) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(BrewColors.Lime.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⇅", color = BrewColors.Lime, style = MaterialTheme.typography.headlineSmall)
                    }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "整库备份",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        Text(
                            "豆子、配方、材料与照片",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.52f)
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                PrimaryButton(
                    text = if (busy) "处理中…" else "导出数据库",
                    onClick = { exportLauncher.launch(BackupManager.backupFileName()) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy
                )
                Spacer(Modifier.height(9.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DarkOutlineAction("合并导入", Modifier.weight(1f), !busy) {
                        pendingBackupMode = DatabaseImportMode.MERGE
                        importBackupLauncher.launch(importMimeTypes)
                    }
                    DarkOutlineAction("覆盖恢复", Modifier.weight(1f), !busy) {
                        pendingBackupMode = DatabaseImportMode.REPLACE
                        showReplaceConfirm = true
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "合并导入不会覆盖已有记录；覆盖恢复会替换整台设备的数据。备份文件包含照片，请只发送给信任的人。",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.44f)
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        SurfaceCard(Modifier.fillMaxWidth(), color = BrewColors.Card) {
            Column(Modifier.padding(18.dp)) {
                Overline("ONE RECIPE, OFFLINE")
                Spacer(Modifier.height(6.dp))
                Text(
                    "导入一杯配方",
                    style = MaterialTheme.typography.headlineSmall,
                    color = BrewColors.Ink
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "选择朋友发来的 `.brewkinrecipe.json` 文件。里面的豆子信息、层次、笔记和照片都会一起带过来。",
                    style = MaterialTheme.typography.bodySmall,
                    color = BrewColors.Muted
                )
                Spacer(Modifier.height(16.dp))
                PrimaryButton(
                    text = "选择配方文件",
                    onClick = { importRecipeLauncher.launch(importMimeTypes) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        SurfaceCard(Modifier.fillMaxWidth(), color = BrewColors.PaperSoft, border = null) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(BrewColors.LimeSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        CheckIcon(Modifier.size(14.dp), BrewColors.Forest)
                    }
                    Spacer(Modifier.width(9.dp))
                    Text(
                        "怎么把一杯分享给别人",
                        style = MaterialTheme.typography.titleMedium,
                        color = BrewColors.Ink
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "打开目录里任意一杯 → 右上角更多 → 发送配方文件。把生成的 .json 文件通过微信、AirDrop 或文件 App 发给对方，对方在这里导入即可。全程不需要联网。",
                    style = MaterialTheme.typography.bodySmall,
                    color = BrewColors.Muted
                )
            }
        }
        if (status != null) {
            Spacer(Modifier.height(12.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (statusIsError) BrewColors.Rose else BrewColors.LimeSoft,
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    status ?: "",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (statusIsError) BrewColors.RoseDeep else BrewColors.Forest
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "数据只保存在本机。",
            style = MaterialTheme.typography.labelSmall,
            color = BrewColors.MutedLight
        )
    }

    if (busy) {
        Box(
            Modifier
                .fillMaxSize()
                .background(BrewColors.InkSolid.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            Surface(shape = RoundedCornerShape(24.dp), color = BrewColors.Card) {
                Row(
                    Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp,
                        color = BrewColors.LimeDeep
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("正在处理文件…", style = MaterialTheme.typography.bodyMedium, color = BrewColors.Ink)
                }
            }
        }
    }

    if (showReplaceConfirm) {
        AlertDialog(
            onDismissRequest = { showReplaceConfirm = false },
            containerColor = BrewColors.Card,
            title = { Text("覆盖当前数据？", color = BrewColors.Ink) },
            text = {
                Text(
                    "导入文件里的豆子、配方、材料和照片会替换这台设备现有的全部数据。这个操作不能撤销。",
                    color = BrewColors.Muted
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showReplaceConfirm = false
                    importBackupLauncher.launch(importMimeTypes)
                }) { Text("继续覆盖", color = BrewColors.Brown) }
            },
            dismissButton = {
                TextButton(onClick = { showReplaceConfirm = false }) { Text("取消", color = BrewColors.Muted) }
            }
        )
    }
}

@Composable
private fun DarkOutlineAction(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(44.dp),
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.06f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.78f))
        }
    }
}

@Composable
private fun DataStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = DMMono, fontWeight = FontWeight.Medium),
            color = BrewColors.Ink
        )
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = BrewColors.Muted)
    }
}

@Composable
private fun DataDivider() {
    Box(Modifier.width(1.dp).height(28.dp).background(BrewColors.LineSoft))
}

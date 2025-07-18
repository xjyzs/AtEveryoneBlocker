package com.xjyzs.ateveryoneblocker

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xjyzs.ateveryoneblocker.ui.theme.AtEveryoneBlockerTheme
import java.io.File
import java.io.OutputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AtEveryoneBlockerTheme {
                Scaffold (modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainUI(Modifier.padding(innerPadding).fillMaxSize())
                }
            }
        }
    }
}

@Composable
fun MainUI(modifier: Modifier) {
    var blacklistMode by remember { mutableStateOf(false) }
    var groups by remember { mutableStateOf("") }
    val context = LocalContext.current
    val vibrator= context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    val tmpPath = "/data/local/tmp/"
    var process: Process
    var outputStream by remember { mutableStateOf<OutputStream?>(null) }
    LaunchedEffect(Unit) {
        try {
            if (File("/data/local/tmp/blacklistMode").exists()) {
                if (File("/data/local/tmp/blacklistMode").readText() == "true\n") {
                    blacklistMode = true
                }
            }
            if (File("/data/local/tmp/groups").exists()) {
                val txt = File("/data/local/tmp/groups").readText()
                groups = txt.substring(0, txt.length - 1)
            }
            process = ProcessBuilder("su").apply {
                redirectErrorStream(true)
            }.start()
            outputStream = process.outputStream
        } catch (e: Exception) {
            Toast.makeText(context, "请先授予root权限：" + e.message, Toast.LENGTH_SHORT).show()
        }
    }
    Column(modifier.wrapContentSize(Alignment.Center)){
        Row(verticalAlignment = Alignment.CenterVertically){
            Button(
                {
                    blacklistMode = !blacklistMode
                    outputStream!!.write(("echo $blacklistMode > ${tmpPath}blacklistMode\n").toByteArray())
                    outputStream!!.flush()
                },
                colors = ButtonDefaults.buttonColors(
                    Color.Transparent,
                    LocalContentColor.current
                ),
                shape = RectangleShape,
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("黑名单模式", fontSize = 24.sp)
                Spacer(Modifier.weight(1f))
                Switch(checked = blacklistMode, onCheckedChange = {
                    blacklistMode = it
                    clickVibrate(vibrator)
                    outputStream!!.write(("echo $blacklistMode > ${tmpPath}blacklistMode\n").toByteArray())
                    outputStream!!.flush()
                })
            }
        }
        Spacer(Modifier.height(30.dp))
        Text("${if (blacklistMode){"黑"}else{"白"}}名单群聊名称(每行一个)")
        TextField(groups, {
            groups=it
            outputStream!!.write(("echo $groups > ${tmpPath}groups\n").toByteArray())
            outputStream!!.flush()
        }, Modifier.fillMaxWidth(), maxLines = 10)
        Spacer(Modifier.height(60.dp))
        Button({
            outputStream!!.write(("am force-stop com.xiaomi.xmsf\n").toByteArray())
            outputStream!!.flush()
        }, Modifier.fillMaxWidth()) {
            Text("重启 小米服务框架")
        }
    }
}


fun clickVibrate(vibrator: Vibrator){
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val attributes = VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH)
        vibrator.vibrate(
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK),
            attributes
        )
    }
}
package com.xjyzs.ateveryoneblocker

import android.app.Notification
import android.os.Bundle
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class MainHook : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        if (lpparam.packageName != "com.xiaomi.xmsf") return

        try {
            val targetClass = XposedHelpers.findClass(
                "com.xiaomi.push.service.g1", lpparam.classLoader
            )

            XposedHelpers.findAndHookMethod(
                targetClass,
                "w",
                Int::class.javaPrimitiveType,
                Notification::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        LogToFile.log(File("/data/local/tmp/blacklistMode").readText())
                        //val id = param.args[0] as Int
                        val notification = param.args[1] as Notification
                        val extras: Bundle? = notification.extras
                        val title = extras?.getString("android.title") ?: "(无标题)"
                        val text = extras?.getString("android.text") ?: "(无内容)"
                        var blacklistMode = false
                        var groups = ""
                        if (File("/data/local/tmp/blacklistMode").exists()) {
                            if (File("/data/local/tmp/blacklistMode").readText() == "true\n") {
                                blacklistMode = true
                            }
                        }
                        if (File("/data/local/tmp/groups").exists()) {
                            groups = File("/data/local/tmp/groups").readText()
                        }
                        if ("[有全体消息]" in text && text[0] == '[') {
                            if (blacklistMode && title in groups || !blacklistMode && title !in groups) {
                                LogToFile.log(
                                    "${
                                        SimpleDateFormat(
                                            "yyyy-MM-dd HH:mm:ss",
                                            Locale.getDefault()
                                        ).format(
                                            Date()
                                        )
                                    } 成功拦截消息："
                                )
                                LogToFile.log("群聊: $title")
                                LogToFile.log("内容: $text\n")
                                param.result = null
                            }
                        }
                    }
                }
            )
        } catch (e: Throwable) {
            LogToFile.log("Hook失败: ${e.message}")
        }
    }
}

object LogToFile {
    fun log(text: String?,packageName: String="com.xiaomi.xmsf") {
        try {
            val file = File("/data/user/0/${packageName}/xposed_log.txt")
            val fos = FileOutputStream(file, true)
            val writer = OutputStreamWriter(fos)
            writer.write(text + "\n")
            writer.close()
            fos.close()
        } catch (_: Exception) { }
    }
}
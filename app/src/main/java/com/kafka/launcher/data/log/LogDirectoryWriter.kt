package com.kafka.launcher.data.log

import android.content.Context
import com.kafka.launcher.config.LauncherConfig
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class LogDirectoryWriter(context: Context) {
    private val logDir = File(context.getExternalFilesDir(null), LauncherConfig.logDirectoryName).apply { mkdirs() }
    private val manifestFile = File(logDir, LauncherConfig.logManifestFileName)
    private val packageFile = File(logDir, LauncherConfig.logPackageFileName)
    private val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
    private val dispatcher = Dispatchers.IO

    companion object {
        private val mutex = Mutex()
    }

    fun resolve(name: String): File = File(logDir, name)

    suspend fun write(block: suspend () -> Unit) {
        withContext(dispatcher) {
            mutex.withLock {
                block()
                writeManifest(includePackage = false)
                writePackage()
                writeManifest(includePackage = true)
            }
        }
    }

    private fun writeManifest(includePackage: Boolean) {
        val files = logDir.listFiles()
            ?.filter { it.isFile && it.name != LauncherConfig.logManifestFileName && (includePackage || it.name != LauncherConfig.logPackageFileName) }
            ?.sortedBy { it.name }
            ?: emptyList()
        val content = buildString {
            append("{\"generated\":\"")
            append(timestamp())
            append("\",\"files\":[")
            files.forEachIndexed { index, file ->
                append("{\"name\":\"")
                append(file.name)
                append("\",\"size\":")
                append(file.length())
                append(",\"modified\":\"")
                append(formatter.format(Instant.ofEpochMilli(file.lastModified()).atZone(ZoneId.systemDefault())))
                append("\"}")
                if (index < files.lastIndex) append(",")
            }
            append("]}")
        }
        manifestFile.writeText(content)
    }

    private fun writePackage() {
        val files = logDir.listFiles()
            ?.filter { it.isFile && it.name != LauncherConfig.logPackageFileName }
            ?.sortedBy { it.name }
            ?: emptyList()
        if (files.isEmpty()) {
            if (packageFile.exists()) {
                packageFile.delete()
            }
            return
        }
        ZipOutputStream(packageFile.outputStream().buffered()).use { zip ->
            files.forEach { file ->
                zip.putNextEntry(ZipEntry(file.name))
                file.inputStream().buffered().use { input ->
                    input.copyTo(zip)
                }
                zip.closeEntry()
            }
        }
    }

    private fun timestamp(): String = formatter.format(ZonedDateTime.now())
}

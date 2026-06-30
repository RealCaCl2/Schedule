package com.cacl2.schedule.model

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.util.Base64
import com.cacl2.schedule.data.local.entity.CourseEntity
import com.cacl2.schedule.data.local.entity.SemesterEntity
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.EncodeHintType
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.Result
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.encoder.Encoder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.Inflater

data class SharedScheduleData(
    val version: Int = 1,
    val appName: String = "轻课表",
    val semester: SharedSemester,
    val courses: List<SharedCourse>
)

data class SharedSemester(
    val name: String,
    val startDate: String,
    val totalWeeks: Int,
    val periodsPerDay: Int
)

data class SharedCourse(
    val courseName: String,
    val teacher: String,
    val location: String,
    val dayOfWeek: Int,
    val startPeriod: Int,
    val endPeriod: Int,
    val startWeek: Int,
    val endWeek: Int,
    val weekType: Int = 0
)

object SharedScheduleCodec {

    // ── Binary codec — much tighter than JSON+gzip ──

    fun encode(data: SharedScheduleData): String {
        val buf = ByteArrayOutputStream()
        val out = java.io.DataOutputStream(buf)

        // Header: version + app name (for compatibility check)
        out.writeByte(1) // version
        val appBytes = data.appName.toByteArray(Charsets.UTF_8)
        out.writeByte(appBytes.size)
        out.write(appBytes)

        // Semester
        val semNameBytes = data.semester.name.toByteArray(Charsets.UTF_8)
        out.writeShort(semNameBytes.size)
        out.write(semNameBytes)
        // startDate: "2025-09-01" → store as year(1B) + month(1B) + day(1B)
        val dateParts = data.semester.startDate.split("-")
        val (year, month, day) = if (dateParts.size == 3) {
            val y = dateParts[0].toIntOrNull()
            val m = dateParts[1].toIntOrNull()
            val d = dateParts[2].toIntOrNull()
            if (y != null && m != null && d != null && m in 1..12 && d in 1..31) {
                Triple(y, m, d)
            } else {
                val today = java.time.LocalDate.now()
                Triple(today.year, today.monthValue, today.dayOfMonth)
            }
        } else {
            // Fallback: use today's date rather than a hardcoded Jan 1st
            val today = java.time.LocalDate.now()
            Triple(today.year, today.monthValue, today.dayOfMonth)
        }
        out.writeByte(year - 2000) // 25 = 2025
        out.writeByte(month)
        out.writeByte(day)
        out.writeByte(data.semester.totalWeeks)
        out.writeByte(data.semester.periodsPerDay)

        // Courses
        out.writeByte(data.courses.size)
        for (c in data.courses) {
            val cn = c.courseName.toByteArray(Charsets.UTF_8)
            out.writeByte(cn.size)
            out.write(cn)
            val tn = c.teacher.toByteArray(Charsets.UTF_8)
            out.writeByte(tn.size)
            out.write(tn)
            val ln = c.location.toByteArray(Charsets.UTF_8)
            out.writeByte(ln.size)
            out.write(ln)
            // Pack dayOfWeek(3bit) + weekType(2bit) into 1 byte
            val packed = ((c.dayOfWeek and 0x07) shl 2) or (c.weekType and 0x03)
            out.writeByte(packed)
            out.writeByte(c.startPeriod)
            out.writeByte(c.endPeriod)
            out.writeByte(c.startWeek)
            out.writeByte(c.endWeek)
        }

        out.flush()
        val raw = buf.toByteArray()
        val compressed = deflate(raw)
        return Base64.encodeToString(compressed, Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING)
    }

    fun decode(encoded: String): SharedScheduleData? {
        return try {
            val compressed = Base64.decode(encoded, Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING)
            val raw = inflate(compressed)
            val input = java.io.DataInputStream(java.io.ByteArrayInputStream(raw))

            val version = input.readByte().toInt()
            val appLen = input.readUnsignedByte()
            val appBytes = ByteArray(appLen)
            input.readFully(appBytes)
            val appName = String(appBytes, Charsets.UTF_8)

            // Semester
            val semNameLen = input.readShort().toInt()
            val semNameBytes = ByteArray(semNameLen)
            input.readFully(semNameBytes)
            val semName = String(semNameBytes, Charsets.UTF_8)
            val year = input.readUnsignedByte() + 2000
            val month = input.readUnsignedByte()
            val day = input.readUnsignedByte()
            val startDate = String.format("%04d-%02d-%02d", year, month, day)
            val totalWeeks = input.readUnsignedByte()
            val periodsPerDay = input.readUnsignedByte()

            val semester = SharedSemester(semName, startDate, totalWeeks, periodsPerDay)

            val courseCount = input.readUnsignedByte()
            val courses = mutableListOf<SharedCourse>()
            for (i in 0 until courseCount) {
                val cnLen = input.readUnsignedByte()
                val cnBytes = ByteArray(cnLen)
                input.readFully(cnBytes)
                val courseName = String(cnBytes, Charsets.UTF_8)

                val tnLen = input.readUnsignedByte()
                val tnBytes = ByteArray(tnLen)
                input.readFully(tnBytes)
                val teacher = String(tnBytes, Charsets.UTF_8)

                val lnLen = input.readUnsignedByte()
                val lnBytes = ByteArray(lnLen)
                input.readFully(lnBytes)
                val location = String(lnBytes, Charsets.UTF_8)

                val packed = input.readUnsignedByte()
                val dayOfWeek = (packed shr 2) and 0x07
                val weekType = packed and 0x03
                val startPeriod = input.readUnsignedByte()
                val endPeriod = input.readUnsignedByte()
                val startWeek = input.readUnsignedByte()
                val endWeek = input.readUnsignedByte()

                courses.add(SharedCourse(courseName, teacher, location, dayOfWeek, startPeriod, endPeriod, startWeek, endWeek, weekType))
            }

            SharedScheduleData(version = 1, appName = appName, semester = semester, courses = courses)
        } catch (_: Exception) {
            null
        }
    }

    fun generateQrBitmap(encoded: String, moduleSize: Int = 6): Bitmap {
        return try {
            // Use Encoder directly to get the minimum QR version for the data.
            // Then build a BitMatrix at that exact size for pixel-perfect rendering.
            val qrCode = Encoder.encode(
                encoded,
                com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.M,
                mapOf(EncodeHintType.MARGIN to 2)
            )
            val byteMatrix = qrCode.matrix
            val modules = byteMatrix.width // e.g. 29 for version 3
            val size = modules * moduleSize
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
            for (x in 0 until modules) {
                for (y in 0 until modules) {
                    if (byteMatrix.get(x, y).toInt() == 1) {
                        canvas.drawRect(
                            (x * moduleSize).toFloat(), (y * moduleSize).toFloat(),
                            ((x + 1) * moduleSize).toFloat(), ((y + 1) * moduleSize).toFloat(),
                            paint
                        )
                    }
                }
            }
            bitmap
        } catch (e: WriterException) {
            // Fallback
            Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        }
    }

    fun toEntity(course: SharedCourse, semesterId: String, colorIndex: Int): CourseEntity {
        return CourseEntity(
            courseName = course.courseName,
            teacher = course.teacher,
            location = course.location,
            dayOfWeek = course.dayOfWeek,
            startPeriod = course.startPeriod,
            endPeriod = course.endPeriod,
            startWeek = course.startWeek,
            endWeek = course.endWeek,
            weekType = course.weekType,
            colorIndex = colorIndex,
            semesterId = semesterId
        )
    }

    fun decodeQrFromUri(uri: Uri, contentResolver: android.content.ContentResolver): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream) ?: return null
            inputStream.close()

            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            val source = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val reader = QRCodeReader()
            val result: Result = reader.decode(binaryBitmap)
            result.text
        } catch (_: Exception) {
            null
        }
    }

    fun toSharedCourse(entity: CourseEntity): SharedCourse {
        return SharedCourse(
            courseName = entity.courseName,
            teacher = entity.teacher,
            location = entity.location,
            dayOfWeek = entity.dayOfWeek,
            startPeriod = entity.startPeriod,
            endPeriod = entity.endPeriod,
            startWeek = entity.startWeek,
            endWeek = entity.endWeek,
            weekType = entity.weekType
        )
    }

    private fun deflate(data: ByteArray): ByteArray {
        val deflater = Deflater(Deflater.BEST_COMPRESSION, true) // nowrap = no header
        deflater.setInput(data)
        deflater.finish()
        val bos = ByteArrayOutputStream()
        val buf = ByteArray(512)
        while (!deflater.finished()) {
            val n = deflater.deflate(buf)
            bos.write(buf, 0, n)
        }
        deflater.end()
        return bos.toByteArray()
    }

    private fun inflate(data: ByteArray): ByteArray {
        val inflater = Inflater(true) // nowrap = no header
        inflater.setInput(data)
        val bos = ByteArrayOutputStream()
        val buf = ByteArray(512)
        while (!inflater.finished()) {
            val n = inflater.inflate(buf)
            bos.write(buf, 0, n)
        }
        inflater.end()
        return bos.toByteArray()
    }
}

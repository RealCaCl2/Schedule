package com.cacl2.schedule.data.parser

import com.cacl2.schedule.data.local.entity.CourseEntity
import com.cacl2.schedule.util.CourseColorMapper
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

object QiangZhiParser {

    data class ParseResult(
        val courses: List<CourseEntity>,
        val errors: List<String> = emptyList()
    )

    private data class WeekInfo(val startWeek: Int, val endWeek: Int, val weekType: Int)

    fun parse(html: String): ParseResult {
        val errors = mutableListOf<String>()
        val courses = mutableListOf<CourseEntity>()

        try {
            val document: Document = Jsoup.parse(html)
            val table = document.selectFirst("#kbtable")
                ?: document.select("table").firstOrNull { it.selectFirst("div.kbcontent, div.kbcontent1") != null }

            val scheduleDivs = (table?.select("div.kbcontent") ?: document.select("div.kbcontent"))
                .filterNot { isBlankBlock(it.html()) }

            if (scheduleDivs.isEmpty()) {
                return ParseResult(emptyList(), listOf("Schedule blocks not found (div.kbcontent)."))
            }

            var fallbackRowCounter = 0
            for (div in scheduleDivs) {
                try {
                    val parentTd = div.parent()
                    val parentTr = parentTd?.parent()

                    val rowFallback = parseRowPeriodFallback(parentTr)
                        ?: run {
                            fallbackRowCounter++
                            Pair((fallbackRowCounter - 1) * 2 + 1, (fallbackRowCounter - 1) * 2 + 2)
                        }

                    val dayFromColumn = if (parentTd != null && parentTr != null) {
                        val rowCells = parentTr.children().filter { it.tagName() == "td" }
                        val tdIndex = rowCells.indexOf(parentTd)
                        if (tdIndex in 1..7) tdIndex else 0
                    } else {
                        0
                    }

                    courses += parseKbContentDiv(
                        div = div,
                        fallbackPeriod = rowFallback,
                        fallbackDay = dayFromColumn,
                        errors = errors
                    )
                } catch (e: Exception) {
                    errors.add("Failed to parse a schedule cell: ${e.message}")
                }
            }
        } catch (e: Exception) {
            return ParseResult(emptyList(), listOf("Failed to parse HTML: ${e.message}"))
        }

        if (courses.isEmpty() && errors.isEmpty()) {
            errors.add("No courses were parsed from the schedule page.")
        }

        return ParseResult(courses, errors)
    }

    private fun parseKbContentDiv(
        div: Element,
        fallbackPeriod: Pair<Int, Int>,
        fallbackDay: Int,
        errors: MutableList<String>
    ): List<CourseEntity> {
        val divHtml = div.html().trim()
        val blocks = divHtml
            .split(Regex("(?i)-{5,}\\s*<br\\s*/?>"))
            .map { it.trim() }
            .filter { it.isNotBlank() && !isBlankBlock(it) }

        if (blocks.isEmpty()) return emptyList()

        if (blocks.size == 1) {
            return listOfNotNull(
                parseSingleCourseFromElement(
                    element = div,
                    originalDiv = div,
                    fallbackPeriod = fallbackPeriod,
                    fallbackDay = fallbackDay,
                    errors = errors
                )
            )
        }

        return blocks.mapNotNull { block ->
            val wrapped = Jsoup.parseBodyFragment("<div>$block</div>").selectFirst("div") ?: return@mapNotNull null
            parseSingleCourseFromElement(
                element = wrapped,
                originalDiv = div,
                fallbackPeriod = fallbackPeriod,
                fallbackDay = fallbackDay,
                errors = errors
            )
        }
    }

    private fun parseSingleCourseFromElement(
        element: Element,
        originalDiv: Element,
        fallbackPeriod: Pair<Int, Int>,
        fallbackDay: Int,
        errors: MutableList<String>
    ): CourseEntity? {
        val courseName = extractCourseName(element)
        if (courseName.isBlank()) return null

        val teacher = extractFontByTitle(element, listOf("老师", "教师"))
        val location = extractFontByTitle(element, listOf("教室"))
        val weekPeriodText = extractFontByTitle(element, listOf("周次(节次)", "周次 (节次)", "周次"))
            .ifBlank { extractWeekPeriodFallback(element) }

        if (weekPeriodText.isBlank()) {
            errors.add("Week/period info missing for course: $courseName")
            return null
        }

        val weekInfo = parseWeekRange(weekPeriodText)
        if (weekInfo == null) {
            errors.add("Failed to parse week range '$weekPeriodText' for course: $courseName")
            return null
        }

        val periodInfo = parsePeriodRange(weekPeriodText, fallbackPeriod)

        val dayOfWeek = extractDayOfWeek(originalDiv, fallbackDay)
        if (dayOfWeek !in 1..7) {
            errors.add("Failed to determine weekday for course: $courseName")
            return null
        }

        return CourseEntity(
            courseName = courseName,
            teacher = teacher,
            location = location,
            dayOfWeek = dayOfWeek,
            startPeriod = periodInfo.first,
            endPeriod = periodInfo.second,
            startWeek = weekInfo.startWeek,
            endWeek = weekInfo.endWeek,
            weekType = weekInfo.weekType,
            colorIndex = CourseColorMapper.getColorIndex(courseName)
        )
    }

    private fun extractCourseName(element: Element): String {
        val withoutFonts = element.clone().apply { select("font").remove() }
        val lines = withoutFonts.html()
            .split(Regex("(?i)<br\\s*/?>"))
            .map { Jsoup.parse(it).text().trim().replace("\u00A0", " ") }
            .filter { it.isNotBlank() && !it.matches(Regex("-{3,}")) }

        return lines.joinToString("\n").trim()
    }

    private fun extractFontByTitle(element: Element, titleHints: List<String>): String {
        val direct = element.select("font[title]").firstOrNull { font ->
            val normalized = font.attr("title").replace(" ", "")
            titleHints.any { hint -> normalized.contains(hint.replace(" ", "")) }
        }
        if (direct != null) return direct.text().trim()

        return element.select("font")
            .firstOrNull { font ->
                val title = font.attr("title")
                titleHints.any { hint -> title.contains(hint) }
            }
            ?.text()
            ?.trim()
            .orEmpty()
    }

    private fun extractWeekPeriodFallback(element: Element): String {
        return element.select("font")
            .firstOrNull { font ->
                val text = font.text()
                text.contains("周") && text.contains("节")
            }
            ?.text()
            ?.trim()
            .orEmpty()
    }

    private fun parseWeekRange(text: String): WeekInfo? {
        val normalized = text.replace("（", "(").replace("）", ")").replace(" ", "")

        Regex("""(\d+)-(\d+)\((周|单周|双周)\)""").find(normalized)?.let { m ->
            val start = m.groupValues[1].toIntOrNull() ?: return null
            val end = m.groupValues[2].toIntOrNull() ?: return null
            val weekType = when (m.groupValues[3]) {
                "单周" -> 1
                "双周" -> 2
                else -> 0
            }
            return WeekInfo(start, end, weekType)
        }

        Regex("""(\d+)\((周|单周|双周)\)""").find(normalized)?.let { m ->
            val week = m.groupValues[1].toIntOrNull() ?: return null
            val weekType = when (m.groupValues[2]) {
                "单周" -> 1
                "双周" -> 2
                else -> 0
            }
            return WeekInfo(week, week, weekType)
        }

        Regex("""(\d+)-(\d+)(周|单周|双周)""").find(normalized)?.let { m ->
            val start = m.groupValues[1].toIntOrNull() ?: return null
            val end = m.groupValues[2].toIntOrNull() ?: return null
            val weekType = when (m.groupValues[3]) {
                "单周" -> 1
                "双周" -> 2
                else -> 0
            }
            return WeekInfo(start, end, weekType)
        }

        return null
    }

    private fun parsePeriodRange(text: String, fallbackPeriod: Pair<Int, Int>): Pair<Int, Int> {
        val normalized = text.replace("（", "(").replace("）", ")")

        Regex("""\[([^\]]+)节]""").find(normalized)?.groupValues?.getOrNull(1)?.let { raw ->
            val nums = Regex("""\d+""").findAll(raw).mapNotNull { it.value.toIntOrNull() }.toList()
            if (nums.isNotEmpty()) return Pair(nums.first(), nums.last())
        }

        Regex("""(\d{1,2})(?:-(\d{1,2}))+节""").find(normalized)?.let { m ->
            val nums = Regex("""\d+""").findAll(m.value).mapNotNull { it.value.toIntOrNull() }.toList()
            if (nums.isNotEmpty()) return Pair(nums.first(), nums.last())
        }

        return fallbackPeriod
    }

    private fun parseRowPeriodFallback(row: Element?): Pair<Int, Int>? {
        row ?: return null
        val firstCellText = row.children()
            .firstOrNull { it.tagName() == "td" }
            ?.text()
            ?.replace(" ", "")
            ?: return null

        val numbers = Regex("""\d+""").findAll(firstCellText).mapNotNull { it.value.toIntOrNull() }.toList()
        if (numbers.isEmpty()) return null
        if (numbers.size == 1) return Pair(numbers[0], numbers[0])
        return Pair(numbers.first(), numbers.last())
    }

    private fun extractDayOfWeek(div: Element, fallbackDay: Int): Int {
        val id = div.id().trim()
        if (id.isNotBlank()) {
            Regex("""-(\d)-[12]$""").find(id)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let {
                if (it in 1..7) return it
            }
            Regex("""-(\d)-""").find(id)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let {
                if (it in 1..7) return it
            }
        }

        return if (fallbackDay in 1..7) fallbackDay else 0
    }

    private fun isBlankBlock(html: String): Boolean {
        val text = Jsoup.parse(html).text().replace("\u00A0", " ").trim()
        return text.isBlank()
    }
}

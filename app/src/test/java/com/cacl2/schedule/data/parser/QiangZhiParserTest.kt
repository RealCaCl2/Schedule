package com.cacl2.schedule.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QiangZhiParserTest {

    @Test
    fun parse_single_course_success() {
        val html = """
            <table id="kbtable">
              <tr><th>time</th><th>Mon</th></tr>
              <tr>
                <td>01-02节</td>
                <td>
                  <div id="ABC-1-2" class="kbcontent">
                    概率论与数理统计<br/>
                    <font title="老师">嵇绍春</font><br/>
                    <font title="周次(节次)">1-14(周)[01-02节]</font><br/>
                    <font title="教室">12305</font><br/>
                  </div>
                </td>
              </tr>
            </table>
        """.trimIndent()

        val result = QiangZhiParser.parse(html)

        assertEquals("errors=${result.errors}", 1, result.courses.size)
        val course = result.courses.first()
        assertEquals("概率论与数理统计", course.courseName)
        assertEquals("嵇绍春", course.teacher)
        assertEquals("12305", course.location)
        assertEquals(1, course.dayOfWeek)
        assertEquals(1, course.startWeek)
        assertEquals(14, course.endWeek)
        assertEquals(0, course.weekType)
        assertEquals(1, course.startPeriod)
        assertEquals(2, course.endPeriod)
    }

    @Test
    fun parse_multi_course_in_one_cell() {
        val html = """
            <table id="kbtable">
              <tr><th>time</th><th>Thu</th></tr>
              <tr>
                <td>05-06节</td>
                <td>
                  <div id="DEF-4-2" class="kbcontent">
                    电子商务安全<br/>
                    <font title="老师">王晴</font><br/>
                    <font title="周次(节次)">1-12(单周)[05-06节]</font><br/>
                    <font title="教室">YFJ0103</font><br/>
                    ---------------------<br/>
                    管理信息系统<br/>
                    <font title="老师">曹培红</font><br/>
                    <font title="周次(节次)">1-12(双周)[05-06节]</font><br/>
                    <font title="教室">YFJ0108</font><br/>
                  </div>
                </td>
              </tr>
            </table>
        """.trimIndent()

        val result = QiangZhiParser.parse(html)

        assertEquals("errors=${result.errors}", 2, result.courses.size)
        assertTrue(result.courses.any { it.courseName == "电子商务安全" && it.weekType == 1 })
        assertTrue(result.courses.any { it.courseName == "管理信息系统" && it.weekType == 2 })
    }

    @Test
    fun parse_single_week_and_three_period_span() {
        val html = """
            <table id="kbtable">
              <tr><th>time</th><th>Wed</th></tr>
              <tr>
                <td>07-09节</td>
                <td>
                  <div id="GHI-3-2" class="kbcontent">
                    国家安全教育和军事理论2<br/>
                    <font title="老师">张前程</font><br/>
                    <font title="周次(节次)">5(周)[07-08-09节]</font><br/>
                    <font title="教室">YFJ0103</font><br/>
                  </div>
                </td>
              </tr>
            </table>
        """.trimIndent()

        val result = QiangZhiParser.parse(html)

        assertEquals("errors=${result.errors}", 1, result.courses.size)
        val course = result.courses.first()
        assertEquals(5, course.startWeek)
        assertEquals(5, course.endWeek)
        assertEquals(7, course.startPeriod)
        assertEquals(9, course.endPeriod)
    }

    @Test
    fun parse_course_name_with_subtitle_line() {
        val html = """
            <table id="kbtable">
              <tr><th>time</th><th>Fri</th></tr>
              <tr>
                <td>07-08节</td>
                <td>
                  <div id="JKL-5-2" class="kbcontent">
                    大学体育4<br/>
                    (男子篮球提高班)<br/>
                    <font title="老师">张家银</font><br/>
                    <font title="周次(节次)">1-17(周)[07-08节]</font><br/>
                    <font title="教室">篮球场</font><br/>
                  </div>
                </td>
              </tr>
            </table>
        """.trimIndent()

        val result = QiangZhiParser.parse(html)

        assertEquals("errors=${result.errors}", 1, result.courses.size)
        assertEquals("大学体育4\n(男子篮球提高班)", result.courses.first().courseName)
    }
}

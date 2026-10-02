package com.ht.intelza.data.csv

import org.junit.Assert.assertEquals
import org.junit.Test

class CsvTest {

    @Test
    fun `parses quoted cells and escaped quotes`() {
        val rows = Csv.parse("name,note\r\n\"Rao, Asha\",\"said \"\"hi\"\"\"\nBen,\n")
        assertEquals(listOf(listOf("name", "note"), listOf("Rao, Asha", "said \"hi\""), listOf("Ben", "")), rows)
    }

    @Test
    fun `detects semicolons and tabs`() {
        assertEquals(listOf(listOf("a", "b"), listOf("c", "d")), Csv.parse("a;b\nc;d"))
        assertEquals(listOf(listOf("a", "b")), Csv.parse("a\tb"))
    }

    @Test
    fun `skips blank lines and byte order mark`() {
        assertEquals(listOf(listOf("x")), Csv.parse("﻿x\n\n,\n"))
    }

    @Test
    fun `writes values that need quoting`() {
        assertEquals("a,\"b,c\",\"say \"\"hi\"\"\"\r\n1,,\r\n", Csv.write(listOf(listOf("a", "b,c", "say \"hi\""), listOf(1, null, ""))))
    }

    @Test
    fun `round trips`() {
        val rows = listOf(listOf("Name", "Score"), listOf("O'Neil, Sam", "75%"), listOf("Line\nbreak", "x"))
        assertEquals(rows, Csv.parse(Csv.write(rows)))
    }

    @Test
    fun `student import uses header names`() {
        val rows = StudentImport.parse("Roll No,Student Name,Card\n12,Asha Rao,3\n13,Ben Li,\n,,\n")
        assertEquals(
            listOf(StudentImportRow("Asha Rao", "12", 3), StudentImportRow("Ben Li", "13", null)),
            rows,
        )
    }

    @Test
    fun `student import without header reads name then roll then card`() {
        val rows = StudentImport.parse("Kanamesh,7\nZoe\n")
        assertEquals(listOf(StudentImportRow("Kanamesh", "7", null), StudentImportRow("Zoe", "", null)), rows)
    }
}

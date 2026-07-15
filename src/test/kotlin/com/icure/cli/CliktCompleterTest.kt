package com.icure.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import org.jline.reader.Candidate
import org.jline.reader.LineReaderBuilder
import org.jline.reader.Parser
import org.jline.reader.impl.DefaultParser
import org.jline.terminal.TerminalBuilder
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class CliktCompleterTest {
    private class CouchDb : CliktCommand(name = "couch-db") {
        val user by option("-u", "--user", help = "Credentials user:password")
        val verbose by option("--verbose", help = "Verbose output").flag()
        val server by argument("server").optional()
        override fun run() {}
    }

    private class ListDatabases : CliktCommand(name = "list-databases") {
        val format by option("--format", help = "Output format").choice("json", "text")
        val limit by option("--limit", help = "Maximum results")
        override fun run() {}
    }

    private class AddShardReplica : CliktCommand(name = "add-shard-replica") {
        val db by option("--db")
        override fun run() {}
    }

    private fun complete(line: String): List<Candidate> {
        val root = CouchDb().subcommands(ListDatabases(), AddShardReplica())
        val completer = CliktCompleter(listOf(root))
        val reader = LineReaderBuilder.builder()
            .terminal(TerminalBuilder.builder().dumb(true).build())
            .build()
        val parsed = DefaultParser().parse(line, line.length, Parser.ParseContext.COMPLETE)
        val candidates = mutableListOf<Candidate>()
        completer.complete(reader, parsed, candidates)
        return candidates
    }

    private fun values(line: String) = complete(line).map { it.value() }

    @Test
    fun `completes root command names`() {
        assertContains(values("couch"), "couch-db")
    }

    @Test
    fun `completes subcommand names`() {
        assertContains(values("couch-db li"), "list-databases")
    }

    @Test
    fun `completes subcommand options`() {
        assertContains(values("couch-db list-databases --"), "--format")
    }

    @Test
    fun `completes option values declared as choices`() {
        val vals = values("couch-db list-databases --format ")
        assertContains(vals, "json")
        assertContains(vals, "text")
    }

    @Test
    fun `completes root options`() {
        val vals = values("couch-db -")
        assertContains(vals, "-u")
        assertContains(vals, "--verbose")
    }

    @Test
    fun `still completes subcommands after root options and arguments`() {
        assertContains(values("couch-db -u admin:pass http://localhost:5984 li"), "list-databases")
    }

    @Test
    fun `flag options do not swallow the following word`() {
        assertContains(values("couch-db --verbose li"), "list-databases")
    }

    @Test
    fun `does not offer subcommands where an option value is expected`() {
        assertFalse(values("couch-db -u ").contains("list-databases"))
    }

    @Test
    fun `does not re-offer options already used`() {
        assertFalse(values("couch-db list-databases --format json --").contains("--format"))
    }

    @Test
    fun `offers the help option registered by clikt`() {
        assertContains(values("couch-db list-databases --h"), "--help")
    }

    @Test
    fun `option candidates carry help descriptions`() {
        val format = complete("couch-db list-databases --").first { it.value() == "--format" }
        assertEquals("Output format", format.descr())
    }
}

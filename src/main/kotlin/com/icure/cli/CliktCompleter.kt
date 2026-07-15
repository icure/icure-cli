package com.icure.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.Option
import org.jline.reader.Candidate
import org.jline.reader.Completer
import org.jline.reader.LineReader
import org.jline.reader.ParsedLine

/**
 * JLine completer driven by the Clikt command tree: completes subcommands, options
 * (with their help text), option values and argument values at every level, tolerating
 * options and arguments interleaved before subcommands (e.g. `couch-db -u a:b <url> <sub>`).
 */
class CliktCompleter(commands: List<CliktCommand>) : Completer {
    // getFormattedHelp() forces Clikt to create the Context of the whole subtree, without
    // which optionHelp()/currentContext throw "Context accessed before parse has been called".
    private val roots = commands.onEach { runCatching { it.getFormattedHelp() } }

    override fun complete(reader: LineReader, line: ParsedLine, candidates: MutableList<Candidate>) {
        // A completer that throws silently disables completion for the whole line in JLine.
        runCatching { doComplete(reader, line, candidates) }
    }

    private fun doComplete(reader: LineReader, line: ParsedLine, candidates: MutableList<Candidate>) {
        val words = line.words()
        var current: CliktCommand? = null
        var subcommands = roots.associateBy { it.commandName }
        var pendingValueOption: Option? = null
        val usedOptions = mutableSetOf<String>()

        for (i in 0 until line.wordIndex()) {
            val word = words[i]
            val subcommand = subcommands[word]
            when {
                pendingValueOption != null -> pendingValueOption = null
                subcommand != null -> {
                    current = subcommand
                    subcommands = subcommand.registeredSubcommands().associateBy { it.commandName }
                    usedOptions.clear()
                }
                word.startsWith("-") -> {
                    val name = word.substringBefore('=')
                    usedOptions += name
                    val option = current?.registeredOptions()
                        ?.find { name in it.names || name in it.secondaryNames }
                    if (option != null && option.nvalues.first > 0 && '=' !in word) {
                        pendingValueOption = option
                    }
                }
                // anything else is a positional argument value: consume and stay on this command
            }
        }

        pendingValueOption?.let {
            it.completionCandidates.makeJLineCompleter().complete(reader, line, candidates)
            return
        }

        val prefix = line.word().substring(0, line.wordCursor())
        if (prefix.startsWith("-")) {
            current?.let { cmd ->
                val context = cmd.contextOrNull()
                cmd.registeredOptions()
                    .filterNot { it.hidden || (it.names + it.secondaryNames).any { name -> name in usedOptions } }
                    .forEach { option ->
                        val help = context
                            ?.let { ctx -> runCatching { option.optionHelp(ctx) }.getOrNull() }
                            ?.takeIf { it.isNotBlank() }
                        (option.names + option.secondaryNames).forEach { name ->
                            candidates += Candidate(name, name, null, help, null, null, true)
                        }
                    }
            }
        } else {
            subcommands.values.forEach { sub ->
                val help = sub.contextOrNull()
                    ?.let { ctx -> runCatching { sub.commandHelp(ctx) }.getOrNull() }
                    ?.takeIf { it.isNotBlank() }
                candidates += Candidate(sub.commandName, sub.commandName, null, help, null, null, true)
            }
            current?.registeredArguments()?.forEach { argument ->
                argument.completionCandidates.makeJLineCompleter().complete(reader, line, candidates)
            }
        }
    }

    private fun CliktCommand.contextOrNull(): Context? = runCatching { currentContext }.getOrNull()
}

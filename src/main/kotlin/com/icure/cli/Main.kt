@file:OptIn(ExperimentalSerializationApi::class)

package com.icure.cli

import com.github.ajalt.clikt.completion.CompletionCandidates
import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.output.Localization
import com.github.ajalt.clikt.output.ParameterFormatter
import com.github.ajalt.clikt.parameters.arguments.Argument
import com.github.ajalt.clikt.parameters.options.Option
import org.jline.reader.*;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.DefaultParser.Bracket;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import kotlinx.serialization.ExperimentalSerializationApi
import org.jline.builtins.Completers
import org.jline.reader.impl.completer.AggregateCompleter
import org.jline.reader.impl.completer.ArgumentCompleter
import org.jline.reader.impl.completer.NullCompleter
import org.jline.reader.impl.completer.StringsCompleter
import java.io.File
import java.net.URI
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CancellationException
import java.util.jar.JarFile

class RootCmd : CliktCommand() {
    override fun run() {}
}

class Exit : CliktCommand() {
    override fun run() { System.exit(0) }
}

class CommandsJarManager {
    fun commands(): List<CliktCommand> {
        val plugins =
            try {
                File(this.javaClass.getProtectionDomain().codeSource.location.toURI()).takeIf { it.exists() }?.parentFile?.parentFile?.let {
                    File(it, "plugins")
                }?.takeIf { it.isDirectory() }?.toPath()?.toRealPath()?.toString()
            } catch (e: Exception) {
                null
            } ?: try {
                File("./plugins").toPath().toRealPath().toString().let { File(it) }.takeIf { it.exists() && it.isDirectory() }?.path
            } catch (e: Exception) {
                null
            } ?: try {
                File("~/.iqr/plugins").toPath().toRealPath().toString().let { File(it) }.takeIf { it.exists() && it.isDirectory() }?.path
            } catch (e: Exception) {
                null
            } ?: try {
                File("./icure-cli/plugins").toPath().toRealPath().toString().let { File(it) }.takeIf { it.exists() && it.isDirectory() }?.path
            } catch (e: Exception) {
                null
            } ?: throw IllegalStateException("No plugins directory found")

        println("Loading commands from $plugins")

        val classLoader = this.javaClass.classLoader

        val commands = Files.walk(Path.of(plugins))
            .filter { it.toString().endsWith(".jar") }
            .map {
                JarFile(it.toFile().absolutePath) to URLClassLoader.newInstance(
                    arrayOf(
                        URI.create("jar:file:${it.toFile().absolutePath}!/").toURL()
                    ), classLoader
                )
            }
            .toList()
            .flatMap { (jar, cl) ->
                val mainClasses = jar.manifest.mainAttributes.getValue("Root-Commands").split(",")
                mainClasses.map { cl.loadClass(it).getConstructor().newInstance() as CliktCommand }
            }

        return commands
    }
}

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        val parser = DefaultParser()
        parser.setEofOnUnclosedBracket(Bracket.CURLY, Bracket.ROUND, Bracket.SQUARE)
        parser.isEofOnUnclosedQuote = true
        parser.setRegexCommand("[:]{0,1}[a-zA-Z!]{1,}\\S*") // change default regex to support shell commands
        val terminal: Terminal = TerminalBuilder.builder().build()
        if (terminal.width == 0 || terminal.height == 0) {
            terminal.size = Size(120, 40) // hard coded terminal size when redirecting
        }
        val executeThread = Thread.currentThread()
        terminal.handle(Terminal.Signal.INT) { _ -> executeThread.interrupt() }

        val reader = LineReaderBuilder.builder()
            .terminal(terminal)
            .parser(parser)
            .completer(CommandsJarManager().commands().makeJLineTreeCompleter())
            .variable(LineReader.SECONDARY_PROMPT_PATTERN, "%M%P > ")
            .variable(LineReader.INDENTATION, 2)
            .variable(LineReader.LIST_MAX, 100)
            .option(LineReader.Option.INSERT_BRACKET, true)
            .option(LineReader.Option.EMPTY_WORD_OPTIONS, false)
            .option(LineReader.Option.USE_FORWARD_SLASH, true) // use forward slash in directory separator
            .option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)
            .build()

        while (true) {
            val commands = CommandsJarManager().commands() + Exit()
            try {
                val line = reader.readLine("IQR > ", "", null as MaskingCallback?, null)
                val pl = reader.parser.parse(line, 0)
                val argv = pl.words().toTypedArray()
                val cmd = commands.firstOrNull { it.commandName == argv.firstOrNull() }
                if (cmd == null) {
                    println("${argv.firstOrNull()}: Command not found")
                    println("Available commands: ${commands.joinToString(", ") { it.commandName }}")
                    continue
                } else {
                    cmd.parse(argv.drop(1).toTypedArray())
                    println()
                }
            } catch (e: PrintHelpMessage) {
                println(e.context?.command?.getFormattedHelp() ?: "No help available")
                println()
            } catch (e: UsageError) {
                println(e.formatMessage(e.context?.localization ?: object : Localization {}, ParameterFormatter.Plain))
                println()
            } catch (e: EndOfFileException) {
                break
            } catch (e: UserInterruptException) {
                continue
            } catch (e: CancellationException) {
                continue
            } catch (e: InterruptedException) {
                continue
            } catch (e: Throwable) {
                println("ERROR: ${e.cause}")
                println()
                continue
            }
        }
    } else RootCmd().subcommands(
        Exit(),
        *(CommandsJarManager().commands().toTypedArray())
    ).main(args)
}

internal fun List<CliktCommand>.makeJLineTreeCompleter(): Completer =
    AggregateCompleter(this.map { it.makeJLineTreeCompleter(emptyList()) })

internal fun CliktCommand.makeJLineTreeCompleter(): Completer =
    AggregateCompleter(registeredSubcommands().map { it.makeJLineTreeCompleter(emptyList()) })

internal fun CliktCommand.makeJLineTreeCompleter(parentCommandPath: List<CliktCommand>): Completer =
    registeredSubcommands().let { subcommands ->
        val commandPath = parentCommandPath + listOf(this)
        if (subcommands.isEmpty()) {
            val argumentCompleter = ArgumentCompleter(
                commandPath.map { StringsCompleter(it.commandName) } + listOf(
                    Completers.OptionCompleter(
                        registeredArguments().map { it.makeJLineTreeCompleter() },
                        { _ -> registeredOptions().flatMap { it.makeJLineDescription(this.currentContext) } },
                        commandPath.size
                    )
                )
            )
            argumentCompleter
        } else {
            AggregateCompleter(subcommands.map { it.makeJLineTreeCompleter(commandPath) })
        }
    }

internal fun CompletionCandidates.makeJLineCompleter() = when (this) {
    is CompletionCandidates.Fixed -> StringsCompleter(candidates)
    is CompletionCandidates.Path -> Completers.FileNameCompleter()
    else -> NullCompleter.INSTANCE
}

internal fun Option.makeJLineDescription(context: Context): List<Completers.OptDesc> {
    val valueCompleter = completionCandidates.makeJLineCompleter()

    return names.map { Completers.OptDesc(it, it, optionHelp(context), valueCompleter) } +
            secondaryNames.map { Completers.OptDesc(it, it) }
}

internal fun Argument.makeJLineTreeCompleter(): Completer {
    return NullCompleter.INSTANCE
}

package com.cappielloantonio.tempo.di

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.readText
import kotlin.streams.asSequence

class ViewModelModuleConsistencyTest {
    private val projectRoot: Path = findProjectRoot(Paths.get(System.getProperty("user.dir")))
    private val viewModelDir: Path = projectRoot.resolve("app/src/main/java/com/cappielloantonio/tempo/viewmodel")
    private val moduleFile: Path = projectRoot.resolve("app/src/main/java/com/cappielloantonio/tempo/di/viewModelModule.kt")
    private val ignoredFiles = setOf("ViewModelActionExtensions")

    @Test
    fun allViewModelsAreRegisteredInKoinModule() {
        val declaredViewModels = viewModelSourceFiles()
            .map { it.nameWithoutExtension }
            .toSortedSet()

        val registeredViewModels = registeredViewModels().keys.toSortedSet()

        assertEquals(declaredViewModels, registeredViewModels)
    }

    @Test
    fun everyRegistrationMatchesViewModelConstructorArity() {
        val actualArities = viewModelSourceFiles().associate { file ->
            file.nameWithoutExtension to constructorDependencyCount(file.readText())
        }
        val registeredArities = registeredViewModels().mapValues { (_, args) ->
            registrationDependencyCount(args)
        }

        assertEquals(actualArities.toSortedMap(), registeredArities.toSortedMap())
    }

    private fun viewModelSourceFiles() = Files.list(viewModelDir).use { paths ->
        paths.asSequence()
            .filter { it.isRegularFile() }
            .filter { it.extension == "kt" || it.extension == "java" }
            .filter { it.nameWithoutExtension.endsWith("ViewModel") }
            .filterNot { it.nameWithoutExtension in ignoredFiles }
            .toList()
    }

    private fun registeredViewModels(): Map<String, String> {
        val regex = Regex("""viewModel\s*\{\s*([A-Za-z0-9_]+ViewModel)\((.*)\)\s*}""")
        return moduleFile.readText()
            .lineSequence()
            .mapNotNull { line -> regex.find(line.trim()) }
            .associate { match ->
                match.groupValues[1] to match.groupValues[2]
            }
    }

    private fun constructorDependencyCount(source: String): Int {
        val kotlinClassMatch = Regex("""class\s+\w+\s*\(""").find(source)
        if (kotlinClassMatch == null) {
            return 0
        }

        val classIndex = kotlinClassMatch.range.first
        if (classIndex == -1) return 0

        val openParenIndex = source.indexOf('(', startIndex = classIndex)
        val colonIndex = source.indexOf(':', startIndex = classIndex)

        if (openParenIndex == -1 || (colonIndex != -1 && openParenIndex > colonIndex)) {
            return 0
        }

        val constructorBlock = source.substring(openParenIndex + 1, findClosingParen(source, openParenIndex))
        return Regex("""\b(?:private|protected|internal|public)?\s*(?:val|var)\s+\w+\s*:""")
            .findAll(constructorBlock)
            .count()
    }

    private fun registrationDependencyCount(arguments: String): Int {
        if (arguments.isBlank()) return 0
        return Regex("""\bget\s*\(""").findAll(arguments).count()
    }

    private fun findClosingParen(source: String, openParenIndex: Int): Int {
        var depth = 0
        for (index in openParenIndex until source.length) {
            when (source[index]) {
                '(' -> depth++
                ')' -> {
                    depth--
                    if (depth == 0) {
                        return index
                    }
                }
            }
        }
        error("Could not find matching closing parenthesis in ViewModel declaration")
    }

    private fun findProjectRoot(start: Path): Path {
        generateSequence(start.toAbsolutePath()) { current -> current.parent }
            .forEach { candidate ->
                if (Files.exists(candidate.resolve("app/src/main/java/com/cappielloantonio/tempo/di/viewModelModule.kt"))) {
                    return candidate
                }
            }
        error("Could not locate project root from ${start.toAbsolutePath()}")
    }
}

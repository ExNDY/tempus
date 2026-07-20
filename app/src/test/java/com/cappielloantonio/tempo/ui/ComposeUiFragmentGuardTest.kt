package com.cappielloantonio.tempo.ui

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.name
import kotlin.io.path.readText
import org.junit.Assert.fail
import org.junit.Test

class ComposeUiFragmentGuardTest {

    @Test
    fun mainUiDoesNotUseFragmentApiSurface() {
        val forbidden = listOf(
            Regex("""androidx\.fragment\.app\."""),
            Regex("""DialogFragment"""),
            Regex("""BottomSheetDialogFragment"""),
            Regex("""PreferenceFragmentCompat"""),
            Regex("""NavHostFragment"""),
            Regex("""supportFragmentManager"""),
            Regex("""setFragmentResult"""),
            Regex("""\bclass\s+\w*Fragment\b"""),
            Regex("""\bFragment\("""),
        )
        val offenders = sourceFiles().flatMap { file ->
            val text = file.readText()
            forbidden.mapNotNull { pattern ->
                if (pattern.containsMatchIn(text)) {
                    "${root.relativize(file)} contains ${pattern.pattern}"
                } else {
                    null
                }
            }
        }

        if (offenders.isNotEmpty()) {
            fail(offenders.joinToString(separator = "\n"))
        }
    }

    @Test
    fun mainUiDoesNotUseLegacyFragmentResourceNames() {
        val forbidden = listOf(
            Regex("""fragment_[A-Za-z0-9_]*\.xml$"""),
            Regex("""android:id="@\+id/[A-Za-z0-9_]*Fragment""""),
        )
        val offenders = resourceFiles().flatMap { file ->
            forbidden.mapNotNull { pattern ->
                val matchesFileName = pattern.matches(file.name)
                val matchesContent = file.extension == "xml" && pattern.containsMatchIn(file.readText())
                if (matchesFileName || matchesContent) {
                    "${root.relativize(file)} matches ${pattern.pattern}"
                } else {
                    null
                }
            }
        }

        if (offenders.isNotEmpty()) {
            fail(offenders.joinToString(separator = "\n"))
        }
    }

    private fun sourceFiles(): List<Path> =
        listOf("app/src/main/java", "app/src/tempus/java", "app/src/degoogled/java")
            .flatMap { dir -> walk(root.resolve(dir), setOf("kt", "java")) }

    private fun resourceFiles(): List<Path> =
        walk(root.resolve("app/src/main/res"), setOf("xml"))

    private fun walk(dir: Path, extensions: Set<String>): List<Path> {
        if (!Files.exists(dir)) return emptyList()
        return Files.walk(dir).use { stream ->
            stream
                .filter { Files.isRegularFile(it) && it.extension in extensions }
                .toList()
        }
    }

    companion object {
        private val root: Path = Path.of("").toAbsolutePath().normalize()
    }
}

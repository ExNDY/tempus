package com.cappielloantonio.tempo.ui

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextCastRegressionTest {

    @Test
    fun composeContextAccessDoesNotUseRawActivityCasts() {
        val sourceRoot = listOf(
            File("src/main/java/com/cappielloantonio/tempo"),
            File("app/src/main/java/com/cappielloantonio/tempo"),
        ).first(File::exists)

        val forbiddenPatterns = listOf(
            "LocalContext.current as ",
            "view.context as Activity",
            "context as Activity",
        )

        val offenders = sourceRoot
            .walkTopDown()
            .filter { it.isFile && it.extension in setOf("kt", "java") }
            .flatMap { file ->
                file.readLines().mapIndexedNotNull { index, line ->
                    if (forbiddenPatterns.any(line::contains)) {
                        "${file.relativeTo(sourceRoot)}:${index + 1}: ${line.trim()}"
                    } else {
                        null
                    }
                }
            }
            .toList()

        assertTrue(
            "Replace raw Activity casts with Context.requireActivity<T>():\n${offenders.joinToString("\n")}",
            offenders.isEmpty(),
        )
    }
}

import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.kotlin.dsl.configure

plugins {
    id("com.google.devtools.ksp")
}

extensions.configure<KspExtension> {
    arg("room.schemaLocation", project.layout.projectDirectory.dir("schemas").asFile.absolutePath)
    arg("room.incremental", "true")
}

dependencies {
    add("implementation", libsCatalog.library("androidx-room-runtime"))
    add("ksp", libsCatalog.library("androidx-room-compiler"))
}

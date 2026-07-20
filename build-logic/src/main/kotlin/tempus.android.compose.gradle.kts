import com.android.build.api.dsl.ApplicationExtension
import org.gradle.kotlin.dsl.configure

plugins {
    id("org.jetbrains.kotlin.plugin.compose")
}

extensions.configure<ApplicationExtension> {
    buildFeatures.compose = true
}

dependencies {
    add("implementation", libsCatalog.library("androidx-compose-ui"))
    add("implementation", libsCatalog.library("androidx-compose-material3"))
    add("implementation", libsCatalog.library("androidx-compose-ui-tooling-preview"))
    add("debugImplementation", libsCatalog.library("androidx-compose-ui-tooling"))
    add("implementation", libsCatalog.library("androidx-compose-runtime-livedata"))
    add("implementation", libsCatalog.library("androidx-compose-material-icons-extended"))
}

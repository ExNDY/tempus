tasks.register<Delete>("clean") {
    group = "build"
    delete(layout.buildDirectory)
}

group = "xyz.emlyn"

patches {
    about {
        name = "TODO-emlyn-patches"
        description = "todo."
        source = "git@github.com:emlyn-m/patches.git"
        author = "@emlyn-m"
        contact = "na"
        website = "na"
        license = "GPLv3"
    }
}

// Separate configuration so gson is available at runtime for the
// generatePatchesList task but never bundled into the APK.
val patchListGeneratorClasspath = configurations.create("patchListGeneratorClasspath")

dependencies {
    implementation(libs.morphe.patches.library)
    compileOnly(libs.gson)
    patchListGeneratorClasspath(libs.gson)
}

// The morphe plugin adds `extensionConfiguration` directly as a resource srcDir.
// IDE sync (Gradle 9 + the Kotlin LSP) then resolves it without the project lock
// and fails, so route it through a Sync task and point resources at the output instead.
val syncExtensionResources = tasks.register<Sync>("syncExtensionResources") {
    from(configurations.named("extensionConfiguration"))
    into(layout.buildDirectory.dir("generated/extensionResources"))
}

sourceSets.main {
    resources.setSrcDirs(listOf("src/main/resources", syncExtensionResources))
}

tasks {
    register<JavaExec>("generatePatchesList") {
        description = "Build patch with patch list"

        dependsOn(build)

        classpath = sourceSets["main"].runtimeClasspath + patchListGeneratorClasspath
        mainClass.set("util.PatchListGeneratorKt")
    }

    // Used by gradle-semantic-release-plugin.
    publish {
        dependsOn("generatePatchesList")
    }
}

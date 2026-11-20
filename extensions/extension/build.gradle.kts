import org.gradle.plugins.ide.eclipse.model.Classpath
import org.gradle.plugins.ide.eclipse.model.Library
import org.gradle.plugins.ide.eclipse.model.SourceFolder

plugins {
    eclipse
}

extension {
    name = "extensions/extension.mpe"
}

android {
    namespace = "xyz.emlyn.patchextension"
    defaultConfig {
        minSdk = 24
    }
}

// fix for jdtls
eclipse.classpath {
    afterEvaluate { plusConfigurations.add(configurations["releaseCompileClasspath"]) }
    file.whenMerged {
        this as Classpath
        entries.add(SourceFolder("src/main/java", null))
        androidComponents.sdkComponents.bootClasspath.get().forEach {
            entries.add(Library(fileReference(it.asFile)))
        }
    }
}

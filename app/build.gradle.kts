import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.io.InputStream

plugins {
    kotlin("android")
    kotlin("kapt")
    id("com.android.application")
}

dependencies {
    compileOnly(project(":hideapi"))

    implementation(project(":core"))
    implementation(project(":service"))
    implementation(project(":design"))
    implementation(project(":common"))

    implementation(libs.kotlin.coroutine)
    implementation(libs.androidx.core)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.coordinator)
    implementation(libs.androidx.recyclerview)
    implementation(libs.google.material)
    // Библиотека quickie удалена для облегчения веса APK
    implementation(libs.androidx.activity.ktx)
}

tasks.getByName("clean", type = Delete::class) {
    delete(file("release"))
}

val geoFilesDownloadDir = "src/main/assets"

// Используем современный способ регистрации задачи
tasks.register("downloadGeoFiles") {
    // Список пуст, чтобы ничего не скачивалось. Типы указаны явно для компилятора.
    val geoFilesUrls = mapOf<String, String>()

    doLast {
        geoFilesUrls.forEach { (downloadUrl, outputFileName) ->
            val url = URL(downloadUrl)
            val outputPath = file("$geoFilesDownloadDir/$outputFileName")
            outputPath.parentFile.mkdirs()

            // Явно указываем тип InputStream, чтобы Gradle не путался
            url.openStream().use { input: InputStream ->
                Files.copy(input, outputPath.toPath(), StandardCopyOption.REPLACE_EXISTING)
                println("$outputFileName downloaded to $outputPath")
            }
        }
    }
}

afterEvaluate {
    // Безопасное обращение к задаче по имени
    val downloadGeoFilesTask = tasks.named("downloadGeoFiles")

    tasks.configureEach {
        if (name.startsWith("assemble")) {
            dependsOn(downloadGeoFilesTask)
        }
    }
}

tasks.getByName("clean", type = Delete::class) {
    delete(file(geoFilesDownloadDir))
}
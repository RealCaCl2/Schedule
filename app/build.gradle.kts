import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// 从 local.properties 读取签名配置
val keystorePropsFile = rootProject.file("local.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) {
        keystorePropsFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.cacl2.schedule"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.cacl2.schedule"
        minSdk = 30
        targetSdk = 36
        versionCode = 4
        versionName = "1.3.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file(keystoreProps.getProperty("keystore.path") ?: "schedule.keystore")
            storePassword = keystoreProps.getProperty("keystore.password") ?: ""
            keyAlias = keystoreProps.getProperty("keystore.alias") ?: "schedule"
            keyPassword = keystoreProps.getProperty("keystore.keyPassword") ?: ""
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material.icons.extended)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // ViewModel Compose
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // WebKit
    implementation(libs.androidx.webkit)

    // Jsoup
    implementation(libs.jsoup)

    // ZXing (QR code generation + scanning)
    implementation(libs.zxing.core)
    implementation(libs.zxing.embedded)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// ═══════════════════════════════════════════════════════════════
//  generateUpdateJson — 构建 APK 后自动生成 update.json
// ═══════════════════════════════════════════════════════════════
tasks.register("generateUpdateJson") {
    description = "Generate update.json from build config (auto-fills fileSize + SHA256)"
    group = "publishing"

    doLast {
        val versionCode = android.defaultConfig.versionCode
        val versionName = android.defaultConfig.versionName
        val apkName = "Schedule_v${versionName}.apk"

        // 根据实际部署地址修改 downloadUrl
        val downloadUrl = project.findProperty("updateDownloadUrl") as? String
            ?: "https://gitee.com/cacl2lhg/lschedule/raw/master/$apkName"

        // 自动从项目根目录 changelog.txt 读取更新日志
        val changelogFile = rootProject.file("changelog.txt")
        val changelog = if (changelogFile.exists()) {
            changelogFile.readText().trim()
        } else {
            project.findProperty("updateChangelog") as? String
                ?: "- 问题修复与体验优化"
        }

        val forceUpdate = project.findProperty("updateForce")?.toString()?.toBooleanStrictOrNull() ?: false

        // 自动检测 APK — 优先 release，其次 debug
        val apkCandidates = listOf(
            layout.buildDirectory.file("outputs/apk/release/app-release.apk"),
            layout.buildDirectory.file("outputs/apk/debug/app-debug.apk")
        )
        val apkFile = apkCandidates.mapNotNull { it.get().asFile }
            .firstOrNull { it.exists() }

        val fileSize = apkFile?.length() ?: 0L
        val sha256 = if (apkFile != null && fileSize > 0) {
            apkFile.sha256()
        } else {
            ""
        }

        val json = """
{
  "versionCode": $versionCode,
  "versionName": "$versionName",
  "downloadUrl": "$downloadUrl",
  "changelog": ${escapeJson(changelog)},
  "forceUpdate": $forceUpdate,
  "fileSize": $fileSize,
  "sha256": "$sha256"
}
        """.trimIndent()

        val outputDir = layout.buildDirectory.dir("release").get().asFile
        outputDir.mkdirs()
        val outputFile = File(outputDir, "update.json")
        outputFile.writeText(json)
        println("✅ update.json generated: ${outputFile.absolutePath}")
        println("   versionCode=$versionCode  versionName=$versionName")
        println("   downloadUrl=$downloadUrl")
        println("   fileSize=${formatFileSize(fileSize)}  sha256=$sha256")
    }
}

fun File.sha256(): String {
    return try {
        val isWindows = System.getProperty("os.name").lowercase().contains("win")
        val cmd = if (isWindows) {
            listOf("powershell", "-Command",
                "(Get-FileHash '${absolutePath}' -Algorithm SHA256).Hash.ToLower()")
        } else {
            listOf("sha256sum", absolutePath)
        }
        val process = ProcessBuilder(cmd).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText().trim()
        process.waitFor()
        if (isWindows) {
            output // PowerShell 直接输出纯哈希值（小写）
        } else {
            output.substringBefore(" ").trim().lowercase()
        }
    } catch (_: Exception) {
        ""
    }
}

fun formatFileSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
    else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
}

// 将多行文本转义为 JSON 字符串
fun escapeJson(text: String): String {
    val escaped = text
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\r\n", "\\n")
        .replace("\n", "\\n")
        .replace("\r", "\\n")
    return "\"$escaped\""
}

// 每次 assemble 后自动生成 update.json
tasks.matching { it.name.startsWith("assemble") }.configureEach {
    finalizedBy("generateUpdateJson")
}

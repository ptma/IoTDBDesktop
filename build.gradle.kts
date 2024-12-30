@file:Suppress("UNCHECKED_CAST")

import groovy.json.JsonSlurper
import groovy.lang.Closure
import io.github.fvarrui.javapackager.gradle.PackagePluginExtension
import io.github.fvarrui.javapackager.gradle.PackageTask
import io.github.fvarrui.javapackager.model.*
import io.github.fvarrui.javapackager.model.Platform
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.internal.os.OperatingSystem

plugins {
    `java-library`
}

buildscript {
    repositories {
        maven(url = "https://maven.aliyun.com/repository/public/")
        mavenLocal()
        mavenCentral()
        dependencies {
            // ********* package with gradle 7.6.2 *********
            // @see https://github.com/fvarrui/JavaPackager/issues/315
            // classpath("io.github.fvarrui:javapackager:1.6.7")
            classpath("io.github.fvarrui:javapackager:1.7.6")
        }
    }
}

plugins.apply("io.github.fvarrui.javapackager.plugin")

val versionConfig = "${rootProject.projectDir.path}/src/main/resources/version.json"
val versionJson = JsonSlurper().parse(File(versionConfig)) as Map<String, String>
val appliactionVersion = versionJson.get("version")
val applicationName: String = "IoTDBDesktop"
val organization: String = "ptma@163.com"
val copyrightVal: String = "Copyright (C) ptma@163.com"
val supportUrl: String = "https://github.com/ptma/iotdb-desktop"

val flatlafVersion = "3.4"
val fatJar = false

val requireModules = listOf(
    "java.base",
    "java.desktop",
    "java.prefs",
    "java.logging",
    "java.naming",
    "java.sql",
    "java.xml",
    "jdk.dynalink",
    "jdk.unsupported",
    "jdk.management"
)

if (JavaVersion.current() < JavaVersion.VERSION_17)
    throw RuntimeException("compile required Java ${JavaVersion.VERSION_17}, current Java ${JavaVersion.current()}")

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.9.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")

    compileOnly("org.projectlombok:lombok:1.18.20")
    annotationProcessor("org.projectlombok:lombok:1.18.20")
    testCompileOnly("org.projectlombok:lombok:1.18.20")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.20")

    implementation("com.formdev:flatlaf:${flatlafVersion}")
    implementation("com.formdev:flatlaf-swingx:${flatlafVersion}")
    implementation("com.formdev:flatlaf-extras:${flatlafVersion}")
    implementation("com.formdev:flatlaf-intellij-themes:${flatlafVersion}")
    implementation("com.formdev:flatlaf-fonts-jetbrains-mono:2.242")
    implementation("at.swimmesberger:swingx-core:1.6.8")

    implementation("com.jgoodies:jgoodies-forms:1.9.0")
    implementation("com.intellij:forms_rt:7.0.3") {
        exclude(group = "asm", module = "asm-commons")
    }
    implementation("com.miglayout:miglayout-swing:11.3")

    implementation("com.fifesoft:rsyntaxtextarea:3.5.3")
    implementation("com.fifesoft:autocomplete:3.3.1")
    implementation(files("libs/swing-toast-notifications-1.0.1.jar"))

    implementation("cn.hutool:hutool-core:5.8.24")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("org.apache.iotdb:iotdb-session:1.3.3")
//    implementation("org.apache.iotdb:node-commons:1.3.3")
    implementation("org.apache.commons:commons-csv:1.12.0")

    implementation("org.slf4j:slf4j-api:2.0.7")
    implementation("org.slf4j:jul-to-slf4j:2.0.9")
    implementation("ch.qos.logback:logback-classic:1.4.8")
}

repositories {
    maven(url = "https://maven.aliyun.com/repository/public/")
    mavenLocal()
    mavenCentral()
}

tasks.test {
    useJUnitPlatform()
    testLogging.exceptionFormat = TestExceptionFormat.FULL
}

tasks.compileJava {
    sourceCompatibility = "17"
    targetCompatibility = "17"
    options.encoding = "UTF-8"
    options.isDeprecation = false
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes("Main-Class" to "org.apache.iotdb.desktop.IotdbDesktopApp")
        attributes("Implementation-Vendor" to "https://github.com/ptma/iotdb-desktop")
        attributes("Implementation-Copyright" to copyrightVal)
        attributes("Implementation-Version" to appliactionVersion)
        attributes("Multi-Release" to "true")
    }

    exclude("module-info.class")
    exclude("META-INF/versions/*/module-info.class")
    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.LIST")
    exclude("META-INF/*.factories")

    if (fatJar) {
        from({
            configurations.runtimeClasspath.get()
                .filter { it.name.endsWith("jar") }
                .map {
                    zipTree(it).matching {
                        exclude("META-INF/LICENSE")
                    }
                }
        })
    }

    from("${rootDir}/LICENSE") {
        into("META-INF")
    }
}

configure<PackagePluginExtension> {
    mainClass("org.apache.iotdb.desktop.IotdbDesktopApp")
    packagingJdk(File(System.getProperty("java.home")))
    bundleJre(true)
    customizedJre(true)
    modules(requireModules)
    jreDirectoryName("jre")
}

var taskPlatform = Platform.windows
var taskPlatform_M1 = false

tasks.register<PackageTask>("packageForWindows") {
    taskPlatform = Platform.windows

    val innoSetupLanguageMap = LinkedHashMap<String, String>()
    innoSetupLanguageMap["Chinese"] = "compiler:Languages\\ChineseSimplified.isl"
    innoSetupLanguageMap["English"] = "compiler:Default.isl"

    description = "package For Windows"

    organizationName = organization
    organizationUrl = supportUrl
    version = appliactionVersion;

    platform = Platform.windows
    isCreateZipball = false
    winConfig(closureOf<WindowsConfig> {
        icoFile = getIconFile("IotdbDesktop.ico")
        headerType = HeaderType.gui
        originalFilename = applicationName
        copyright = copyrightVal
        productName = applicationName
        productVersion = version
        fileVersion = version
        isGenerateSetup = false
        setupLanguages = innoSetupLanguageMap
        isCreateZipball = true
        isGenerateMsi = false
        isGenerateMsm = false
        msiUpgradeCode = version
        isDisableDirPage = false
        isDisableFinishedPage = false
        isDisableWelcomePage = false
    } as Closure<WindowsConfig>)
    dependsOn(tasks.build)
}

tasks.register<PackageTask>("packageForLinux") {
    taskPlatform = Platform.linux

    description = "package For Linux"
    platform = Platform.linux

    organizationName = organization
    organizationUrl = supportUrl
    version = appliactionVersion;

    linuxConfig(
        closureOf<LinuxConfig> {
            pngFile = getIconFile("IotdbDesktop.png")
            isGenerateDeb = true
            isGenerateRpm = true
            isCreateTarball = true
            isGenerateInstaller = true
            categories = listOf("Office")
        } as Closure<LinuxConfig>
    )
    dependsOn(tasks.build)
}

tasks.register<PackageTask>("packageForMac_M1") {
    taskPlatform = Platform.mac
    taskPlatform_M1 = true

    description = "package For Mac M1"
    platform = Platform.mac

    organizationName = organization
    organizationUrl = supportUrl
    version = appliactionVersion;

    macConfig(
        closureOf<MacConfig> {
            icnsFile = getIconFile("IotdbDesktop.icns")
            isGenerateDmg = true
            macStartup = MacStartup.ARM64
        } as Closure<MacConfig>
    )
    dependsOn(tasks.build)
}

tasks.register<PackageTask>("packageForMac") {
    taskPlatform = Platform.mac
    taskPlatform_M1 = true

    description = "package For Mac"
    platform = Platform.mac

    organizationName = organization
    organizationUrl = supportUrl
    version = appliactionVersion;

    macConfig(
        closureOf<MacConfig> {
            icnsFile = getIconFile("IotdbDesktop.icns")
            isGenerateDmg = true
            macStartup = MacStartup.X86_64
        } as Closure<MacConfig>
    )
    dependsOn(tasks.build)
}

fun getIconFile(fileName: String): File {
    return File(projectDir.absolutePath + File.separator + "assets" + File.separator + fileName)
}


import com.android.build.api.variant.ApplicationVariant

plugins {
    id("com.android.application")
    kotlin("plugin.serialization") version "2.4.0"
    kotlin("plugin.compose") version "2.4.0"
}

android {
    compileSdk = 37

    defaultConfig {
        applicationId = "helium314.keyboard"
        minSdk = 21
        targetSdk = 37
        versionCode = 4301
        versionName = "4.3-beta2"
                ndk {
            abiFilters.clear()
            abiFilters.addAll(listOf("arm64-v8a"))
        }
        androidResources {
            localeFilters += listOf("es", "en")
            noCompress += listOf("mdl", "fst", "ark", "int", "conf", "stats", "dubm", "ie", "mat")
        }
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }


        buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            isJniDebuggable = false
        }

        create("nouserlib") {
            isMinifyEnabled = true
            isShrinkResources = false
            isDebuggable = false
            isJniDebuggable = false
        }
        debug {
            isMinifyEnabled = false
            isJniDebuggable = false
            applicationIdSuffix = ".debug"
        }
        create("runTests") {
            isMinifyEnabled = false
            isJniDebuggable = false
        }
        create("debugNoMinify") {
            isDebuggable = true
            isMinifyEnabled = false
            isJniDebuggable = false
            signingConfig = signingConfigs.getByName("debug")
            applicationIdSuffix = ".debug"
        }

                androidComponents.onVariants { variant: ApplicationVariant ->
            val unusedDicts = listOf(
                "main_ar.dict", "main_be.dict", "main_bg.dict", "main_ca.dict", "main_cs.dict",
                "main_da.dict", "main_de.dict", "main_el.dict", "main_en.dict", "main_eo.dict",
                "main_et.dict", "main_eu.dict", "main_fa.dict", "main_fi.dict", "main_fr.dict",
                "main_gl.dict", "main_he.dict", "main_hi.dict", "main_hr.dict", "main_hu.dict",
                "main_hy.dict", "main_id.dict", "main_it.dict", "main_ka.dict", "main_kk.dict",
                "main_ko.dict", "main_ky.dict", "main_lt.dict", "main_lv.dict", "main_mk.dict",
                "main_ml.dict", "main_mn.dict", "main_mr.dict", "main_nb.dict", "main_nl.dict",
                "main_pl.dict", "main_pt.dict", "main_ro.dict", "main_ru.dict", "main_sk.dict",
                "main_sl.dict", "main_sr.dict", "main_sv.dict", "main_ta.dict", "main_te.dict",
                "main_th.dict", "main_tr.dict", "main_uk.dict", "main_uz.dict", "main_vi.dict"
            )
            variant.androidResources.ignoreAssetsPatterns = unusedDicts
            if (variant.buildType == "debug") {
                variant.proguardFiles = emptyList()

                variant.proguardFiles.add(project.layout.buildDirectory.file(project.buildFile.parent + "/dontoptimize.pro"))
                variant.proguardFiles.add(project.layout.buildDirectory.file(project.buildFile.parent + "/proguard-rules.pro"))
            }
            variant.outputs.forEach { output ->
                if (output is com.android.build.api.variant.impl.VariantOutputImpl) {
                    output.outputFileName = "HeliBoard_${defaultConfig.versionName}-${variant.buildType}.apk"
                }
            }
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose = true
    }

    externalNativeBuild {
        ndkBuild {
            path = File("src/main/jni/Android.mk")
        }
    }
    ndkVersion = "28.0.13004108"

        packaging {
        jniLibs {
            useLegacyPackaging = false
        }
        resources {
            excludes += listOf(
                "META-INF/LICENSE*",
                "META-INF/NOTICE*",
                "META-INF/*.kotlin_module",
                "kotlin-tooling-metadata.json"
            )
        }
    }


    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    namespace = "helium314.keyboard.latin"
    lint {
        abortOnError = true
    }
}

dependencies {
    // androidx
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.autofill:autofill:1.3.0")
    implementation("androidx.viewpager2:viewpager2:1.1.0")

    // kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    // compose
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    implementation(platform("androidx.compose:compose-bom:2025.11.01"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    "debugNoMinifyImplementation"("androidx.compose.ui:ui-tooling")
    implementation("androidx.navigation:navigation-compose:2.9.8")
    implementation("sh.calvin.reorderable:reorderable:3.1.0")
    implementation("com.github.skydoves:colorpicker-compose:1.1.3")

    // Vosk offline speech recognition
    implementation("net.java.dev.jna:jna:5.13.0@aar")
    implementation("com.alphacephei:vosk-android:0.3.47@aar")

    // test
    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
    testImplementation("org.mockito:mockito-core:5.23.0")
    testImplementation("org.robolectric:robolectric:4.16.1")
    testImplementation("androidx.test:runner:1.7.0")
    testImplementation("androidx.test:core:1.7.0")
}

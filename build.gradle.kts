plugins {
    alias(uuBuild.plugins.android.application) apply false
    alias(uuBuild.plugins.android.library) apply false
    alias(uuBuild.plugins.kotlin.android) apply false
    alias(uuBuild.plugins.nexus.publish)
    alias(uuBuild.plugins.kotlin.serialization)
    alias(uuBuild.plugins.uu.library) apply false
    alias(uuBuild.plugins.uu.library.app) apply false
    alias(uuBuild.plugins.uu.android.test) apply false
    alias(uuBuild.plugins.uu.publish)
    // AGP 9 removed `kotlin-kapt`. `com.android.legacy-kapt` ships with AGP
    // itself and is the drop-in replacement for modules whose annotation
    // processors (here: data binding for @BindingAdapter / `*BindingImpl`)
    // haven't migrated to KSP. Version pinned to the AGP version in the
    // uuBuild catalog.
    id("com.android.legacy-kapt") version uuBuild.versions.agp.get() apply false
}

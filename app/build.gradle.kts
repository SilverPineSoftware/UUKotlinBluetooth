plugins {
    alias(uuBuild.plugins.uu.library.app)
    // Required by data binding (generates BR / Binding classes). AGP 9 removed
    // support for `kotlin-kapt`; `com.android.legacy-kapt` is the drop-in
    // replacement for projects whose annotation processors haven't moved to KSP.
    id("com.android.legacy-kapt")
}

android {
    buildFeatures {
        dataBinding = true
        viewBinding = true
    }
}

dependencies {
    implementation(uuBuild.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.google.material)
    implementation(libs.uu.core.ktx)
    implementation(libs.uu.ux.ktx)
    implementation(project(":library"))

    androidTestImplementation(uuBuild.androidx.junit)
    androidTestImplementation(uuBuild.androidx.espresso.core)
}

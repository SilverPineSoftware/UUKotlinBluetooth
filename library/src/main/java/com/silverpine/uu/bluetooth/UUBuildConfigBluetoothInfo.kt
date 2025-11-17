package com.silverpine.uu.bluetooth

class UUBuildConfigBluetoothInfo : UUBluetoothInfo
{
    override val buildVersion: String
        get() = BuildConfig.BUILD_VERSION

    override val buildBranch: String
        get() = BuildConfig.BUILD_BRANCH

    override val buildCommitHash: String
        get() = BuildConfig.BUILD_COMMIT_HASH

    override val buildDate: String
        get() = BuildConfig.BUILD_DATE
}
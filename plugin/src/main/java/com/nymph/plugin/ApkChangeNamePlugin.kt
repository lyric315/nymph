package com.nymph.plugin

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.api.ApplicationVariant
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter


/**
 * Created by lirui on 2020-02-29.
 */
class ApkChangeNamePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        //容错处理，判断当前Project是否apply了 com.android.application 插件
        var pluginContainer = project.plugins
        if (pluginContainer.findPlugin(AppPlugin::class.java) == null) {
            throw IllegalStateException("Must apply \'com.android.application\' in your build.gradle!")
        }

        var androidExtension = project.extensions.getByType(AppExtension::class.java)
        androidExtension.applicationVariants.all { variant ->
            variant.outputs.forEach { output ->
                var outputFile = output.outputFile
                // 遍历apk文件，并修改apk名称
                if (outputFile != null
                    && outputFile.exists()
                    && outputFile.name.endsWith(".apk")
                    && variant.buildType.name.equals("debug")
                ) {
                    var apkName = generateApkName(variant)
                    var targetFile = File(outputFile.parent, apkName)
                    outputFile.renameTo(targetFile)
                }
            }
        }
    }

    /**
     * 生成apkname
     */
    private fun generateApkName(variant: ApplicationVariant): String {
        var date = LocalDate.now()
        var flavorName = variant.buildType.name
        return "nymph_" + flavorName + date.format(DateTimeFormatter.BASIC_ISO_DATE) + ".apk"
    }
}
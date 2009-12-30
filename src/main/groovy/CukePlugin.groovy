package com.smokejumperit.gradle

import org.gradle.api.*
import org.gradle.api.plugins.*

class CukePlugin extends SjitPlugin {
  void use(Project project, ProjectPluginsContainer projectPluginsHandler) { 
    def pluginLogger = this.logger
    project.usePlugin(RunJRubyPlugin)
    project.convention.plugins.cuke = new CukePluginConvention()
    project.convention.plugins.cuke.featuresDir = "src/test/features"
    project.convention.plugins.cuke.configs = ['cuke']
    project.configurations { 
      cuke {
        extendsFrom testRuntime, runtime, jruby
        ['test', 'main'].each { project.sourceSets."$it".classes }
      }
    }
    project.task('runFeatures') << { 
      project.useGem('cucumber')

      def cukeBin = project.gemScript('cucumber')
      pluginLogger.debug("Cucumber binary: $cukeBin")

      def featuresDir = project.tryRelativePath(project.convention.plugins.cuke.featuresDir)
      pluginLogger.debug("Features directory: $featuresDir")

      def configs = project.convention.plugins.cuke.configs.collect { "$it" } as String[]
      pluginLogger.debug("Configurations: $configs")

      project.runJRuby("$cukeBin $featuresDir", configs)
    }
    project.runFeatures.dependsOn project.classes, project.testClasses
  }
}

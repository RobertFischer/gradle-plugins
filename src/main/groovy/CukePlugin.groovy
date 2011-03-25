package com.smokejumperit.gradle

import org.gradle.api.*
import org.gradle.api.plugins.*

class CukePlugin extends SjitPlugin {

  void apply(Project project) {
    def pluginLogger = this.logger

    project.apply(plugin:RunJRubyPlugin)
    project.apply(plugin:ProjectExtPlugin)

    project.convention.plugins.cuke = new CukePluginConvention()
    project.convention.plugins.cuke.featuresDir = "${project.projectDir}/src/test/features"
    project.convention.plugins.cuke.stepsOutputDir = "${project.buildDir}/classes/test"
    project.convention.plugins.cuke.configs = ["cuke"]

    project.configurations { 
      cuke {
        extendsFrom(testRuntime, runtime, jruby)
      }
    }

    def cukeConfigs = {->
      return project.convention.plugins.cuke.configs.collect { "$it" } as String[]
    }

    project.task('cukeGems') {
			description = "Installs the Cucumber gems"
			doFirst {
				project.useGem("gherkin")
				project.useGem("cucumber")
				project.useGem("rspec")
			}
    }

    project.task('runFeatures', dependsOn:[project.tasks.cukeGems]) << { 
      description = "Runs the Cucumber features"

      def featuresDir = project.tryRelativePath(project.convention.plugins.cuke.featuresDir)
      def stepsOutputDir = project.tryRelativePath(project.convention.plugins.cuke.stepsOutputDir)
      pluginLogger.debug("Features directory: $featuresDir")

      def configs = cukeConfigs()
      pluginLogger.debug("Configurations: ${configs.toString()}")

      def outputDir = project.tryRelativePath(new File(project.buildDir, "cuke-output"))
      project.ant.mkdir(dir:outputDir)
      // --require target/test-classes -- Do we really need that?
      project.runJRuby("--color --format pretty --format junit --out $outputDir --require $stepsOutputDir $featuresDir", configs)
    }

		def ptasks = project.tasks
    ptasks.runFeatures.dependsOn ptasks.classes, ptasks.testClasses, ptasks.cukeGems

  }
}

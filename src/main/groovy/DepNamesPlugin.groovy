package com.smokejumperit.gradle

import org.gradle.api.*
import org.gradle.api.plugins.*
import org.gradle.api.artifacts.dsl.DependencyHandler

/**
* Adds dependency keywords to the project based on <code>~/.gradle/dependencies.properties</code>.
*/
class DepNamesPlugin extends SjitPlugin {
	def propFile = new File(
		new File(
			System.getProperty("user.home", "~"), 
			".gradle"
		), "dependencies.properties"
	).absoluteFile

  void apply(Project project) {
		def props = new Properties()

		if(propFile.exists()) {
			propFile.withReader { reader ->
				props.load(reader)
			}
		}

		def projectPropFile = new File(project.rootDir, 
			"dependencies.properties"
		).absoluteFile
		if(projectPropFile.exists()) {
			projectPropFile.withReader { reader ->
				props.load(reader)
			}
		}

		props.each { k, v ->
			DependencyHandler.metaClass."$k" = v
		}
  }
}

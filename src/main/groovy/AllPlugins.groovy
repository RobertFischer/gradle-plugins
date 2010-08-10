package com.smokejumperit.gradle

import org.gradle.api.*
import org.gradle.api.plugins.*

class AllPlugins extends SjitPlugin {

  def plugins

  AllPlugins() {
    def classLoader = this.class.classLoader

    logger.trace("Going to find the SJIT plugins list")
    def file = classLoader.getResourceAsStream('com/smokejumperit/gradle/sjit.plugins')
    if(!file) throw new Exception("Cannot find SmokejumperIT Plugins file")
    plugins = file.text.split()
    file.close()
  }

  void apply(Project project) {
    logger.debug("Going to load these plugins: $plugins")

    def classLoader = this.class.classLoader

    plugins.each {
      def name = "com.smokejumperit.gradle.$it"
      def cls = Class.forName(name, true, classLoader)
      if(!cls) throw new Exception("Could not find class $cls")

      logger.info("${this.class} implies using plugin $name for $project")
      project.apply(plugin:cls)
      logger.debug("Successfully used plugin $name for $project")
    }
    
  }
}

package com.smokejumperit.gradle

import org.gradle.api.*
import org.gradle.api.plugins.*

class AllPlugins extends SjitPlugin {

  def plugins

  AllPlugins() {
    def classLoader = this.class.classLoader

    def file = classLoader.getResourceAsStream('com/smokejumperit/gradle/sjit.plugins')
    if(!file) throw new Exception("Cannot find SmokejumperIT Plugins file")
    plugins = file.text.split()
    file.close()
  }

  void use(Project project, ProjectPluginsContainer projectPluginsHandler) { 
    def classLoader = this.class.classLoader

    plugins.each {
      def name = "com.smokejumperit.gradle.$it"
      def cls = Class.forName(name, true, classLoader)
      if(!cls) throw new Exception("Could not find class $cls")

      project.logger.info("Delegating to using plugin $name for $project")
      project.usePlugin(cls)
      project.logger.debug("Successfully used plugin $name for $project")
    }
    
  }
}

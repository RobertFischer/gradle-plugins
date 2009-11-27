package com.smokejumperit.gradle

import org.gradle.api.*
import org.gradle.api.plugins.*

class RunJRubyPlugin extends SjitPlugin {
  void use(Project project, ProjectPluginsContainer projectPluginsHandler) { 
    project.usePlugin(ExecPlugin)
    
  }
}

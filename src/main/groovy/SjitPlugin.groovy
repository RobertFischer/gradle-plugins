package com.smokejumperit.gradle

import org.gradle.api.*
import org.gradle.api.plugins.*
import org.gradle.api.logging.*

abstract class SjitPlugin implements Plugin<Project> {

  def logger = Logging.getLogger(this.class) 
  def getLog() { logger }

  def propertyDefaults(Map props, Project project) {
    props.each { k,v -> propertyDefault(project, k, v) }
  }

  def propertyDefault(Project project, String propName, defaultValue) {
    if(!project.hasProperty(propName)) project."$propName" = defaultValue
  }

}

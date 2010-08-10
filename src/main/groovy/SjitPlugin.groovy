package com.smokejumperit.gradle

import org.gradle.api.*
import org.gradle.api.plugins.*
import org.gradle.api.logging.*

abstract class SjitPlugin implements Plugin<Project> {

  Logger getLogger() { Logging.getLogger(this.class) }

}

package com.smokejumperit.gradle

import org.gradle.api.*
import org.gradle.api.plugins.*
import org.gradle.api.logging.*
import static com.google.common.collect.Sets.*
import static java.util.Collections.*

abstract class SjitPlugin implements Plugin {

  final Logger logger = Logging.getLogger(this.class)
  
  abstract void use(Project project, ProjectPluginsContainer projectPluginsHandler);

  void inProject(project, Closure impl) {
    impl.delegate = project
    impl()
  }

}

package com.smokejumperit.gradle

import org.gradle.api.*
import org.gradle.api.plugins.*
import static com.google.common.collect.Sets.*
import static java.util.Collections.*

abstract class SjitPlugin implements Plugin {
  
  abstract void use(Project project, ProjectPluginsContainer projectPluginsHandler);

}

package com.smokejumperit.gradle

import org.gradle.api.*
import org.gradle.api.plugins.*

/**
* Various and sundry extensions to the Project API.
*/
class ProjectExtPlugin extends SjitPlugin {
  void use(Project project, ProjectPluginsContainer projectPluginsHandler) { 
    project.metaClass.tryRelativePath = { path ->
      try {
        path = project.relativePath(path)
      } catch(gradle.api.GradleException e) {
        logger.trace("Could not relativize $path", e)
      } catch(NullPointerException npe) {
        logger.trace("Attempted to relativize $path, but got NPE: returning null")
      }
      return path
    }
  }
}

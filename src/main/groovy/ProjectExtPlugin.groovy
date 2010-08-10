package com.smokejumperit.gradle

import org.gradle.api.*
import org.gradle.api.plugins.*

/**
* Various and sundry extensions to the Project API.
*/
class ProjectExtPlugin extends SjitPlugin {
  void apply(Project project) {
    project.metaClass.tryRelativePath = { path ->
      try {
        path = project.relativePath(path)
      } catch(GradleException e) {
        logger.trace("Could not relativize $path", e)
      } catch(NullPointerException e) {
        logger.trace("Attempted to relativize $path, but got NPE: returning $path")
      }
      return path
    }
  }
}

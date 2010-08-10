package com.smokejumperit.gradle

import org.gradle.api.*
import org.gradle.api.plugins.*
import com.google.common.base.*
import com.google.common.collect.MapMaker

// TODO Find out if there is a ClassLoader that can delegate to multiple other classloaders
class ClassLoadersPlugin extends SjitPlugin {

  void apply(Project project) {

    def configUrls = new MapMaker().concurrencyLevel(2).makeComputingMap(
      [apply: { String config ->
        logger.trace("Collecting classpath URLs for configuration '$config'")
        return project.configurations."$config".collect {
          it.toURI().toURL()
        }
      }] as Function
    )

    project.metaClass.classPathElementsFor = { String[] configs ->
      return configs.collect {
        configUrls[it]
      }.flatten()*.toExternalForm().collect {
        it.replaceFirst(/^file:/, "")
      }
    }

    project.metaClass.classPathFor = { String[] configs ->
      return project.classPathElementsFor(configs).join(System.properties['path.separator'])
    }

    project.metaClass.classLoaderFor = { String[] configs ->
      return new URLClassLoader(configs.collect { 
        configUrls[it]
      }.flatten() as URL[]) 
    }

    project.metaClass.classFor = { String className, String[] configNames ->
      Class.forName(className, true, project.classLoaderFor(configNames))
    }

  }

}

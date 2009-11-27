package com.smokejumperit.gradle

import org.gradle.api.*
import org.gradle.api.plugins.*
import java.util.concurrent.ConcurrentHashMap

class ClassLoadersPlugin extends SjitPlugin {

  void use(Project project, ProjectPluginsContainer projectPluginsHandler) { 
    def classLoaders = new ConcurrentHashMap(8, 0.9f, 2)
    project.metaClass.classLoaderFor = { String config ->
      if(classLoaders.isEmpty() || !classLoaders.containsKey(config)) {
        classLoaders.putIfAbsent(config,
          new URLClassLoader(delegate.configurations."$config".collect { 
            it.toURI().toURL() 
          } as URL[])
        )
      }
      classLoaders.get(config)
    }

    project.metaClass.classFor = { String configName, String className ->
      delegate.classLoaderFor(configName).loadClass(className, true)
    }
  }
}

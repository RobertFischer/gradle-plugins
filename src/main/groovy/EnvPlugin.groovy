package com.smokejumperit.gradle

import org.gradle.api.*
import org.gradle.api.plugins.*

class EnvPlugin extends SjitPlugin {
  void use(Project project, ProjectPluginsContainer projectPluginsHandler) { 
    project.ant.property(environment:'env')
    def envs = [:] 
    project.ant.properties.each { k,v ->
      if(k.startsWith('env.')) {
        envs.put("$k".replaceFirst(/^env\./, ""), v)
      }
    }
    logger.info("Environment keys: ${(envs.keySet() as List).sort()}")
    project.metaClass.env = Collections.unmodifiableMap(envs)
    project.metaClass.env = { k -> 
      k = k?.toString()
      if(!envs.containsKey(k)) {
        throw new Exception("Could not find system environment variable: $k (Use `env['$k']` instead of `env('$k')` to get null on not found)")
      }
      envs[k] 
    }
  }
}

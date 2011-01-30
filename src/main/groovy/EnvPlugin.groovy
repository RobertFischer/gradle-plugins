package com.smokejumperit.gradle

import org.gradle.api.*
import org.gradle.api.plugins.*
import java.util.concurrent.*
import java.util.concurrent.atomic.*

class EnvPlugin extends SjitPlugin {

  private final envCache = Collections.unmodifiableMap(System.getenv())

  void apply(Project project) {
    def envs = envCache
    project.metaClass.env = envs
    project.metaClass.env = { k -> 
      k = "${k}".toString()
      if(!envs.containsKey(k)) {
        throw new RuntimeException(
					"Could not find system environment variable: $k (Use `env['$k']` instead of `env('$k')` to get null on not found)"
				)
      }
      envs[k] 
    }
  }

}

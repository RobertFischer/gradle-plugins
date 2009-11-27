package com.smokejumperit.gradle

import org.gradle.api.*
import org.gradle.api.plugins.*

class RunJRubyPlugin extends SjitPlugin {
  void use(Project project, ProjectPluginsContainer projectPluginsHandler) { 
    def pluginLogger = this.logger
    inProject(project) {
      usePlugin(ClassLoadersPlugin)
      configurations { runJRuby }
      dependencies { 
        runJRuby "org.jruby.embed:jruby-embed:0.1.2" // TODO Update to a more recent JRuby
      }
      metaClass.runJRuby = { String cmdArg, String[] configs=([runJRuby] as String[]) ->
        def JRuby = classFor(runJRuby, configs)
        def curThread = Thread.currentThread()
        curThread.setContextClassLoader(JRuby.class.classLoader)
        pluginLogger.info("Running JRuby: $cmdArg")
        JRuby.main("-S $cmdArg".split())
        pluginLogger.debug("Done running JRuby: $cmdArg")
        curThread.setContextClassLoader(ClassLoader.systemClassLoader) // Allow JRuby to be GC'ed
      }
    }
  }
}

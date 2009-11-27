# Gradle Plugins

These are a variety of plugins that I wrote for [Gradle](http://gradle.org) and figured I'd share with the world.

## ClassLoaders

### Description

Provides two methods on the `project` object to work with configuration classloaders:

* `classLoaderFor(configName)`&mdash;Provides a `java.lang.ClassLoader` consisting of all of the classes for a configuration.

* `classFor(configName, className)`&mdash;Looks up the class for name `className` using the class loader for config `configName`.

Note that each call to one of these methods generates a new ClassLoader instance: this is a feature, not a bug, because it allows the ClassLoader to be garbage collected if it and its classes are done being consumed.  This can be critical to saving PermGen space.

### Example

    task(foo) << {
      project.classFor(bar, "my.app.Main").main()
    }

## ExecPlugin

### Description

Provides methods `exec(cmd)` and `exec(cmd, baseDir)` on the project to execute shell commands.  If the command does not return 0, the build will fail.

### Example

    project.exec("ls -al", project.buildDir)

## EnvPlugin

### Description

Provides a map property on the project named `env` consisting of the external system's environment variables.  Also provides a method `env(key)` on the project that will return a particular environment variable's value, and explode if it does not exist.

# Installation

Add the following lines to your build script to add the jars to your buildscript classpath and use the plugins:

    // Example of using two plugins
    usePlugin(com.smokejumperit.gradle.ClassLoadersPlugin)
    usePlugin(com.smokejumperit.gradle.ExecPlugin)

    buildscript {
      repositories {
        mavenRepo urls:'http://repo.smokejumperit.com'
      }
      dependencies {
        classpath 'com.smokejumperit:gradle-plugins:0.2'
      }
    }

If you want to ust use all the SmokejumperIT plugins, you can do this:

    usePlugin(com.smokejumperit.gradle.AllPlugins)

    buildscript {
      repositories {
        mavenRepo urls:'http://repo.smokejumperit.com'
      }
      dependencies {
        classpath 'com.smokejumperit:gradle-plugins:0.2'
      }
    }

See the `sample` directory of this project for a build script which does this.

# Author and Origin

These plugins were written by [Robert Fischer](http://smokejumperit.com/).  They are published at [GitHub:RobertFischer/gradle-plugins](http://github.com/RobertFischer/gradle-plugins).

# License

All these plugins are licensed under the [Creative Commons — CC0 1.0 Universal](http://creativecommons.org/publicdomain/zero/1.0/) license with no warranty (expressed or implied) for any purpose.

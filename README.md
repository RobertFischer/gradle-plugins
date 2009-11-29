# Gradle Plugins

These are a variety of plugins that I wrote for [Gradle](http://gradle.org) and figured I'd share with the world.

## ClassLoaders

### Description

Provides two methods on the `project` object to work with configuration classloaders:

* `classLoaderFor(String... configNames)`&mdash;Provides a `java.lang.ClassLoader` consisting of all of the dependencies in the named configurations, in the order specified.

* `classFor(String className, String... configNames)`&mdash;Looks up the class for name `className` using the class loader for configs in `configNames`.

Note that each call to one of these methods generates a new ClassLoader instance: this is a feature, not a bug, because it allows the ClassLoader to be garbage collected if it and its classes are done being consumed.  This can be critical to saving PermGen space.

### Example

    task(foo) << {
      project.classFor("my.app.Main", bar).main()
    }

## ExecPlugin

### Description

Provides methods `exec(cmd)` and `exec(cmd, baseDir)` on the project to execute shell commands.  If the command does not return 0, the build will fail.

### Example

    project.exec("ls -al", project.buildDir)

## EnvPlugin

### Description

Provides a map property on the project named `env` consisting of the external system's environment variables.  Also provides a method `env(key)` on the project that will return a particular environment variable's value, and explode if it does not exist.

## ProjectExtPlugin

### Description

This is a holder for various extensions to the Project API.  

### Example

    // Attempt to make path relative to project if it is a subdir
    project.tryRelativePath("/some/random/path/foo/bar/baz")


## RunJRubyPlugin

### Description

A plugin to run JRuby scripts and manage gems.

### API

    project.gemHome // Provides the location for where gems are installed
    project.gemHome(String gemName) // Provides the root folder of the gem
    project.gemScript(String gemName) // Provides the main script for the gem
    project.runJRuby(String cmdArg, String[] configs=['runJRuby'])
    project.useGem(String gemName) // Installs a gem (if not currently installed)


## CukePlugin

### Description

Runs [Cucumber|http://cukes.info].  Specifically, provides a task (`runFeatures`) that executes the features, which are assumed to reside in `./src/test/features/`.  By default, the `runJRuby` and `runtime` configurations are loaded when the features are executed.

To modify the defaults, change `convention.plugin.cuke`.  The `configs` property of that object is a list of configurations to load when executing feature tests.  The `featuresDir` property of that object denotes where the root of the features reside.

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

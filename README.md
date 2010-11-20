# Gradle Plugins

These are a variety of plugins that I wrote for [Gradle](http://gradle.org) and figured I'd share with the world.

All of the plugins below have the class name `com.smokejumperit.gradle.NameOfPlugin`.  So `AllPlugins` is `com.smokejumperit.gradle.AllPlugins`, and `EnvPlugin` is `com.smokejumperit.gradle.EnvPlugin`.  And so on.

## AllPlugins

### Description

Convenience plugin that loads all the plugins listed below.

## DepNames Plugin

### Description

Provides the ability to keep external dependency names and versions in one location, and use them throughout
a number of projects.  By assigning a Java identifier as a key and a configuration spec as a value in a 
properties file, that identifier becomes available in the `dependencies` configuration block.

### Example

In your `~/.gradle/dependencies.properties`:

  commonsLang: commons-lang:commons-lang:2.5

In your `build.gradle`:

  apply plugin:com.smokejumperit.gradle.DepNamesPlugin
  apply plugin:'java'

  repositories {
    mavenCentral()
  }
  dependencies {
    compile commonsLang
  }

Result: You now have commonsLang in your classpath.

## OneJar Plugin

### Description

Provides tasks to generate a single executable jar containing dependencies.  Uses [one-jar](http://one-jar.sourceforge.net) under the hood.
The resulting jar file will be next to the standard jar, but with '-fat' attached to the name.

## Javacc Plugin

### Description

Provides tasks to compile JavaCC/JJTree files.  Apply the plugin and use `-t` to get details on the tasks if you want to call them
directly: otherwise, the parser will be generated and the files compiled as a prelude to `compileJava`, and the resulting parser
will be placed into the archive of the `jar` task.

The `JAVACC_HOME` environment variable (that's *environment variable*, not *Java property*) must be set to the home directory of
the JavaCC installation, as per the `javacchome` parameter of [the Ant `javacc` task](http://ant.apache.org/manual/Tasks/javacc.html).

The source for JavaCC/JJTree (including `*.java` support files) should be put in folder packages under 
`./src/javacc` --- it's not a source-set, so there's no `java`
or `javacc` subdirectory.  So if you want to generate your parser in `com.smokejumperit.parser`, the location for the JJTree file
is `./src/javacc/com/smokejumperit/parser/myparser.jjt`.  If you want to change that location, change the `javaccSrcDir` property
of your project.

Java files (`*.java`) adjacent to JavaCC or JJTree files (`*.jj` or `*.jjt`) will be deleted on clean.  JavaCC files (`*.jj`) adjacent 
to JJTree files (`*.jjt`) will be deleted on clean.  Any `*.java` files (or other files) not adjacent to a `*.jj` and `*.jjt` file will
survive a clean.

## ClassLoadersPlugin

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

Runs [Cucumber](http://cukes.info).  Specifically, provides a task (`runFeatures`) that executes the features, which are assumed to reside in `./src/test/features/`.  By default, the classloader for the `cuke` configuration is used when the features are executed.  That configuration includes `testRuntime`, `runtime`, `jruby`, and the classes generated from the `test` and `main` source sets.

To modify the defaults, change `convention.plugin.cuke`.  The `configs` property of that object is a list of configurations to load when executing feature tests.  The `featuresDir` property of that object denotes where the root of the features reside.

This is an integration with [Cuke4Duke](http://wiki.github.com/aslakhellesoy/cuke4duke/): the `ant` property of the project now has a `cuke`
task to execute Cucumber.

You may have to run with `gradle -i` in order to see the Cucumber output.

# Installation

Add the following lines to your build script to add the jars to your buildscript classpath and use the plugins:

    // Example of using two plugins
    apply plugin:com.smokejumperit.gradle.ClassLoadersPlugin
    apply plugin:com.smokejumperit.gradle.ExecPlugin

    buildscript {
      repositories {
        mavenRepo urls:'http://repo.smokejumperit.com'
      }
      dependencies {
        classpath 'com.smokejumperit:gradle-plugins:0.6.2'
      }
    }

If you want to ust use all the SmokejumperIT plugins, you can do this:

    apply plugin:com.smokejumperit.gradle.AllPlugins

    buildscript {
      repositories {
        mavenRepo urls:'http://repo.smokejumperit.com'
      }
      dependencies {
        classpath 'com.smokejumperit:gradle-plugins:0.6.2'
      }
    }

See the `sample` directory of this project for a build script which does this.

# Author and Origin

These plugins were written by [Robert Fischer](http://smokejumperit.com/).  They are published at [GitHub:RobertFischer/gradle-plugins](http://github.com/RobertFischer/gradle-plugins).

# Additional Credit

Thanks to [Jeppe Nejsum Madsen](http://jeppenejsum.wordpress.com) for some additional testing and work (found an NPE circumstance and supplied a patch).

# License

All these plugins are licensed under the [Creative Commons — CC0 1.0 Universal](http://creativecommons.org/publicdomain/zero/1.0/) license with no warranty (expressed or implied) for any purpose.

# RedPen plugin for Intellij IDEA and other JetBrains IDEs

## About

This plugin integrates [RedPen](http://redpen.cc) text validation into the Intellij IDEA by adding a new RedPen inspection.

### Features

* Validates text with RedPen as you type
* Supports Plain Text, Markdown and AsciiDoc file formats (make sure the relevant plugins are also installed)
* Validation error messages can be listed by pressing *Ctrl+Alt+Shift+R* or via menu *Analyze -> RedPen: List Errors*.
* RedPen configuration can be modified in Settings -> Editor -> RedPen
* Supports all default RedPen languages and variants (English, Japanese)
* Language and variant are autodetected for each file and can be manually overridden per file via status bar widget
* Settings are stored per project under *.idea/redpen* directory, so can be shared with fellow developers
* Custom dictionaries can be put to *.idea/redpen* directory and JavaScriptValidator scripts can be put to *.idea/redpen/js*. 

## Installation

Stay tuned until we publish the plugin to JetBrains Plugin repository!

## For developers [![Build Status](https://travis-ci.org/redpen-cc/redpen-intellij-plugin.svg?branch=master)](https://travis-ci.org/redpen-cc/redpen-intellij-plugin)

Before project will compile you need to fetch dependencies into *lib* directory:

  ```ant deps```
  
Then open the provided project files with IntelliJ IDEA, setup *Intellij Platform SDK* for the project and run **Plugin** configuration.

For command-line builds you can also download the latest IDEA into *idea* subdirectory automatically:

  ```ant download-idea```

Publishing to JetBrains Plugin Repository is done using (make sure JETBRAINS_PWD environment variable is set):

  ```ant publish```
  
Publishing is done by [Travis](https://travis-ci.org/redpen-cc/redpen-intellij-plugin) on every successful build.

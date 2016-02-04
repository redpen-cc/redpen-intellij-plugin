# RedPen plugin for Intellij IDEA and other JetBrains IDEs

## About

This plugin integrates [RedPen](http://redpen.cc) text validation into the Intellij IDEA by adding a new RedPen inspection.

### Features

* Validates text with RedPen as you type
* Supports Plain Text, Markdown and AsciiDoc file formats
* Validation error messages can be listed by pressing *Ctrl+Alt+Shift+R* or via menu *Analyze -> RedPen: List Errors*.
* Currently, only English is supported

## Installation

Stay tuned until we publish the plugin to JetBrains Plugin repository!

## For developers

Before project will compile you need to fetch dependencies into *lib* directory:

  ```ant deps```
  
Then open the provided project files with IntelliJ IDEA, setup *Intellij Platform SDK* for the project and run **Plugin** configuration.

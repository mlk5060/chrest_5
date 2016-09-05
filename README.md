# jCHREST #

This software project contains materials relating to the Java implementation of
the CHREST cognitive architecture.  

The architecture can be easily "plugged-into" any computational implementation
of psychological experiments compatible with Java and facilitates studies into
human cognitive behaviour and processes.

## Notable features ##

* Cognitive processes adhere to principles of bounded-rationality, i.e.
	* Cognitive processes are time-sensitive
	* Cognitive structures have limited capacity for information input/output
	to/from other cognitive structures
* Separation of long and short term memory
	* Long-term memory implemented as a discrimination network
	* SHort-term memory implemented as a pseudo FIFO list.
* Ability to learn (store) strings, numbers and locations of objects in 2D space
in action, verbal and visual modalities
* Ability to create associate learned information together both automatically
and manually
* Simulated input/output interfaces to cognitive structures
	* Eye
* Extensible domain-specific functionality
	* Eye fixation types
	* Eye fixation strategies
	* Input normalisation
* Visual-Spatial field (for mental planning, reasoning etc.)
* First or third-person learning

## Using jChrest ##

Please compile jChrest first (see section below) and then run the Jar file in 
the `target` folder to load the jChrest GUI.

## Compiling jChrest ##

### Setup ###

Compilation uses a combination of [JRuby](http://jruby.org) and [Apache
buildr](https://buildr.apache.org/). If you haven't already, install JRuby (see
documentation [here](http://jruby.org/getting-started), we recommend installing
JRuby using the [Ruby Version Manager](https://rvm.io/)) and then the `buildr`
RubyGem by issuing the following command in your command-line interface:

  > gem install buildr

### Compilation ###

To just compile jChrest source code and not create a Jar, issue the following
command in your command-line interface when in the top-level directory of
jChrest:

  > buildr compile

To package jChrest into a self-contained Jar file, navigate to the `targets`
directory and issue the following command in your command-line interface:

  > buildr package

### Testing ###

The jChrest test suite uses JRuby code to write tests and the 
[modellers_testing_framework](https://rubygems.org/gems/modellers_testing_framework) 
RubyGem to run the tests in the suite.

Assuming the JRuby executable is on your system's `PATH`, install the 
`modellers_testing_framework` RubyGem using:

  > jruby -S gem install modellers_testing_framework

To run the test suite, issue the following command in your command-line
interface when in the top-level directory of jChrest:

  > buildr tests

*NOTE:* issuing `buildr tests` will also package jChrest as a Jar before
the test suite is run so you can simply "skip" issuing `buildr package` in this
case.  Indeed, we recommend using this command instead of `buildr package` since
running tests should also verify if new code breaks existing code.

*NOTE:* the test suite does not as of yet offer 100% code coverage and cannot
therefore guarantee that modifications to existing code does not break other
existing code.

### Documentation ###

There are two documents, a user-guide and a manual, these are stored in the
`doc` folder.  Issue the following commands in your command-line interface when
in the top-level directory of jChrest to construct the two documents. PDFs
for the documents are stored in `doc/user-guide` and `doc/manual` respectively.

  > buildr guide

  > buildr manual

### Packaging ###

To bundle jChrest's Jar, documentation and examples into a zip file in the
'release' folder, issue the following command in your command-line interface
when in the top-level directory of jChrest:

  > buildr bundle

## Versioning ##

The versioning for jChrest follows [Semantic Versioning](http://semver.org/). 
Versioning starts at 5.1.0 since the jChrest repository was created when CHREST
5 was released (the versioning system used up until CHREST 5 was not Semantic
Versioning). Starting from 5.1.0 instead of 1.0.0 may seem incongruent with
Semantic Versioning however, 5 is used as the first major version since it
reflects CHREST's previous development and versioning and is retained for
posterity.

## License ##

Chrest project files may be redistributed under the terms of the [Open Works
License](http://owl.apotheon.org/).

[JFreeChart](http://www.jfree.org/jfreechart/) is used to provide the graphs
within CHREST, and the distribution includes JFreeChart under the terms of the
[GNU Lesser General Public Licence](http://www.gnu.org/licenses/lgpl.html). 

## Contributors ##

This repository is maintained by [Martyn Lloyd-Kelly](http://https://liverpool.ac.uk/psychology-health-and-society/staff/martyn-brendan-lloyd-kelly/).

The original design and ideas behind CHREST were created by [Fernand Gobet](https://www.liverpool.ac.uk/psychology-health-and-society/staff/fernand-gobet/).

CHREST's Java implementation and continued support was/is provided by [Peter Lane](http://peterlane.info/).

The emotions module was created by [Marvin Schiller](http://www.marvin-schiller.de/).


# CHREST 5 #

This software project contains materials relating to the CHREST 5 cognitive 
architecture.  

CHREST 5 pays particular attention to time and has an improved public API enabling the software to be easily applied in a number of existing domains and can be easily extended to work with others.

Notable improvements from CHREST 4:

* Visual-Spatial field implementation (for mental planning, reasoning etc.)
* Publicly extendable domain-specific fixations 
* Automated fixation performance
* Automated chunk association (including visual-action or production associations)
* First or third-person learning
* Improved debugging

## Using Chrest ##

Simply run `target/chrest-5.jar` to load the CHREST GUI.

## Compiling Chrest ##

### Setup ###

Compilation uses a 'buildr' script.  So first install 'buildr' using:

  > gem install buildr

Testing uses [jruby](http://jruby.org) and the
[modellers_testing_framework](https://rubygems.org/gems/modellers_testing_framework) jruby gem.
Assuming the jruby executable is on your PATH, install the gem using:

  > jruby -S gem install modellers_testing_framework

### Compilation ###

To compile CHREST (no jar created):

  > buildr compile

To package CHREST into a self-contained jar file, in the 'targets' folder:

  > buildr package

### Testing ###

To run the test suite:

  > buildr tests

### Documentation ###

There are two documents, a user-guide and a manual.  These are stored in the
'doc' folder.  Use the following the construct the two documents, pdfs are
stored in 'doc/user-guide' and 'doc/manual' respectively.

  > buildr guide

  > buildr manual

### Packaging ###

To bundle CHREST, the documentation and examples into a zip file in the 'release' 
folder:

  > buildr bundle

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


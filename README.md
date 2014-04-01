# CHREST #

This software project contains materials relating to the CHREST cognitive 
architecture.  

## Using Chrest ##

Details on using Chrest will be made available on the
[wiki](https://github.com/petercrlane/chrest/wiki).

A prepackaged version of Chrest is available at
[http://chrest.info/software.html](http://chrest.info/software.html)

## Compiling Chrest ##

### Setup ###

Compilation uses a 'buildr' script.  So first install 'buildr' using:

  > gem install buildr

Testing uses [jruby](http://jruby.org) and a jruby gem
[modellers_testing_framework](https://rubygems.org/gems/modellers_testing_framework).
Assuming the jruby executable is on the PATH, install the gem using:

  > jruby -S gem install modellers_testing_framework

### Compilation ###

To compile CHREST:

  > buildr compile

To package CHREST into a self-contained jar file, in the 'targets' folder:

  > buildr package

### Testing ###

To run the tests:

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

The latest version of CHREST can be downloaded pre-packaged from
[http://chrest.info/software.html](http://chrest.info/software.html).

## License ##

Chrest project files may be redistributed under the terms of the [Open Works
License](http://owl.apotheon.org/).

[JFreeChart](http://www.jfree.org/jfreechart/) is used to provide the graphs
within CHREST, and the distribution includes JFreeChart under the terms of the
[GNU Lesser General Public Licence](http://www.gnu.org/licenses/lgpl.html). 

## Contributors ##

This repository is maintained by [Peter Lane](http://peterlane.info).

The original design and ideas behind CHREST were created by [Fernand Gobet](http://www.brunel.ac.uk/~hsstffg/).

The emotions module was created by [Marvin Schiller](http://www.marvin-schiller.de/).


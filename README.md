# CHREST #

This software project contains materials relating to the CHREST cognitive 
architecture.  

## Using Chrest ##

Details on using Chrest will be made available on the
[wiki](https://github.com/petercrlane/chrest/wiki).

A prepackaged version of Chrest is available at
[http://chrest.info/software.html](http://chrest.info/software.html)

## Compiling Chrest ##

### Compilation ###

Compilation uses a 'buildr' script.  So first install 'buildr' using:

  > gem install buildr

To compile CHREST:

  > buildr compile

To package CHREST into a self-contained jar file, in the 'targets' folder:

  > buildr package


### Testing ###

For testing CHREST, first install jruby.  Assuming the jruby executable is on the path, 
install the gem:

  > jruby -S gem install modellers_testing_framework

and to run the tests:

  > buildr tests

### Documentation ###

There are two documents, a user-guide and a manual.  These are stored in the
'doc' folder.  Use the following the construct the two documents, pdfs are
stored in 'doc/user-guide' and 'doc/manual' respectively.

  > buildr guide
  > buildr manual

## License ##

Chrest project files may be redistributed under the terms of the [Open Works
License](http://owl.apotheon.org/).

[JFreeChart](http://www.jfree.org/jfreechart/) is used to provide the graphs
within CHREST, and the distribution includes JFreeChart under the terms of the
[GNU Lesser General Public Licence](http://www.gnu.org/licenses/lgpl.html). 

## Contributors ##

The original design and ideas behind Chrest were created by [Fernand Gobet](www.brunel.ac.uk/~hsstffg/).

The emotions module was created by [Marvin Schiller](http://www.marvin-schiller.de/).


CHREST
======

This software project contains materials relating to the CHREST cognitive 
architecture.  

Using Chrest
------------

Details on using Chrest will be made available on the
[wiki](https://github.com/petercrlane/chrest/wiki).

A prepackaged version of Chrest is available at
[http://chrest.info/software.html](http://chrest.info/software.html)

Compiling Chrest
----------------

The Chrest source code is within the folder +src/jchrest-architecture+.  To compile the 
code, you also require the following jar files within your project folder or on your 
CLASSPATH:

- [JCommon](http://sourceforge.net/projects/jfreechart/files/3.%20JCommon/), version 1.0.17 or later  
- [JFreeChart](http://sourceforge.net/projects/jfreechart/), version 1.0.14 or later

Alternatively, the Chrest folder includes a Rakefile which can be used for
compiling and building Chrest.  The Rakefile assumes the presence of Ruby (to
run Rake), a Java compiler, and some bash functions - the Rakefile has only
been tested on Ubuntu, and may need adapting for other platforms.

Before using the Rakefile, you need to add the two required library files to
the lib folder.  Download from the following sites, unpack and place the
respective jar file into the lib folder:

- [JCommon](http://sourceforge.net/projects/jfreechart/files/3.%20JCommon/), version 1.0.17
- [JFreeChart](http://sourceforge.net/projects/jfreechart/), version 1.0.14

(Later versions should work fine, but will need the corresponding change to the Rakefile.)

    > rake -T

will show a list of available tasks. 

The test suite requires jruby - edit the Rakefile to suit your runtime.  
You will need to install the testing gem:

    > jruby -S gem install modellers_testing_framework

License
-------

Chrest project files may be redistributed under the terms of the [Open Works
License](http://owl.apotheon.org/).

[JFreeChart](http://www.jfree.org/jfreechart/) is used to provide the graphs
within CHREST, and the distribution includes JFreeChart under the terms of the
[GNU Lesser General Public Licence](http://www.gnu.org/licenses/lgpl.html). 

Contributors
------------

The original design and ideas behind Chrest were created by [Fernand Gobet](www.brunel.ac.uk/~hsstffg/).

The emotions module was created by [Marvin Schiller](http://www.marvin-schiller.de/).


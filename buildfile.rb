# Buildr file for managing the CHREST project

VERSION = '4.0.0-alpha-2'

repositories.remote << 'http://repo1.maven.org/maven2'

H2DATABASE = 'com.h2database:h2:jar:1.4.187'
JCOMMON = 'jfree:jcommon:jar:1.0.16'
JFREECHART = 'jfree:jfreechart:jar:1.0.13'
JSOUP = 'org.jsoup:jsoup:jar:1.8.2'
STATISTICS = 'org.apache.commons:commons-math:jar:2.2'

define 'chrest' do
  project.version = VERSION
  compile.with H2DATABASE, JCOMMON, JFREECHART, JSOUP, STATISTICS
  package(:jar).with(
    :manifest=>{'Main-Class'=>'jchrest.gui.Shell'}
  ).merge(
    compile.dependencies
  )

  run.with(H2DATABASE, JCOMMON, JFREECHART).using :main => "jchrest.gui.Shell"
end

desc 'build the user guide'
task :guide do
  Dir.chdir('doc/user-guide') do
    if !File.exists?('user-guide.pdf') ||
      (File.stat('user-guide.txt').mtime > File.stat('user-guide.pdf').mtime)
      sh 'a2x -fpdf -darticle --dblatex-opts "-P latex.output.revhistory=0" user-guide.txt'
    end
  end
end

desc 'build the manual'
task :manual do
  Dir.chdir('doc/manual') do
     if !File.exists?('manual.pdf') || 
       (File.stat('manual.txt').mtime > File.stat('manual.pdf').mtime)
      sh 'asciidoc-bib -s ieee manual.txt'
      sh 'a2x -fpdf -darticle --dblatex-opts "-P latex.output.revhistory=0" manual-ref.txt'
      sh 'mv manual-ref.pdf manual.pdf'
    end
  end
end

desc 'run all Chrest tests'
task :tests => :compile do
  Dir.chdir('tests') do
    #Make sure sqlite4java JAR is on classpath or tests won't run due to missing dependencies
    sh 'jruby -J-cp ../target/classes:../sqlite4java-392/sqlite4java.jar all-chrest-tests.rb'
  end
end

directory 'release/chrest'
desc 'bundle for release'
task :bundle => [:guide, :manual, :package, :doc, 'release/chrest'] do
  Dir.chdir('release/chrest') do
    sh 'rm -rf documentation' # remove it if exists already
    sh 'mkdir documentation'
    sh 'cp ../../lib/license.txt documentation'
    sh 'cp ../../doc/user-guide/user-guide.pdf documentation'
    sh 'cp ../../doc/manual/manual.pdf documentation'

    sh "cp ../../target/chrest-#{VERSION}.jar ./chrest.jar"
    sh 'cp -r ../../examples .'

    sh 'cp -r ../../target/doc documentation/javadoc'
    File.open("start-chrest.sh", "w") do |file|
      file.puts <<END
java -Xmx100M -jar chrest.jar
END
    end
    File.open("start-chrest.bat", "w") do |file|
      file.puts <<END
start javaw -Xmx100M -jar chrest.jar
END
    end
    File.open("README.txt", "w") do |file|
      file.puts <<END
See documentation/user-guide.pdf for information on running and using CHREST.
END
    end
  end
  Dir.chdir('release') do
    sh "zip -FS -r chrest-#{VERSION}.zip chrest"
  end
end


# Buildr file for managing the CHREST project

version = "8.0.3"

repositories.remote << 'http://repo1.maven.org/maven2'

H2DATABASE = 'com.h2database:h2:jar:1.4.187'
JCOMMON = 'jfree:jcommon:jar:1.0.16'
JFREECHART = 'jfree:jfreechart:jar:1.0.13'
JSOUP = 'org.jsoup:jsoup:jar:1.8.2'
REFLECTIONS = transitive('org.reflections:reflections:jar:0.9.10')
STATISTICS = 'org.apache.commons:commons-math:jar:2.2'

# Contains genetic algorithms like the "Roulette Wheel Selection" algorithm
# that is used when CHREST is asked to generate an action using visual
# pattern recognition.
WATCHMAKER_FRAMEWORK = 'org.uncommons.watchmaker:watchmaker-framework:jar:0.7.1' 

define 'chrest' do
  puts "\n\e[33mThe current CHREST version is set to: '" + version + "' would you like to update this? (y/n)\e[0m"
  decision = STDIN.gets.chomp

  if decision == "y"
    puts "\n\e[33mPlease enter the new version number:\e[0m"
    version = STDIN.gets.chomp
  end
  puts ""

  #Remove any previous CHREST JARs so only the JAR to be created exists in "target"
  if !Dir.glob('./target/*.jar').empty?
    sh "rm ./target/*.jar"
  end

  project.version = version
  compile.with(H2DATABASE, JCOMMON, JFREECHART, JSOUP, REFLECTIONS, STATISTICS, WATCHMAKER_FRAMEWORK)
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
    Rake::Task["package"].invoke #Create a new JAR so that the classpath set below uses the most up-to-date version of CHREST
    sh "jruby -J-cp ../target/chrest-#{version}.jar all-chrest-tests.rb" #Run tests using most up-to-date CHREST code.
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

    sh "cp ../../target/chrest-#{version}.jar ./chrest.jar"
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
    sh "zip -FS -r chrest-#{version}.zip chrest"
  end
end

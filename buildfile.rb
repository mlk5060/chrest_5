# Buildr file for managing the CHREST project

repositories.remote << 'http://repo1.maven.org/maven2'

JCOMMON = 'jfree:jcommon:jar:1.0.16'
JFREECHART = 'jfree:jfreechart:jar:1.0.13'

define 'chrest' do
  project.version = '4.0.0-alpha-2'
  compile.with JCOMMON, JFREECHART
  package(:jar).with(
    :manifest=>{'Main-Class'=>'jchrest.gui.Shell'}
  ).merge(
    compile.dependencies
  )

  run.with(JCOMMON, JFREECHART).using :main => "jchrest.gui.Shell"
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
    sh 'jruby -J-cp ../target/classes all-chrest-tests.rb'
  end
end

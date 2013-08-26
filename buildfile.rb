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


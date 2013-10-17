package blackboard.gradle.jasmine

import org.gradle.api.DefaultTask
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.file.DefaultSourceDirectorySet
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.tasks.TaskAction

import java.util.jar.JarEntry
import java.util.jar.JarFile

class JasmineTask extends DefaultTask {

  SourceDirectorySet source;

  public SourceDirectorySet getSource() {
    return source
  }

  protected void setFileResolver( FileResolver resolver ) {
    source = new DefaultSourceDirectorySet( "Jasmine Source", resolver )
    source.srcDir(project.file("src/test/jasmine"))
    source.filter.include("**/*.js")
  }

  @TaskAction
  public void runJasmineTests() {
    source.files.each { file ->
      runJasmine( file )
    }
  }

  def writeResource( String name, JarFile jar, String path ) {
    JarEntry entry = jar.getEntry( name )
    def stream = jar.getInputStream( entry )
    def out = new FileWriter( path + name )

    try {
      stream.eachLine { line ->
        out.println( line )
      }
    }
    finally {
      out.close()
      stream.close()
    }
  }

  def writeJasmineResources( path ) {
    project.buildscript.configurations.classpath.each { artifact ->
      JarFile jar = new JarFile( artifact )

      try {
        JarEntry entry = jar.getEntry( "jasmine.js" )

        if ( entry ) {
          writeResource( "jasmine.js", jar, path )
          writeResource( "jasmine-html.js", jar, path )
          writeResource( "jasmine.console_reporter.js", jar, path )
          writeResource( "jasmine.junit_reporter.js", jar, path )
          writeResource( "run-jasmine.js", jar, path )
        }
      }
      finally {
        jar.close()
      }
    }
  }

  public void runJasmine( File file ) {
    def jasmineTestPath = project.buildDir.absolutePath + "/jasmine/"
    project.mkdir( jasmineTestPath )

    writeJasmineResources( jasmineTestPath )

    // start writing the driver
    def driver = project.file( jasmineTestPath + "/" + "jasmine.html" )

    driver.withPrintWriter { out ->
      out.println( '<html><head>\n' +
              '  <script type="text/javascript" src="jasmine.js"></script>\n' +
              '  <script type="text/javascript" src="jasmine-html.js"></script>\n' +
              '  <script type="text/javascript" src="jasmine.console_reporter.js"></script>\n' +
              '  <script type="text/javascript" src="jasmine.junit_reporter.js"></script>\n' +
              '  <script type="text/javascript">\n' +
              '    (function() {\n' +
              '      var jasmineEnv = jasmine.getEnv();\n' +
              '      jasmineEnv.updateInterval = 1000;\n' +
              '      jasmineEnv.addReporter(new jasmine.HtmlReporter());\n' +
              '      jasmineEnv.addReporter(new jasmine.JUnitXmlReporter("target/test-reports/"));\n' +
              '      jasmineEnv.addReporter(new jasmine.ConsoleReporter());\n' +
              '      window.onload = function() {\n' +
              '        jasmineEnv.execute();\n' +
              '      };\n' +
              '    })();\n' +
              '  </script>');

      def scriptTagWritten = false

      project.file( file ).eachLine { line ->
        if ( line.trim().startsWith( "testing(") ) {
          def includeFile = line.trim().substring( 8, line.trim().lastIndexOf( ')' ) ).trim()
          includeFile = findFileUnderTest( file, jasmineTestPath, includeFile.substring( 1, includeFile.length() - 1 ) )

          out.println( '<script type="text/javascript" src="' + includeFile.absolutePath + '"></script>' )
        }
        else {
          if ( !scriptTagWritten )
            out.println( '<script type="text/javascript">' )

          scriptTagWritten = true
          out.println( line )
        }
      }

      out.println( '</script></head><body></body></html>' )
    }

    project.exec {
      commandLine = ['phantomjs', project.buildDir.absolutePath + '/jasmine/run-jasmine.js', driver.absolutePath]
    }
  }

  def findFileUnderTest( File specFile, String jasmineTestPath, String testingPath ) {
    // first check relative to the current path
    def file = project.file( testingPath )
    if (file.exists())
      return file

    file = project.file( jasmineTestPath + "/" + testingPath )
    if (file.exists())
      return file

    return project.file( "src/main/webapp/" + testingPath )
  }

}

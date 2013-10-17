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

  public SourceDirectorySet srcDir( Object dir ) {
    return source.srcDir( dir )
  }

  public SourceDirectorySet srcDirs( Object... dirs ) {
    return source.srcDirs( dirs )
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

  def writeResourceToStream( String name, JarFile jar, FileWriter out ) {
    JarEntry entry = jar.getEntry( name )
    def stream = jar.getInputStream( entry )

    try {
      stream.eachLine { line ->
        out.println( line )
      }

      out.println()
      out.flush()
    }
    finally {
      stream.close()
    }
  }

  def writeResourceToFile( String name, JarFile jar, String file, boolean append ) {
    def out = new FileWriter( file, append )

    try {
      writeResourceToStream( name, jar, out )
    }
    finally {
      out.close()
    }
  }

  def writeResourceToPath( String name, JarFile jar, String path ) {
    writeResourceToFile( name, jar, path + name, false )
  }

  def writeJasmineResources( path ) {
    def gradleJasmine = path + "jasmine.js"

    project.buildscript.configurations.classpath.each { artifact ->
      JarFile jar = new JarFile( artifact )

      try {
        JarEntry entry = jar.getEntry( "jasmine.js" )

        if ( entry ) {
          writeResourceToFile( "jasmine.js", jar, gradleJasmine, false )
          writeResourceToFile( "jasmine-html.js", jar, gradleJasmine, true )
          writeResourceToFile( "jasmine-init.js", jar, gradleJasmine, true )
          writeResourceToPath( "jasmine-execute.js", jar, path )
          writeResourceToPath( "run-jasmine.js", jar, path )
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
      out.println( '<html>\n  <head>\n    <script type="text/javascript" src="jasmine.js"></script>\n' +
              '    <script type="text/javascript" src="jasmine-execute.js"></script>' )

      project.file( file ).eachLine { line ->
        if ( line.trim().startsWith( "testing(") ) {
          def includeFile = line.trim().substring( 8, line.trim().lastIndexOf( ')' ) ).trim()
          includeFile = findFileUnderTest( file, includeFile.substring( 1, includeFile.length() - 1 ) )
          out.println( '    <script type="text/javascript" src="' + includeFile.absolutePath + '"></script>' )
        }
        else if ( line.trim().startsWith( "runWith(") ) {
          def runFile = line.trim().substring( 8, line.trim().lastIndexOf( ')' ) ).trim()
          driver = findFileUnderTest( file, runFile.substring( 1, runFile.length() - 1 ) )
        }
      }

      out.println( '    <script type="text/javascript" src="' + file.absolutePath + '"></script>' )
      out.println( '  </head>\n  <body>\n  </body>\n</html>' )
    }

    def phantomjs = (System.getProperty("os.name").toLowerCase().contains("windows") ? "phantomjs.exe" : "phantomjs")
    project.exec {
      commandLine = [phantomjs, jasmineTestPath + 'run-jasmine.js', driver.absolutePath]
    }
  }

  def findFileUnderTest( File specFile, String testingPath ) {
    // relative to the test spec
    def file = new File( specFile.parentFile, testingPath )
    if (file.exists())
      return file

    // from project root
    file = project.file( testingPath )
    if (file.exists())
      return file

    // from webapp root
    return project.file( "src/main/webapp/" + testingPath )
  }

}

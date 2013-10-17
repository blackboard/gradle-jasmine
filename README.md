The gradle-jasmine plugin adds Jasmine/PhantomJS integration to Gradle.  To use, apply the jasmine plugin:

    buildscript {
        repositories {
            maven {
                url "https://maven.blackboard.com/content/repositories/releases"
            }
        }
        dependencies {
            classpath "blackboard:gradle-jamsine:1.0"
        }
    }

    apply plugin: "jasmine"

By default, all \*.js files under *src/test/jasmine* will be run as Jasmine test specs.  You can add additional source directories
to the Jasmine plugin:

    jasmine.srcDir "src/main/javascript"

There is no need to include any HTML documents with the Jasmine specs - you can add *testing()* references to the spec:

    testing("foo.js");

    describe("A scenario", function() {
        it("A test", function() {
            Foo f = new Foo();
            assertTrue( f.something );
        }
    });

The *testing()* lines will search for the target file under:

1. Relative to the test spec.
2. Relative to the project root.
3. Relative to *src/main/webapp*.

If you wish to write your own HTML test runner, you can by adding a *runWith()* call at the beginning of your test
specification file and referencing the dynamically generated *${project.buildDir}/jasmine/jasmine.js* file from your
test runner:

    runWith("runner.html")
    testing("foo.js")

    describe("A scenario", function() {
        it("A test", function() {
            Foo f = new Foo();
            assertTrue( f.something );
        }
    });

The runner would look like this:

    <html>
      <head>
        <script type="text/javascript" src="../../../target/jasmine/jasmine.js"></script>

        <!-- You can reference this dynamic JS file to execute the Jasmine tests once the page loads -->
        <script type="text/javascript" src="../../../target/jasmine/jasmine-execute.js"></script>

        <!-- The SUT (Script Under Test)
        <script type="text/javascript" src="../../../src/main/webapp/test.js"></script>

        <!-- The specification -->
        <script type="text/javascript" src="test3.js"></script>
      </head>
      <body>
        ... Any HTML ...
      </body>
    </html>

#### NOTE: You must have PhantomJS installed and in your execution path on whatever test platform your run on.

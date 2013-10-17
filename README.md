gradle-jasmine
==============

The gradle-jasmine plugin adds Jasmine/PhantomJS integration to Gradle.  To use, apply the jasmine plugin:

    buildscript {
        repositories {
            mavenCentral()
        }
        dependencies {
            classpath "blackboard:gradle-jamsine:1.0-SNAPSHOT"
        }
    }

    apply plugin: "jasmine"

By default, all *.js files under *src/test/jasmine* will be run as Jasmine test specs.  There is no need to include
any HTML documents with the Jasmine specs - you can add *testing()* references to the spec:

    testing("foo.js");

    describe("A scenario", function() {
        it("A test", function() {
            Foo f = new Foo();
            assertTrue( f.something );
        }
    });

The testing() lines will search for the target file under:
1. Relative to the project root.
2. Relative to the test spec.
3. Relative to *src/main/webapp*.
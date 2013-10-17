package blackboard.gradle.jasmine

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.file.FileResolver

import javax.inject.Inject

class JasminePlugin  implements Plugin<Project> {

  private final FileResolver fileResolver

  @Inject
  JasminePlugin( FileResolver resolver ) {
    fileResolver = resolver
  }

  @Override
  void apply( Project project ) {
    JasmineTask task = (JasmineTask)project.task( "jasmine", type: JasmineTask )
    task.fileResolver = fileResolver

    project.afterEvaluate {
      project.test.dependsOn task
    }
  }

}

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * Registers the generateCreditsSchemaDoc and docs tasks,
 * and wires generateCreditsSchemaDoc into the check lifecycle.
 *
 * <p>credits.json conformance is enforced by unit tests in
 * {@code src/test/java/.../CreditsJsonValidationTest.java}, which run as part of
 * the {@code test} task (itself a dependency of {@code check}).
 */
public class CreditsPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getTasks().register("generateCreditsSchemaDoc", GenerateCreditsSchemaDocTask.class, task -> {
            task.setGroup("documentation");
            task.setDescription("Generates credits.schema.md from credits.schema.json.");
            task.setSchemaFile(project.getLayout().getProjectDirectory()
                .file("src/main/resources/assets/gtnhcredits/credits.schema.json").getAsFile());
            task.setOutputFile(project.getLayout().getProjectDirectory()
                .file("credits.schema.md").getAsFile());
        });

        project.getTasks().register("docs", task -> {
            task.setGroup("documentation");
            task.setDescription("Generates all project documentation.");
            task.dependsOn("generateCreditsSchemaDoc");
        });

        project.getPluginManager().withPlugin("java-base", appliedPlugin -> {
            project.getTasks().named("check").configure(task -> {
                task.dependsOn("generateCreditsSchemaDoc");
            });
        });
    }
}
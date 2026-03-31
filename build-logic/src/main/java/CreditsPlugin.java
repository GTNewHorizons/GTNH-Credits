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
                .file("credits.schema.json").getAsFile());
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

        String clientDir = String.valueOf(project.findProperty("runClientWorkingDirectory") != null
            ? project.findProperty("runClientWorkingDirectory") : "run/client");
        String serverDir = String.valueOf(project.findProperty("runServerWorkingDirectory") != null
            ? project.findProperty("runServerWorkingDirectory") : "run/server");

        project.getTasks().register("syncDevClient", org.gradle.api.tasks.Copy.class, task -> {
            task.setDescription("Copies dev files into the run/client directory.");
            task.from("src/dev/client");
            task.into(clientDir);
        });

        project.getTasks().register("syncDevServer", org.gradle.api.tasks.Copy.class, task -> {
            task.setDescription("Copies dev files into the run/server directory.");
            task.from("src/dev/server");
            task.into(serverDir);
        });

        project.getTasks().configureEach(task -> {
            String name = task.getName();
            if (name.startsWith("runClient")) {
                task.dependsOn("syncDevClient");
            } else if (name.startsWith("runServer")) {
                task.dependsOn("syncDevServer");
            }
        });
    }
}
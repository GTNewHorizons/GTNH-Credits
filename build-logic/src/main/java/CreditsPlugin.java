import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * Registers the validateCreditsJson, generateCreditsSchemaDoc, and docs tasks,
 * and wires validateCreditsJson + generateCreditsSchemaDoc into the check lifecycle.
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

        project.getTasks().register("validateCreditsJson", ValidateCreditsJsonTask.class, task -> {
            task.setGroup("verification");
            task.setDescription("Validates credits.json against credits.schema.json. "
                + "Checks schema compliance (draft-07), category id uniqueness, "
                + "and person category index bounds.");
            task.setJsonFile(project.getLayout().getProjectDirectory()
                .file("src/main/resources/assets/gtnhcredits/credits.json").getAsFile());
            task.setSchemaFile(project.getLayout().getProjectDirectory()
                .file("credits.schema.json").getAsFile());
            task.setMarkerFile(project.getLayout().getBuildDirectory().get()
                .file("validations/credits-json.txt").getAsFile());
        });

        project.getTasks().register("docs", task -> {
            task.setGroup("documentation");
            task.setDescription("Generates all project documentation.");
            task.dependsOn("generateCreditsSchemaDoc");
        });

        project.getPluginManager().withPlugin("java-base", appliedPlugin -> {
            project.getTasks().named("check").configure(task -> {
                task.dependsOn("validateCreditsJson");
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
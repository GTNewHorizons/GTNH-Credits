tasks.register<GenerateCreditsSchemaDocTask>("generateCreditsSchemaDoc") {
    group = "documentation"
    description = "Generates credits.schema.md from credits.schema.json."
    schemaFile = layout.projectDirectory.file("src/main/resources/assets/gtnhcredits/credits.schema.json").asFile
    outputFile = layout.projectDirectory.file("credits.schema.md").asFile
}

tasks.register<ValidateCreditsJsonTask>("validateCreditsJson") {
    group = "verification"
    description = "Validates credits.json against credits.schema.json. " +
        "Checks schema compliance (draft-07), category id uniqueness, " +
        "and person category index bounds."
    jsonFile   = layout.projectDirectory.file("src/main/resources/assets/gtnhcredits/credits.json").asFile
    schemaFile = layout.projectDirectory.file("src/main/resources/assets/gtnhcredits/credits.schema.json").asFile
    markerFile = layout.buildDirectory.file("validations/credits-json.txt").get().asFile
}

tasks.register("docs") {
    group = "documentation"
    description = "Generates all project documentation."
    dependsOn("generateCreditsSchemaDoc")
}

tasks.named("check") {
    dependsOn("validateCreditsJson")
    dependsOn("generateCreditsSchemaDoc")
}


plugins {
    id("com.gtnewhorizons.gtnhconvention")
    id("net.noiraude.gtnhcredits.build")
}

// CustomMainMenu is client-only and must not run on the server (it loads GuiScreen during preInit,
// which crashes the dedicated server). Keep it out of runServer/runServer25 by using a dedicated
// configuration that is wired only to the client run tasks.
val clientDevRuntime by configurations.creating
val customMainMenuVersion: String by gradle.extra

dependencies {
    clientDevRuntime("com.github.GTNewHorizons:Custom-Main-Menu:$customMainMenuVersion:dev")
}

val setupCustomMainMenuConfig by tasks.registering {
    val configFile = layout.projectDirectory.file("run/client/config/CustomMainMenu/mainmenu.json")
    outputs.file(configFile)
    onlyIf { !configFile.asFile.exists() }
    doLast {
        configFile.asFile.parentFile.mkdirs()
        configFile.asFile.writeText(
            """
            {
              "${'$'}schema": "https://raw.githubusercontent.com/GTNewHorizons/Custom-Main-Menu/master/mainmenu.schema.json",
              "version": 1,
              "images": {},
              "buttons": {
                "credits": {
                  "text": "Credits",
                  "posX": 0,
                  "posY": -160,
                  "width": 150,
                  "height": 20,
                  "alignment": "column_bottom",
                  "action": {
                    "type": "sendIMC",
                    "modid": "gtnhcredits",
                    "message": "openCredits"
                  }
                },
                "singleplayer": {
                  "text": "menu.singleplayer",
                  "posX": 0,
                  "posY": -140,
                  "width": 150,
                  "height": 20,
                  "alignment": "column_bottom",
                  "action": {
                    "type": "openGui",
                    "gui": "singleplayer"
                  }
                },
                "multiplayer": {
                  "text": "menu.multiplayer",
                  "posX": 0,
                  "posY": -120,
                  "width": 150,
                  "height": 20,
                  "alignment": "column_bottom",
                  "action": {
                    "type": "openGui",
                    "gui": "multiplayer"
                  }
                },
                "options": {
                  "text": "menu.options",
                  "posX": 0,
                  "posY": -100,
                  "width": 150,
                  "height": 20,
                  "alignment": "column_bottom",
                  "action": {
                    "type": "openGui",
                    "gui": "options"
                  }
                },
                "mods": {
                  "text": "menu.multiplayer.lan",
                  "posX": 0,
                  "posY": -80,
                  "width": 150,
                  "height": 20,
                  "alignment": "column_bottom",
                  "action": {
                    "type": "openGui",
                    "gui": "mods"
                  }
                },
                "quit": {
                  "text": "menu.quit",
                  "posX": 0,
                  "posY": -20,
                  "width": 150,
                  "height": 20,
                  "alignment": "column_bottom",
                  "action": {
                    "type": "quit"
                  }
                }
              },
              "texts": {},
              "alignments": {
                "column_bottom": {
                  "factorWidth": 0.1,
                  "factorHeight": 0.95
                }
              },
              "other": {
                "background": {
                  "image": "minecraft:textures/gui/options_background.png"
                }
              }
            }
            """.trimIndent()
        )
    }
}

afterEvaluate {
    tasks.matching { it.name.startsWith("runClient") }.configureEach {
        (this as? JavaExec)?.classpath(clientDevRuntime)
        dependsOn(setupCustomMainMenuConfig)
    }
}

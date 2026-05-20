plugins {
    id("dev.kikugie.stonecutter")
}
stonecutter active "1.21.11"

tasks.register("chiseledBuild") {
    group = "project"
    dependsOn(stonecutter.tasks.named("build"))
}

tasks.register("chiseledPublishMods") {
    group = "project"
    dependsOn(stonecutter.tasks.named("publishMods"))
}

plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "1.21.7"

tasks.register("chiseledBuild") {
    group = "project"
    dependsOn(stonecutter.tasks.named("build"))
}

tasks.register("chiseledPublishMods") {
    group = "publishing"
    dependsOn(stonecutter.tasks.named("publishMods"))
}

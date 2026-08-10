plugins {
    java
    `maven-publish`
}

extensions.configure<JavaPluginExtension> {
    withSourcesJar()
}

extensions.configure<PublishingExtension> {
    repositories {
        maven {
            credentials(PasswordCredentials::class.java)

            name = "apiary"
            val base = providers.gradleProperty("apiaryRepositoryUrl").getOrElse("https://repo.velocityctd.com")
            val releasesRepoUrl = "$base/releases"
            val snapshotsRepoUrl = "$base/snapshots"
            setUrl(if (version.toString().endsWith("-SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
        }
    }
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            pom {
                name.set("ApiaryProxy")
                description.set("A Minecraft server proxy with unparalleled server support, scalability and flexibility")
                url.set("https://github.com/ssquadteam/ApiaryProxy")
                scm {
                    url.set("https://github.com/ssquadteam/ApiaryProxy")
                    connection.set("scm:git:https://github.com/ssquadteam/ApiaryProxy.git")
                    developerConnection.set("scm:git:https://github.com/ssquadteam/ApiaryProxy.git")
                }
            }
        }
    }
}

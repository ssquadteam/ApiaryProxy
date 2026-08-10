val currentShortRevision = providers.exec {
    executable = "git"
    args = listOf("rev-parse", "HEAD")
}.standardOutput.asText.map { it.trim().substring(0, 8) }

tasks.withType<Jar> {
    manifest {
        val buildNumber = System.getenv("BUILD_NUMBER")
        val shortRevision = currentShortRevision.get()
        val velocityHumanVersion: String =
            if (project.version.toString().endsWith("-SNAPSHOT")) {
                if (buildNumber == null) {
                    "${project.version}-git-$shortRevision"
                } else {
                    "${project.version}-git-$shortRevision-b$buildNumber"
                }
            } else {
                archiveVersion.get()
            }
        attributes["Implementation-Version"] = velocityHumanVersion
        attributes["Specification-Version"] = shortRevision
        attributes["Enable-Native-Access"] = "ALL-UNNAMED"
    }
}

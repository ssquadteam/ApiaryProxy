/**
 * Precompiled [velocity-init-manifest.gradle.kts][Velocity_init_manifest_gradle] script plugin.
 *
 * @see Velocity_init_manifest_gradle
 */
public
class VelocityInitManifestPlugin : org.gradle.api.Plugin<org.gradle.api.Project> {
    override fun apply(target: org.gradle.api.Project) {
        try {
            Class
                .forName("Velocity_init_manifest_gradle")
                .getDeclaredConstructor(org.gradle.api.Project::class.java, org.gradle.api.Project::class.java)
                .newInstance(target, target)
        } catch (e: java.lang.reflect.InvocationTargetException) {
            throw e.targetException
        }
    }
}

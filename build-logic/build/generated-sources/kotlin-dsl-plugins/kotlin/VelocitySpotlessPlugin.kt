/**
 * Precompiled [velocity-spotless.gradle.kts][Velocity_spotless_gradle] script plugin.
 *
 * @see Velocity_spotless_gradle
 */
public
class VelocitySpotlessPlugin : org.gradle.api.Plugin<org.gradle.api.Project> {
    override fun apply(target: org.gradle.api.Project) {
        try {
            Class
                .forName("Velocity_spotless_gradle")
                .getDeclaredConstructor(org.gradle.api.Project::class.java, org.gradle.api.Project::class.java)
                .newInstance(target, target)
        } catch (e: java.lang.reflect.InvocationTargetException) {
            throw e.targetException
        }
    }
}

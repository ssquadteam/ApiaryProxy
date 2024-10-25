/**
 * Precompiled [velocity-publish.gradle.kts][Velocity_publish_gradle] script plugin.
 *
 * @see Velocity_publish_gradle
 */
public
class VelocityPublishPlugin : org.gradle.api.Plugin<org.gradle.api.Project> {
    override fun apply(target: org.gradle.api.Project) {
        try {
            Class
                .forName("Velocity_publish_gradle")
                .getDeclaredConstructor(org.gradle.api.Project::class.java, org.gradle.api.Project::class.java)
                .newInstance(target, target)
        } catch (e: java.lang.reflect.InvocationTargetException) {
            throw e.targetException
        }
    }
}

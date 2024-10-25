/**
 * Precompiled [velocity-checkstyle.gradle.kts][Velocity_checkstyle_gradle] script plugin.
 *
 * @see Velocity_checkstyle_gradle
 */
public
class VelocityCheckstylePlugin : org.gradle.api.Plugin<org.gradle.api.Project> {
    override fun apply(target: org.gradle.api.Project) {
        try {
            Class
                .forName("Velocity_checkstyle_gradle")
                .getDeclaredConstructor(org.gradle.api.Project::class.java, org.gradle.api.Project::class.java)
                .newInstance(target, target)
        } catch (e: java.lang.reflect.InvocationTargetException) {
            throw e.targetException
        }
    }
}

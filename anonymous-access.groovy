import jenkins.model.*
import hudson.security.*

def instance = Jenkins.getInstance()
def authStrategy = instance.getAuthorizationStrategy()

// Check if the current strategy is a MatrixAuthorizationStrategy
if (authStrategy instanceof GlobalMatrixAuthorizationStrategy) {
    def matrixAuth = authStrategy as GlobalMatrixAuthorizationStrategy

    def anonymousPermissions = matrixAuth.getGrantedPermissions().findAll { permission, sidSet ->
        sidSet.contains("anonymous")
    }

    if (!anonymousPermissions.isEmpty()) {
        println "Anonymous access is currently enabled. Disabling it..."

        anonymousPermissions.each { permission, sidSet ->
            sidSet.remove("anonymous")
            println "Removed permission '${permission.group.title}: ${permission.name}' from anonymous"
        }

        instance.setAuthorizationStrategy(matrixAuth)
        instance.save()
        println "Anonymous access disabled successfully."
    } else {
        println "Anonymous access is already disabled."
    }
} else {
    println "Authorization strategy is not Matrix-based. Script not applicable."
}
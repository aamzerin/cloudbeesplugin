import com.cloudbees.hudson.plugins.folder.properties.AuthorizationMatrixProperty
import jenkins.model.Jenkins
import hudson.security.Permission

println "=== Permissions globales (RBAC CloudBees) ==="

def strategy = Jenkins.instance.getAuthorizationStrategy()

// Vérifie que le système utilise bien une stratégie de type matrix (RBAC activé)
if (strategy.metaClass.respondsTo(strategy, "getGrantedPermissions")) {
    def grantedPermissions = strategy.getGrantedPermissions()

    grantedPermissions.each { Permission permission, Set<String> sids ->
        println "Permission : ${permission.group.title} / ${permission.name}"
        sids.each { sid ->
            println "  - Affecté à : ${sid}"
        }
    }
} else {
    println "La stratégie actuelle ne supporte pas 'getGrantedPermissions'."
}
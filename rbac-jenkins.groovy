import com.cloudbees.opscenter.security.roles.*
import com.cloudbees.opscenter.server.model.*
import com.cloudbees.hudson.plugins.folder.properties.AuthorizationMatrixProperty
import com.synopsys.arc.jenkins.plugins.rolestrategy.*

println "=== Rôles globaux RBAC CloudBees ===\n"

// Récupération du gestionnaire RBAC
def rbac = Jenkins.instance.getExtensionList(RBAC.class)[0]
if (rbac == null) {
    println "RBAC non actif ou non disponible."
    return
}

// Récupère tous les rôles définis
def roles = rbac.getAllRoles()

roles.each { role ->
    println "Rôle : ${role.getName()}"
    println "  Description : ${role.getDescription() ?: 'Aucune'}"
    println "  Permissions :"
    role.getPermissions().each { permission ->
        println "    - ${permission.getId()} (${permission.getName()})"
    }

    println "  Membres affectés :"
    def assignments = rbac.getAssignments(role)
    assignments.each { assignment ->
        println "    - ${assignment.getSid()} (${assignment.isGroup() ? 'Groupe' : 'Utilisateur'})"
    }

    println "---------------------------"
}
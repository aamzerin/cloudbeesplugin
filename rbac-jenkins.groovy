import jenkins.model.*
import hudson.security.*
import com.michelin.cio.hudson.plugins.rolestrategy.*

def instance = Jenkins.getInstance()
def strategy = instance.getAuthorizationStrategy()

if (!(strategy instanceof RoleBasedAuthorizationStrategy)) {
    println "Role-Based Strategy is not enabled."
    return
}

// Helper to print roles and their assignments
def printRoles(roleMap, roleTypeName) {
    println "\n--- ${roleTypeName} Roles ---"
    def roles = roleMap.getRoles()
    roles.each { role ->
        println "Role: ${role.getName()}"
        println "  Permissions:"
        role.getPermissions().each {
            println "    - ${it.group.title} / ${it.name}"
        }
        def sids = roleMap.getSidsForRole(role.getName())
        println "  Assigned to:"
        sids.each { sid ->
            println "    - ${sid}"
        }
    }
}

// Get global roles
def globalRoleMap = strategy.getRoleMap(RoleBasedAuthorizationStrategy.GLOBAL)
printRoles(globalRoleMap, "Global")

// Get project roles
def projectRoleMap = strategy.getRoleMap(RoleBasedAuthorizationStrategy.PROJECT)
printRoles(projectRoleMap, "Project")

// Get agent (slave) roles
def agentRoleMap = strategy.getRoleMap(RoleBasedAuthorizationStrategy.SLAVE)
printRoles(agentRoleMap, "Agent")

// Optional: Folder roles (requires extra plugin support)
try {
    def folderRoleMap = strategy.getRoleMap(RoleBasedAuthorizationStrategy.ITEM)
    printRoles(folderRoleMap, "Folder/Item")
} catch (MissingPropertyException e) {
    println "\nFolder roles not supported or not found."
}
import com.cloudbees.opscenter.server.model.*
import hudson.plugins.git.*
import jenkins.model.*
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.domains.*
import com.cloudbees.plugins.credentials.impl.*
import com.cloudbees.plugins.credentials.CredentialsScope
import hudson.util.Secret
import hudson.plugins.git.GitSCM
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl
import com.cloudbees.hudson.plugins.folder.*
import org.jenkinsci.plugins.workflow.job.*
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition
import hudson.plugins.git.extensions.impl.*

// ======== 1. CREATE TEAM CONTROLLER ==========
def teamName = "my-team-controller"
def teamDisplayName = "My Team Controller"
def existing = Jenkins.instance.getItem(teamName)

if (existing == null) {
    println "Creating team controller: $teamName"
    def item = Jenkins.instance.createProject(ConnectedMaster.class, teamName)
    item.setDisplayName(teamDisplayName)
    item.save()
} else {
    println "Team controller already exists: $teamName"
}

def team = Jenkins.instance.getItem(teamName)

// ======== 2. CONFIGURE PROXY ================
def proxyHost = "proxy.example.com"
def proxyPort = 8080
def proxyUser = "proxyUser"
def proxyPassword = "proxyPassword"

def proxy = new hudson.ProxyConfiguration(proxyHost, proxyPort, proxyUser, proxyPassword, "")
Jenkins.instance.proxy = proxy
Jenkins.instance.save()
println "Proxy configured."

// ======== 3. INSTALL PLUGINS ================
def pluginList = ['git', 'workflow-aggregator', 'plain-credentials', 'blueocean']
def pluginManager = Jenkins.instance.pluginManager
def updateCenter = Jenkins.instance.updateCenter

pluginList.each { pluginId ->
    if (!pluginManager.getPlugin(pluginId)) {
        println "Installing plugin: $pluginId"
        def plugin = updateCenter.getPlugin(pluginId)
        if (plugin) {
            plugin.deploy()
        } else {
            println "Plugin not found in update center: $pluginId"
        }
    } else {
        println "Plugin already installed: $pluginId"
    }
}

// ======== 4. ADD CREDENTIALS ================
def credentialsList = [
    [
        id: "my-token",
        description: "GitHub Token",
        secret: "ghp_exampleToken",
    ]
]

def globalDomain = Domain.global()
def store = Jenkins.instance.getExtensionList('com.cloudbees.plugins.credentials.SystemCredentialsProvider')[0].getStore()

credentialsList.each { cred ->
    def c = new StringCredentialsImpl(
        CredentialsScope.GLOBAL,
        cred.id,
        cred.description,
        Secret.fromString(cred.secret)
    )
    store.addCredentials(globalDomain, c)
    println "Credential added: ${cred.id}"
}

// ======== 5. CREATE PIPELINE JOB ============
def jobName = "example-pipeline"
def pipelineScript = """
pipeline {
    agent any
    stages {
        stage('Hello') {
            steps {
                echo 'Hello from ${teamName}'
            }
        }
    }
}
"""

def job = team.getItem(jobName)
if (job == null) {
    println "Creating pipeline job: $jobName"
    def pipelineJob = new WorkflowJob(team, jobName)
    pipelineJob.definition = new CpsFlowDefinition(pipelineScript, true)
    team.add(pipelineJob, jobName)
    pipelineJob.save()
} else {
    println "Pipeline job already exists: $jobName"
}


-----


import com.cloudbees.opscenter.server.model.ManagedMaster
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition

def controllerName = "NOM_DU_CLIENT_CONTROLLER" // change ça

def mm = Jenkins.instance.getAllItems(ManagedMaster).find { it.name == controllerName }

if (mm == null) {
    throw new Exception("Client controller '${controllerName}' non trouvé")
}

def jenkins = mm.getOwner().getJenkins()
def jobName = "mon-job-auto"

if (jenkins.getItem(jobName) != null) {
    println "Job '${jobName}' existe déjà dans le client controller '${controllerName}'"
    return
}

def pipelineScript = '''
pipeline {
    agent any
    stages {
        stage('Start') {
            steps {
                echo 'Job créé depuis CJOC'
            }
        }
    }
}
'''

def job = new WorkflowJob(jenkins, jobName)
job.setDefinition(new CpsFlowDefinition(pipelineScript, true))
jenkins.add(job, jobName)

println "✅ Job '${jobName}' créé avec succès dans '${controllerName}'"



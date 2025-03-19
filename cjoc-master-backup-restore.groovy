pipeline {
    agent { label 'operations-center' }
    parameters {
        string(name: 'MASTER_NAME', defaultValue: 'client-controller-1', description: 'Client Controller Name')
    }
    environment {
        BACKUP_DIR = "/var/jenkins_home"
        BACKUP_FILE = "backup-${params.MASTER_NAME}-$(date +%F-%H-%M-%S).tar.gz"
        TEMP_PATH = "/tmp/\$BACKUP_FILE"
        NEXUS_URL = "http://nexus.example.com/repository/backups/"
        NEXUS_CREDENTIALS_ID = "nexus-cred" // Jenkins credential ID for Nexus
    }
    stages {
        stage('Create Backup on Client Controller') {
            steps {
                script {
                    echo "Executing backup on client controller: ${params.MASTER_NAME}"
                    def backupCommand = """
                        tar -czf \$TEMP_PATH -C $BACKUP_DIR .
                        echo "Backup created: \$TEMP_PATH"
                    """
                    jenkins.model.Jenkins.instance.getItemByFullName("${params.MASTER_NAME}")
                        .getComputer().getChannel().exec(backupCommand)
                }
            }
        }
        stage('Upload Backup to Nexus') {
            steps {
                script {
                    echo "Uploading backup to Nexus..."
                    def uploadCommand = """
                        curl -u \$(echo \${NEXUS_CREDENTIALS_ID} | tr ':' ' ') --upload-file \$TEMP_PATH \$NEXUS_URL\$BACKUP_FILE
                        echo "Backup uploaded successfully!"
                    """
                    jenkins.model.Jenkins.instance.getItemByFullName("${params.MASTER_NAME}")
                        .getComputer().getChannel().exec(uploadCommand)
                }
            }
        }
    }
}

/////////////////////////////////////////////////////////////////////////////////////////////////////

pipeline {
    agent { label 'operations-center' }
    parameters {
        string(name: 'MASTER_NAME', defaultValue: 'client-controller-1', description: 'Client Controller Name')
    }
    environment {
        BACKUP_DIR = "/var/jenkins_home"
        NEXUS_URL = "http://nexus.example.com/repository/backups/"
        BACKUP_FILE = "backup-${params.MASTER_NAME}-latest.tar.gz"
        TEMP_PATH = "/tmp/\$BACKUP_FILE"
        NEXUS_CREDENTIALS_ID = "nexus-cred" // Jenkins credential ID for Nexus
    }
    stages {
        stage('Download Backup from Nexus') {
            steps {
                script {
                    echo "Downloading backup from Nexus for ${params.MASTER_NAME}"
                    def downloadCommand = """
                        curl -u \$(echo \${NEXUS_CREDENTIALS_ID} | tr ':' ' ') -o \$TEMP_PATH \$NEXUS_URL\$BACKUP_FILE
                        echo "Backup downloaded: \$TEMP_PATH"
                    """
                    jenkins.model.Jenkins.instance.getItemByFullName("${params.MASTER_NAME}")
                        .getComputer().getChannel().exec(downloadCommand)
                }
            }
        }
        stage('Restore Backup on Client Controller') {
            steps {
                script {
                    echo "Restoring backup on ${params.MASTER_NAME}"
                    def restoreCommand = """
                        systemctl stop jenkins  # Stop Jenkins before restoring
                        tar -xzf \$TEMP_PATH -C $BACKUP_DIR
                        systemctl start jenkins  # Restart Jenkins
                        echo "Restore complete!"
                    """
                    jenkins.model.Jenkins.instance.getItemByFullName("${params.MASTER_NAME}")
                        .getComputer().getChannel().exec(restoreCommand)
                }
            }
        }
    }
}


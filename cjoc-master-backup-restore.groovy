pipeline {
    agent { label 'operations-center' }
    parameters {
        string(name: 'MASTER_NAME', defaultValue: 'client-controller-1', description: 'Client Controller Name')
        string(name: 'NAMESPACE', defaultValue: 'jenkins', description: 'Kubernetes Namespace of the Master')
    }
    environment {
        BACKUP_DIR = "/var/jenkins_home"
        BACKUP_FILE = "backup-${params.MASTER_NAME}-$(date +%F-%H-%M-%S).tar.gz"
        TEMP_PATH = "/tmp/\$BACKUP_FILE"
        NEXUS_URL = "http://nexus.example.com/repository/backups/"
        NEXUS_CREDENTIALS_ID = "nexus-cred" // Jenkins credential ID
    }
    stages {
        stage('Identify Master Pod') {
            steps {
                script {
                    echo "Finding pod for ${params.MASTER_NAME}..."
                    def podName = sh(script: "kubectl get pods -n ${params.NAMESPACE} -l app=${params.MASTER_NAME} -o jsonpath='{.items[0].metadata.name}'", returnStdout: true).trim()
                    if (!podName) {
                        error("No pod found for ${params.MASTER_NAME} in namespace ${params.NAMESPACE}")
                    }
                    env.MASTER_POD = podName
                    echo "Master pod found: ${env.MASTER_POD}"
                }
            }
        }
        stage('Create Backup') {
            steps {
                script {
                    echo "Creating backup inside pod ${env.MASTER_POD}..."
                    sh """
                        kubectl exec -n ${params.NAMESPACE} ${env.MASTER_POD} -- sh -c \\
                        'echo "Stopping Jenkins..."; 
                        if supervisorctl status jenkins; then supervisorctl stop jenkins; fi;
                        echo "Creating backup archive...";
                        tar -czf ${TEMP_PATH} -C ${BACKUP_DIR} .;
                        echo "Starting Jenkins...";
                        supervisorctl start jenkins;
                        echo "Backup created: ${TEMP_PATH}"'
                    """
                }
            }
        }
        stage('Upload Backup to Nexus') {
            steps {
                script {
                    echo "Uploading backup to Nexus..."
                    sh """
                        kubectl exec -n ${params.NAMESPACE} ${env.MASTER_POD} -- sh -c \\
                        'curl -u \$(echo \${NEXUS_CREDENTIALS_ID} | tr ":" " ") --upload-file ${TEMP_PATH} ${NEXUS_URL}${BACKUP_FILE};
                        echo "Backup uploaded successfully!"'
                    """
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
        string(name: 'NAMESPACE', defaultValue: 'jenkins', description: 'Kubernetes Namespace of the Master')
    }
    environment {
        BACKUP_DIR = "/var/jenkins_home"
        BACKUP_FILE = "backup-${params.MASTER_NAME}-latest.tar.gz"
        TEMP_PATH = "/tmp/\$BACKUP_FILE"
        NEXUS_URL = "http://nexus.example.com/repository/backups/"
        NEXUS_CREDENTIALS_ID = "nexus-cred" // Jenkins credential ID
    }
    stages {
        stage('Identify Master Pod') {
            steps {
                script {
                    echo "Finding pod for ${params.MASTER_NAME}..."
                    def podName = sh(script: "kubectl get pods -n ${params.NAMESPACE} -l app=${params.MASTER_NAME} -o jsonpath='{.items[0].metadata.name}'", returnStdout: true).trim()
                    if (!podName) {
                        error("No pod found for ${params.MASTER_NAME} in namespace ${params.NAMESPACE}")
                    }
                    env.MASTER_POD = podName
                    echo "Master pod found: ${env.MASTER_POD}"
                }
            }
        }
        stage('Download Backup from Nexus') {
            steps {
                script {
                    echo "Downloading backup inside pod ${env.MASTER_POD}..."
                    sh """
                        kubectl exec -n ${params.NAMESPACE} ${env.MASTER_POD} -- sh -c \\
                        'curl -u \$(echo \${NEXUS_CREDENTIALS_ID} | tr ":" " ") -o ${TEMP_PATH} ${NEXUS_URL}${BACKUP_FILE}'
                    """
                }
            }
        }
        stage('Restore Backup') {
            steps {
                script {
                    echo "Restoring backup on ${params.MASTER_NAME}..."
                    sh """
                        kubectl exec -n ${params.NAMESPACE} ${env.MASTER_POD} -- sh -c \\
                        'if [ -f ${TEMP_PATH} ]; then
                            echo "Stopping Jenkins...";
                            if supervisorctl status jenkins; then supervisorctl stop jenkins; fi;
                            echo "Extracting backup...";
                            tar -xzf ${TEMP_PATH} -C ${BACKUP_DIR};
                            echo "Starting Jenkins...";
                            supervisorctl start jenkins;
                            echo "Restore complete!";
                        else
                            echo "Backup file not found!";
                            exit 1;
                        fi'
                    """
                }
            }
        }
    }
}

pipeline {
    agent {
        kubernetes {
            label 'molgenis'
        }
    }
    stages {
        stage('Prepare') {
            steps {
                script {
                    env.GIT_COMMIT = sh(script: 'git rev-parse HEAD', returnStdout: true).trim()
                }
                container('vault') {
                    script {
                        sh "mkdir /home/jenkins/.m2"
                        sh(script: 'vault read -field=value secret/ops/jenkins/maven/settings.xml > /home/jenkins/.m2/settings.xml')
                        env.GITHUB_TOKEN = sh(script: 'vault read -field=value secret/ops/token/github', returnStdout: true)
                        env.CODECOV_TOKEN = sh(script: 'vault read -field=data-transform-vkgl secret/ops/token/codecov', returnStdout: true)
                        env.SONAR_TOKEN = sh(script: 'vault read -field=value secret/ops/token/sonar', returnStdout: true)
                        env.GITHUB_USER = sh(script: 'vault read -field=username secret/ops/token/github', returnStdout: true)
                    }
                }
            }
        }
        stage('Build: [ pull request ]') {
            when {
                changeRequest()
            }
            steps {
                container('maven') {
                    sh "mvn clean install"
                }
            }
            post {
                always {
                    container('maven') {
                        sh "curl -s https://codecov.io/bash | bash -s - -c -F unit -K -C ${GIT_COMMIT}"
                        sh "mvn -q -B sonar:sonar -Dsonar.login=${env.SONAR_TOKEN} -Dsonar.github.oauth=${env.GITHUB_TOKEN} -Dsonar.pullrequest.base=${CHANGE_TARGET} -Dsonar.pullrequest.branch=${BRANCH_NAME} -Dsonar.pullrequest.key=${env.CHANGE_ID} -Dsonar.pullrequest.provider=GitHub -Dsonar.pullrequest.github.repository=molgenis/data-transform-vkgl -Dsonar.ws.timeout=120"
                    }
                }
            }
        }
        stage('Build: [ master ]') {
            when {
                branch 'master'
            }
            steps {
                milestone 1
                container('maven') {
                    sh "mvn clean install"
                }
            }
            post {
                always {
                    container('maven') {
                        sh "curl -s https://codecov.io/bash | bash -s - -c -F unit -K -C ${GIT_COMMIT}"
                        sh "mvn -q -B sonar:sonar -Dsonar.login=${SONAR_TOKEN} -Dsonar.ws.timeout=120"
                    }
                }
            }
        }
    }
}

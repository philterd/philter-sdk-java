pipeline {
    agent any
    triggers {
        pollSCM 'H/10 * * * *'
    }
    tools {
        maven 'maven-3.6.0'
        jdk 'jdk8u192'
    }
    options {
        buildDiscarder(logRotator(numToKeepStr: '3'))
    }
    parameters {
        booleanParam(defaultValue: false, description: 'Release', name: 'isRelease')
    }
    stages {
        stage ('Initialize') {
            steps {
                sh '''
                    echo "PATH = ${PATH}"
                    echo "M2_HOME = ${M2_HOME}"
                '''
            }
        }
        stage ('Build') {
            steps {
                sh "mvn -p ./java/pom.xml clean install deploy"
            }
        }
        stage ('Release') {
            when {
                expression {
                    if (env.ISRELEASE == "true") {
                        return true
                    }
                    return false
                }
            }
            steps {
                sh "mvn -p ./java/pom.xml --batch-mode release:clean release:prepare release:perform"
            }
        }
    }
}

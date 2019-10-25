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
    stages {
        stage ('Initialize') {
            steps {
                sh '''
                    echo "PATH = ${PATH}"
                    echo "M2_HOME = ${M2_HOME}"
                '''
            }
        }
        stage ('Java Build and Release') {
            steps {
                sh "mvn -f ./java/pom.xml clean install"
                //sh "mvn -f ./java/pom.xml --batch-mode release:clean release:prepare release:perform"                
            }
        }
        stage ('.Net Core Build') {
            steps {
                sh "dotnet restore ./dotnetcore"
                sh "dotnet build ./dotnetcore -c Release"
            }
        }
    }
}

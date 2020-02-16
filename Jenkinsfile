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
        booleanParam(name: 'JAVA', defaultValue: true, description: 'Build Java library')
        booleanParam(name: 'DOTNET', defaultValue: false, description: 'Build .NET library')
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
            when {
                expression {
                    if (env.JAVA == "true") {
                        return true
                    }
                    return false
                }
            }           
            steps {
                sh "mvn -f ./java/pom.xml clean install deploy"
                //sh "mvn -f ./java/pom.xml --batch-mode release:clean release:prepare release:perform"                
            }
        }
        stage ('.Net Core Build') {
            when {
                expression {
                    if (env.DOTNET == "true") {
                        return true
                    }
                    return false
                }
            }   
            steps {
                sh "dotnet restore ./dotnetcore"
                sh "dotnet build ./dotnetcore -c Release"
            }
        }
    }
}

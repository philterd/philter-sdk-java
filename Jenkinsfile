pipeline {
  agent any
  triggers {
    pollSCM 'H/10 * * * *'
  }
  tools {
    maven 'maven-3.6.3'
    jdk 'java-1.11.0-openjdk-amd64'
  }
  options {
    buildDiscarder(logRotator(numToKeepStr: '3'))
  }
  environment {
    VERSION = readMavenPom().getVersion()
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
        sh "mvn clean install deploy javadoc:javadoc site"
        sh "aws s3 sync ./target/site/ s3://mtnfog-public/philter-sdk/java/${VERSION}/ --delete"
      }
    }
  }
}

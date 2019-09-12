#!/usr/bin/env groovy

def devBranchName = 'dev'

def triggers = []

if (env.BRANCH_NAME == devBranchName) {
    triggers << cron('H H(0-2) * * *')
}

properties([
        [$class: 'BuildDiscarderProperty', strategy: [$class: 'LogRotator', numToKeepStr: '20']],
        [$class: 'RebuildSettings', autoRebuild: false, rebuildDisabled: false],
        pipelineTriggers(triggers)
])

node('Android-Docker') {
    withCredentials([usernamePassword(credentialsId: '3933d8b7-19d9-41ba-a4ae-4abe7708d06a', passwordVariable: 'NEXUS_PASSWORD', usernameVariable: 'NEXUS_USERNAME')]) {

        stage('SCM') {
            checkout scm
        }

        stage('Build') {
            sh "./gradlew -PartifactsMobileUsername=${NEXUS_USERNAME} -PartifactsMobilePassword=${NEXUS_PASSWORD} clean lintProdDebug testProdDebugUnitTest appDebug"
        }

        if (isTriggeredManually(currentBuild)) {
            stage('Upload manual build to Artifacts Mobile') {
                uploadCurrentBuild(false)
            }
        }

        if (env.BRANCH_NAME == devBranchName) {
            stage('Upload results to SonarQube') {

                def scannerHome = tool name: 'SonarQubeScanner', type: 'hudson.plugins.sonar.SonarRunnerInstallation'
                withSonarQubeEnv('EU SonarQube') {
                    sh "./gradlew lintProdDebug testProdDebugUnitTest"
                    sh "${scannerHome}/bin/sonar-scanner"
                }
            }

            //ToDo: add once the Fortify addon is instaled on
            /*stage('Fortify Analysis'){
                mkdir Fortify

                buildName="Lahaina_Android"
                sourceanalyzer -b "$buildName" -clean
                sourceanalyzer -b "$buildName" -cp ${APK_NAME} /Scout/src/main/java/ -jdk 1.8
                sourceanalyzer -b "$buildName" -cp ScoutProject/Scout/temp/classpath
            }*/
        }

        if (isTriggeredByTimer(currentBuild)) {
            stage('Build & Publish nightly-build artifacts') {
                uploadCurrentBuild(true)
            }
        }
    }
}

def uploadCurrentBuild(boolean isBuildByTimer) {
    dir('uploader') {
        checkout([$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'de4e817c-f7a3-4241-a17a-096b4bc1cb04', url: 'https://bitbucket.org/telenavincorporated/automationeu-build-nexusuploader']]])
        sh 'chmod +x uploadMobileBuildNexus.sh'
    }
    def printableBranchName = env.BRANCH_NAME.replace("/", "_")
    if (isBuildByTimer) {
        printableBranchName = "nightly_" + printableBranchName
    }

    def buildName = "${WORKSPACE}/osc_${printableBranchName}.apk"
    sh "mv ${WORKSPACE}/app/build/outputs/apk/prod/debug/app-prod-debug.apk ${buildName}"
    sh "echo \"${buildName}\" > upload.dat"
    sh "./uploader/uploadMobileBuildNexus.sh -s Android -p OpenStreetCam -v 2.7.0 -f upload.dat"
}

@NonCPS
def isTriggeredByTimer(currentBuild) {
    try {
        return currentBuild.rawBuild.getCause(hudson.triggers.TimerTrigger$TimerTriggerCause) != null
    } catch (error) {
        echo "${error}"
    }
    return false
}

@NonCPS
def isTriggeredManually(currentBuild) {
    try {
        boolean manualBuild = currentBuild.rawBuild.getCause(hudson.model.Cause$UserIdCause) != null;
        echo "Manual build check status: ${manualBuild}"
        return manualBuild
    } catch (error) {
        echo "${error}"
    }
    return false
}
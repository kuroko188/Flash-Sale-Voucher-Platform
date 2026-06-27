#!groovy
import jenkins.model.*
import jenkins.install.InstallState

def instance = Jenkins.getInstance()
if (instance.getInstallState() != InstallState.INITIAL_SETUP_COMPLETED) {
    println "Skipping Jenkins setup wizard"
    instance.setInstallState(InstallState.INITIAL_SETUP_COMPLETED)
    instance.save()
}

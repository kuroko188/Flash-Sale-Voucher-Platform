#!groovy
import jenkins.model.*
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition
import hudson.plugins.git.GitSCM
import hudson.plugins.git.BranchSpec
import hudson.plugins.git.UserRemoteConfig

def jobName = "flash-sale-voucher-platform"
def gitUrl = "https://github.com/kuroko188/Flash-Sale-Voucher-Platform.git"
def branch = "*/main"
def jenkinsfile = "Jenkinsfile"

def instance = Jenkins.getInstance()
if (instance.getItem(jobName) != null) {
    println "Job already exists: ${jobName}"
    return
}

def job = instance.createProject(WorkflowJob.class, jobName)
def scm = new GitSCM(gitUrl)
scm.setBranches([new BranchSpec(branch)])
def flow = new CpsScmFlowDefinition(scm, jenkinsfile)
job.setDefinition(flow)
job.save()
println "Created pipeline job: ${jobName}"

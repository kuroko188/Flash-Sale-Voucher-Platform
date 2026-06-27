#!groovy
import jenkins.model.*
import hudson.security.*
import hudson.model.User

def jenkins = Jenkins.getInstance()
def realm = new HudsonPrivateSecurityRealm(false)

def existing = User.getById("admin", false)
if (existing != null) {
    User.getAll().findAll { it.id == "admin" }.each { it.delete() }
}

realm.createAccount("admin", "admin123")
jenkins.setSecurityRealm(realm)
jenkins.setAuthorizationStrategy(new FullControlOnceLoggedInAuthorizationStrategy())
jenkins.save()
println "Jenkins admin password reset to admin123"

#!groovy
import jenkins.model.*
import hudson.security.*

def instance = Jenkins.getInstance()
def hudsonRealm = new HudsonPrivateSecurityRealm(false)
if (instance.getSecurityRealm() instanceof HudsonPrivateSecurityRealm) {
    println "Admin user already configured"
    return
}
hudsonRealm.createAccount("admin", "admin123")
instance.setSecurityRealm(hudsonRealm)
instance.setAuthorizationStrategy(new FullControlOnceLoggedInAuthorizationStrategy())
instance.save()
println "Created admin user: admin / admin123"

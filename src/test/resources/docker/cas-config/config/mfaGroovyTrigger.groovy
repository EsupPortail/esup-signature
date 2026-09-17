import java.util.*

class SampleGroovyEventResolver {

    def String run(service, registeredService, authentication, httpRequest, logger, ... args) {
        def ctx = org.apereo.cas.util.spring.ApplicationContextProvider.getApplicationContext()
        def esupOtpService = ctx.getBean("esupOtpService")
        def esupOtpUserInfos = esupOtpService.getUserInfos(authentication.principal.id)
        def esupOtpMethods = esupOtpUserInfos.get("user").get("methods").toMap().values()
	def oneMfaMethodIsActive = esupOtpUserInfos.get("user").getBoolean("has_enabled_method")
        logger.warn("User ${authentication.principal.id} has the following MFA methods: ${esupOtpMethods}")
        logger.warn(oneMfaMethodIsActive.toString())
        return oneMfaMethodIsActive ? "mfa-esupotp" : null
    }
}


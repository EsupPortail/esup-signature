package org.esupportail.esupsignature.annotation;

import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.entity.Message;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.service.UserService;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.validation.support.BindingAwareModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;

import javax.annotation.Resource;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Aspect
@Component
public class SetGlobalAttributsAspect {

    @Resource
    private GlobalProperties globalProperties;

    @Resource
    UserService userService;

    @Around("@within(setGlobalAttributs)")
    public Object getActiveMenu(ProceedingJoinPoint proceedingJoinPoint, SetGlobalAttributs setGlobalAttributs) throws Throwable {
        User user = userService.getCurrentUser();
        User authUser = userService.getUserFromAuthentication();
        Object[] modifiedArgs = proceedingJoinPoint.getArgs();
        int index = 0;
        int userCount = 0;
        for(Object arg : proceedingJoinPoint.getArgs()) {
            if (arg instanceof User) {
                if(userCount == 0) {
                    modifiedArgs[index] = user;
                    userCount++;
                } else {
                    modifiedArgs[index] = authUser;
                }
            }
            if (arg instanceof BindingAwareModelMap) {
                BindingAwareModelMap bindingAwareModelMap = (BindingAwareModelMap) arg;
                bindingAwareModelMap.put("user", user);
                bindingAwareModelMap.put("authUser", authUser);
                bindingAwareModelMap.put("suUsers", userService.getSuUsers(authUser));
                bindingAwareModelMap.put("globalProperties", this.globalProperties);
                bindingAwareModelMap.put("messageNews", userService.getMessages(authUser));
                bindingAwareModelMap.put("activeMenu", setGlobalAttributs.activeMenu());
                modifiedArgs[index] = bindingAwareModelMap;
            }
            index++;
        }
        return proceedingJoinPoint.proceed(modifiedArgs);
    }

}

package org.esupportail.esupsignature.annotation;

import org.esupportail.esupsignature.entity.User;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface SetGlobalAttributs {
    String activeMenu() default "";
}

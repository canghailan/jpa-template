package cc.whohow.jpa.template;


import org.springframework.data.annotation.QueryAnnotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@QueryAnnotation
@Documented
public @interface TemplateQuery {
    String value() default "";

    String countQuery() default "";

    boolean nativeQuery() default true;

    Class<?> resultClass() default Object.class;
}

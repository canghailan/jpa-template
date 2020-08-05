package cc.whohow.jpa.template;

import cc.whohow.jpa.template.directive.*;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.RuntimeSingleton;
import org.apache.velocity.runtime.parser.ParseException;
import org.springframework.data.jpa.repository.query.AbstractJpaQuery;
import org.springframework.data.jpa.repository.query.JpaParameters;
import org.springframework.data.jpa.repository.query.JpaParametersParameterAccessor;
import org.springframework.data.jpa.repository.query.JpaQueryMethod;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Parameter;
import javax.persistence.Query;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class JpaTemplateQuery extends AbstractJpaQuery {
    static {
        RuntimeSingleton.loadDirective(Ifnull.class.getName());
        RuntimeSingleton.loadDirective(Ifnotnull.class.getName());
        RuntimeSingleton.loadDirective(Ifempty.class.getName());
        RuntimeSingleton.loadDirective(Ifnotempty.class.getName());
        RuntimeSingleton.loadDirective(TemplateQueryParameter.class.getName());
        RuntimeSingleton.loadDirective(TemplateQueryWhere.class.getName());
    }

    /**
     * TemplateQuery解析工具
     */
    private final TemplateQueryResolver templateQueryResolver;
    /**
     * 查询模版缓存
     */
    private final Template queryTemplate;
    /**
     * 查询结果类型
     */
    private final Class<?> entityClass;
    /**
     * 查询构造工厂
     */
    private final Function<String, Query> queryFactory;

    public JpaTemplateQuery(JpaQueryMethod method,
                            EntityManager em,
                            TemplateQueryResolver templateQueryResolver) {
        super(method, em);
        this.templateQueryResolver = templateQueryResolver;
        this.queryTemplate = getTemplate(templateQueryResolver.getQuery());
        this.entityClass = detectEntityClass();
        if (isNativeQuery()) {
            if (entityClass == null) {
                queryFactory = this::createNativeQuery;
            } else {
                queryFactory = this::createEntityNativeQuery;
            }
        } else {
            if (entityClass == null) {
                queryFactory = this::createQuery;
            } else {
                queryFactory = this::createEntityQuery;
            }
        }
    }

    @Override
    protected Query doCreateQuery(JpaParametersParameterAccessor jpaParametersParameterAccessor) {
        Object[] values = jpaParametersParameterAccessor.getValues();
        VelocityContext context = new VelocityContext(getTemplateContext(values));

        Writer buffer = new TemplateQueryWriter();
        queryTemplate.merge(context, buffer);
        Query query = queryFactory.apply(buffer.toString());

        for (Parameter<?> parameter : query.getParameters()) {
            if (parameter.getPosition() != null) {
                query.setParameter(parameter.getPosition(), values[parameter.getPosition()]);
            } else {
                query.setParameter(parameter.getName(), context.get(parameter.getName()));
            }
        }

        return query;
    }

    @Override
    protected Query doCreateCountQuery(JpaParametersParameterAccessor jpaParametersParameterAccessor) {
        throw new UnsupportedOperationException("countQuery");
    }

    protected boolean isNativeQuery() {
        return templateQueryResolver.isNativeQuery();
    }

    protected Class<?> detectResultClass() {
        if (templateQueryResolver.getResultClass() != Object.class) {
            return templateQueryResolver.getResultClass();
        }
        return getQueryMethod().getReturnedObjectType();
    }

    protected Class<?> detectEntityClass() {
        Class<?> resultClass = detectResultClass();
        if (resultClass.isAnnotationPresent(Entity.class)) {
            return resultClass;
        }
        return null;
    }

    protected Query createQuery(String sql) {
        return getEntityManager().createQuery(sql);
    }

    protected Query createEntityQuery(String sql) {
        return getEntityManager().createQuery(sql, entityClass);
    }

    protected Query createNativeQuery(String sql) {
        return getEntityManager().createNativeQuery(sql);
    }

    protected Query createEntityNativeQuery(String sql) {
        return getEntityManager().createNativeQuery(sql, entityClass);
    }

    protected Template getTemplate(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        try {
            RuntimeServices runtimeServices = RuntimeSingleton.getRuntimeServices();
            Template template = new Template();
            template.setRuntimeServices(runtimeServices);
            template.setData(runtimeServices.parse(text, getQueryMethod().getName() + "." + hash(text)));
            template.initDocument();
            return template;
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    protected String hash(String text) {
        try {
            return Base64.getEncoder().encodeToString(
                    MessageDigest.getInstance("MD5").digest(
                            text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    protected Map<String, Object> getTemplateContext(Object[] values) {
        JpaParameters parameters = getQueryMethod().getParameters();
        Map<String, Object> context = new HashMap<>();
        for (int i = 0; i < parameters.getNumberOfParameters(); i++) {
            org.springframework.data.repository.query.Parameter parameter = parameters.getParameter(i);
            Optional<String> name = parameter.getName();
            if (name.isPresent()) {
                context.put(name.get(), values[i]);
            }
        }
        return context;
    }
}

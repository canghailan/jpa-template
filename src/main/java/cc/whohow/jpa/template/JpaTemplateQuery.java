package cc.whohow.jpa.template;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.RuntimeSingleton;
import org.apache.velocity.runtime.parser.ParseException;
import org.springframework.data.jpa.repository.query.AbstractJpaQuery;
import org.springframework.data.jpa.repository.query.JpaParameters;
import org.springframework.data.jpa.repository.query.JpaQueryMethod;
import org.springframework.data.jpa.repository.query.QueryUtils;

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

public class JpaTemplateQuery extends AbstractJpaQuery {
    static {
        RuntimeSingleton.loadDirective(TemplateQueryParameter.class.getName());
        RuntimeSingleton.loadDirective(TemplateQueryWhere.class.getName());
    }

    private TemplateQuery templateQuery;
    private Template query;
    private Template countQuery;
    private Class<?> entityClass;

    public JpaTemplateQuery(JpaQueryMethod method, EntityManager em, TemplateQuery templateQuery) {
        super(method, em);
        this.templateQuery = templateQuery;
        this.query = getTemplate(templateQuery.value());
        this.countQuery = getTemplate(templateQuery.countQuery());
        this.entityClass = detectEntityClass();
    }

    @Override
    protected Query doCreateQuery(Object[] values) {
        return doCreate(query, values, entityClass);
    }

    @Override
    protected Query doCreateCountQuery(Object[] values) {
        return doCreate(getCountQueryTemplate(), values, null);
    }

    protected Query doCreate(Template queryTemplate, Object[] values, Class<?> resultClass) {
        VelocityContext context = new VelocityContext(getContext(values));

        Writer buffer = new TemplateQueryWriter();
        queryTemplate.merge(context, buffer);
        String sql = buffer.toString();

        Query query = isNativeQuery() ?
                (resultClass == null) ?
                        getEntityManager().createNativeQuery(sql) :
                        getEntityManager().createNativeQuery(sql, resultClass) :
                (resultClass == null) ?
                        getEntityManager().createQuery(sql) :
                        getEntityManager().createQuery(sql, resultClass);

        for (Parameter<?> parameter : query.getParameters()) {
            if (parameter.getPosition() != null) {
                query.setParameter(parameter.getPosition(), values[parameter.getPosition()]);
            } else {
                query.setParameter(parameter.getName(), context.get(parameter.getName()));
            }
        }

        return query;
    }

    protected boolean isNativeQuery() {
        return templateQuery.nativeQuery();
    }

    protected Template getCountQueryTemplate() {
        if (countQuery == null) {
            countQuery = getTemplate(QueryUtils.createCountQueryFor(templateQuery.value()));
        }
        return countQuery;
    }

    protected Class<?> detectResultClass() {
        if (templateQuery.resultClass() != Object.class) {
            return templateQuery.resultClass();
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

    protected Template getTemplate(String text) {
        if (text.isEmpty()) {
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

    protected Map<String, Object> getContext(Object[] values) {
        JpaParameters parameters = getQueryMethod().getParameters();
        Map<String, Object> context = new HashMap<>();
        for (int i = 0; i < parameters.getNumberOfParameters(); i++) {
            org.springframework.data.repository.query.Parameter parameter = parameters.getParameter(i);
            String name = parameter.getName();
            if (name != null) {
                context.put(name, values[i]);
            }
        }
        return context;
    }
}

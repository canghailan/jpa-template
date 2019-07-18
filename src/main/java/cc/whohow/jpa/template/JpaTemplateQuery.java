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
import java.util.Optional;

public class JpaTemplateQuery extends AbstractJpaQuery {
    static {
        RuntimeSingleton.loadDirective(TemplateQueryParameter.class.getName());
        RuntimeSingleton.loadDirective(TemplateQueryWhere.class.getName());
    }

    private final TemplateQuery templateQuery;
    private Template query;
    private Template countQuery;
    private Class<?> resultClass;
    private boolean isEntityResultClass;

    public JpaTemplateQuery(JpaQueryMethod method, EntityManager em, TemplateQuery templateQuery) {
        super(method, em);
        this.templateQuery = templateQuery;
        this.query = getTemplate(templateQuery.value());
        this.resultClass = detectResultClass();
        this.isEntityResultClass = isEntityResultClass(resultClass);
    }

    protected Class<?> detectResultClass() {
        if (templateQuery.resultClass() != Object.class) {
            return templateQuery.resultClass();
        }
        return getQueryMethod().getReturnedObjectType();
    }

    protected boolean isEntityResultClass(Class<?> resultClass) {
        return resultClass != null && resultClass.isAnnotationPresent(Entity.class);
    }

    @Override
    protected Query doCreateQuery(Object[] values) {
        return doCreate(query, values, isEntityResultClass, resultClass);
    }

    @Override
    protected Query doCreateCountQuery(Object[] values) {
        return doCreate(getCountQueryTemplate(), values, false, Number.class);
    }

    protected Template getCountQueryTemplate() {
        if (countQuery == null) {
            if (templateQuery.countQuery().isEmpty()) {
                countQuery = getTemplate(QueryUtils.createCountQueryFor(templateQuery.value()));
            } else {
                countQuery = getTemplate(templateQuery.countQuery());
            }
        }
        return countQuery;
    }

    protected Query doCreate(Template template, Object[] values,
                             boolean usingResultClass, Class<?> resultClass) {
        VelocityContext context = new VelocityContext(getContext(values));

        Writer buffer = new TemplateQueryWriter();
        template.merge(context, buffer);
        String sql = buffer.toString();

        Query query = isNativeQuery() ?
                usingResultClass ?
                        getEntityManager().createNativeQuery(sql, resultClass) :
                        getEntityManager().createNativeQuery(sql) :
                usingResultClass ?
                        getEntityManager().createQuery(sql, resultClass) :
                        getEntityManager().createQuery(sql);

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

    private Map<String, Object> getContext(Object[] values) {
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

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
import java.util.function.Function;

public class JpaTemplateQuery extends AbstractJpaQuery {
    static {
        RuntimeSingleton.loadDirective(Ifnull.class.getName());
        RuntimeSingleton.loadDirective(Ifnotnull.class.getName());
        RuntimeSingleton.loadDirective(TemplateQueryParameter.class.getName());
        RuntimeSingleton.loadDirective(TemplateQueryWhere.class.getName());
    }

    private TemplateQuery templateQuery;
    private Function<String, String> queryResolver;
    private Template query;
    private Template countQuery;
    private Class<?> entityClass;

    public JpaTemplateQuery(JpaQueryMethod method, EntityManager em,
                            TemplateQuery templateQuery,
                            Function<String, String> queryResolver) {
        super(method, em);
        this.templateQuery = templateQuery;
        this.queryResolver = queryResolver;
        this.query = getTemplate(resolveQuery(templateQuery.value(), templateQuery.name()));
        this.entityClass = detectEntityClass();
    }

    @Override
    protected Query doCreateQuery(Object[] values) {
        VelocityContext context = new VelocityContext(getContext(values));

        Writer buffer = new TemplateQueryWriter();
        query.merge(context, buffer);
        String sql = buffer.toString();

        Query query = isNativeQuery() ?
                (entityClass == null) ?
                        getEntityManager().createNativeQuery(sql) :
                        getEntityManager().createNativeQuery(sql, entityClass) :
                (entityClass == null) ?
                        getEntityManager().createQuery(sql) :
                        getEntityManager().createQuery(sql, entityClass);

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
    protected Query doCreateCountQuery(Object[] values) {
        VelocityContext context = new VelocityContext(getContext(values));

        Writer buffer = new TemplateQueryWriter();
        query.merge(context, buffer);
        String sql = QueryUtils.createCountQueryFor(buffer.toString());

        Query query = isNativeQuery() ?
                getEntityManager().createNativeQuery(sql) :
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

    protected String resolveQuery(String query, String name) {
        return query.isEmpty() ? queryResolver.apply(name) : query;
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
            Optional<String> name = parameter.getName();
            if (name.isPresent()) {
                context.put(name.get(), values[i]);
            }
        }
        return context;
    }
}

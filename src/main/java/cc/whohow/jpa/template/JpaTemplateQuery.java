package cc.whohow.jpa.template;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.RuntimeSingleton;
import org.apache.velocity.runtime.parser.ParseException;
import org.springframework.data.jpa.repository.query.AbstractJpaQuery;
import org.springframework.data.jpa.repository.query.JpaParameters;
import org.springframework.data.jpa.repository.query.JpaQueryMethod;
import org.springframework.data.repository.query.ReturnedType;

import javax.persistence.EntityManager;
import javax.persistence.Parameter;
import javax.persistence.Query;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class JpaTemplateQuery extends AbstractJpaQuery {
    static {
        RuntimeSingleton.loadDirective("com.yitong.jpa.TemplateQueryParameter");
        RuntimeSingleton.loadDirective("com.yitong.jpa.TemplateQueryWhere");
    }

    private final TemplateQuery templateQuery;
    private Template query;
    private Template countQuery;
    private Class<?> resultClass;

    public JpaTemplateQuery(JpaQueryMethod method, EntityManager em, TemplateQuery templateQuery) {
        super(method, em);
        this.templateQuery = templateQuery;
        this.query = getTemplate(templateQuery.value());
        this.countQuery = getTemplate(templateQuery.countQuery());
        ReturnedType returnedType = getQueryMethod().getResultProcessor().getReturnedType();
        this.resultClass = getQueryMethod().isQueryForEntity() ? returnedType.getDomainType() : null;
    }

    @Override
    protected Query doCreateQuery(Object[] values) {
        return doCreate(query, values);
    }

    @Override
    protected Query doCreateCountQuery(Object[] values) {
        return doCreate(countQuery, values);
    }

    private Query doCreate(Template template, Object[] values) {
        VelocityContext context = new VelocityContext(getContext(values));

        Writer buffer = new TemplateQueryWriter();
        template.merge(context, buffer);
        String sql = buffer.toString();

        Query query = templateQuery.nativeQuery() ?
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

    private Template getTemplate(String text) {
        if (text.isEmpty()) {
            return null;
        }
        try {
            RuntimeServices runtimeServices = RuntimeSingleton.getRuntimeServices();
            Template template = new Template();
            template.setRuntimeServices(runtimeServices);
            template.setData(runtimeServices.parse(text, getQueryMethod().getName()));
            template.initDocument();
            return template;
        } catch (ParseException e) {
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

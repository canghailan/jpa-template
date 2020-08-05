package cc.whohow.jpa.template;

import org.springframework.data.jpa.repository.query.JpaQueryMethodFactory;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.RepositoryQuery;

import javax.persistence.EntityManager;
import java.lang.reflect.Method;

public class TemplateQueryLookupStrategy implements QueryLookupStrategy {
    private final EntityManager entityManager;
    private final JpaQueryMethodFactory queryMethodFactory;
    private final QueryLookupStrategy defaultQueryLookupStrategy;

    public TemplateQueryLookupStrategy(EntityManager entityManager,
                                       JpaQueryMethodFactory queryMethodFactory,
                                       QueryLookupStrategy defaultQueryLookupStrategy) {
        this.entityManager = entityManager;
        this.queryMethodFactory = queryMethodFactory;
        this.defaultQueryLookupStrategy = defaultQueryLookupStrategy;
    }

    @Override
    public RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata, ProjectionFactory factory, NamedQueries namedQueries) {
        TemplateQuery templateQuery = method.getAnnotation(TemplateQuery.class);
        if (templateQuery != null) {
            return new JpaTemplateQuery(
                    queryMethodFactory.build(method, metadata, factory),
                    entityManager,
                    new TemplateQueryResolver(method, templateQuery));
        }
        if (defaultQueryLookupStrategy != null) {
            return defaultQueryLookupStrategy.resolveQuery(method, metadata, factory, namedQueries);
        }
        throw new IllegalStateException();
    }
}

package cc.whohow.jpa.template;

import org.springframework.data.jpa.provider.QueryExtractor;
import org.springframework.data.jpa.repository.query.EscapeCharacter;
import org.springframework.data.jpa.repository.query.JpaQueryLookupStrategy;
import org.springframework.data.jpa.repository.query.JpaQueryMethod;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.RepositoryQuery;

import javax.persistence.EntityManager;
import java.lang.reflect.Method;

public class TemplateQueryLookupStrategy implements QueryLookupStrategy {
    private final EntityManager entityManager;
    private final QueryExtractor extractor;
    private final QueryLookupStrategy jpaQueryLookupStrategy;

    public TemplateQueryLookupStrategy(EntityManager entityManager,
                                       Key key,
                                       QueryExtractor extractor,
                                       QueryMethodEvaluationContextProvider evaluationContextProvider,
                                       EscapeCharacter escape) {
        this.entityManager = entityManager;
        this.extractor = extractor;
        this.jpaQueryLookupStrategy = JpaQueryLookupStrategy.create(entityManager, key, extractor, evaluationContextProvider, escape);
    }

    @Override
    public RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata, ProjectionFactory factory, NamedQueries namedQueries) {
        TemplateQuery templateQuery = method.getAnnotation(TemplateQuery.class);
        if (templateQuery != null) {
            return new JpaTemplateQuery(
                    new JpaQueryMethod(method, metadata, factory, extractor), entityManager,
                    templateQuery,
                    new TemplateQueryResolver(method));
        }
        return jpaQueryLookupStrategy.resolveQuery(method, metadata, factory, namedQueries);
    }
}

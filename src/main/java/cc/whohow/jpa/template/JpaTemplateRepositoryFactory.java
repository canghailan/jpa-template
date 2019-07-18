package cc.whohow.jpa.template;

import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.data.repository.query.QueryLookupStrategy;

import javax.persistence.EntityManager;

public class JpaTemplateRepositoryFactory extends JpaRepositoryFactory {
    private final EntityManager entityManager;

    private final PersistenceProvider extractor;

    public JpaTemplateRepositoryFactory(EntityManager entityManager) {
        super(entityManager);
        this.entityManager = entityManager;
        this.extractor = PersistenceProvider.fromEntityManager(entityManager);
    }

    @Override
    protected QueryLookupStrategy getQueryLookupStrategy(QueryLookupStrategy.Key key, EvaluationContextProvider evaluationContextProvider) {
        return new TemplateQueryLookupStrategy(entityManager, key, extractor, evaluationContextProvider);
    }
}

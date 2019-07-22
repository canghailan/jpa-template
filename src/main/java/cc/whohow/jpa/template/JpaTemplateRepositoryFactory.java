package cc.whohow.jpa.template;

import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.jpa.repository.query.EscapeCharacter;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;

import javax.persistence.EntityManager;
import java.util.Optional;

public class JpaTemplateRepositoryFactory extends JpaRepositoryFactory {
    private final EntityManager entityManager;

    private final PersistenceProvider extractor;

    private EscapeCharacter escapeCharacter;

    public JpaTemplateRepositoryFactory(EntityManager entityManager) {
        super(entityManager);
        this.entityManager = entityManager;
        this.extractor = PersistenceProvider.fromEntityManager(entityManager);
        this.escapeCharacter = EscapeCharacter.DEFAULT;
    }

    @Override
    protected Optional<QueryLookupStrategy> getQueryLookupStrategy(QueryLookupStrategy.Key key,
                                                                   QueryMethodEvaluationContextProvider evaluationContextProvider) {
        return Optional.of(new TemplateQueryLookupStrategy(entityManager, key, extractor, evaluationContextProvider, escapeCharacter));
    }
}

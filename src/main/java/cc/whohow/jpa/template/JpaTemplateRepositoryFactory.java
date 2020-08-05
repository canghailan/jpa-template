package cc.whohow.jpa.template;

import org.springframework.data.jpa.repository.query.JpaQueryMethodFactory;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;

import javax.persistence.EntityManager;
import java.lang.reflect.Field;
import java.util.Optional;

public class JpaTemplateRepositoryFactory extends JpaRepositoryFactory {
    protected final EntityManager entityManager;
    protected final JpaQueryMethodFactory queryMethodFactory;

    public JpaTemplateRepositoryFactory(EntityManager entityManager) {
        super(entityManager);
        this.entityManager = entityManager;
        try {
            Field field = JpaRepositoryFactory.class.getDeclaredField("queryMethodFactory");
            field.setAccessible(true);
            this.queryMethodFactory = (JpaQueryMethodFactory) field.get(this);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected Optional<QueryLookupStrategy> getQueryLookupStrategy(QueryLookupStrategy.Key key,
                                                                   QueryMethodEvaluationContextProvider evaluationContextProvider) {
        Optional<QueryLookupStrategy> queryLookupStrategy = super.getQueryLookupStrategy(key, evaluationContextProvider);
        return Optional.of(new TemplateQueryLookupStrategy(
                entityManager, queryMethodFactory, queryLookupStrategy.orElse(null)));
    }
}

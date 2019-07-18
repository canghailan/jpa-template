package cc.whohow.jpa.template;

import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

import javax.persistence.EntityManager;

public class JpaTemplateRepositoryFactoryBean extends JpaRepositoryFactoryBean {
    public JpaTemplateRepositoryFactoryBean(Class repositoryInterface) {
        super(repositoryInterface);
    }

    @Override
    protected RepositoryFactorySupport createRepositoryFactory(EntityManager entityManager) {
        return new JpaTemplateRepositoryFactory(entityManager);
    }
}

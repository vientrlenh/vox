package com.sep.vox.infrastructure.service;

import java.util.function.Supplier;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.sep.vox.application.port.output.RequiresNewTransactionPort;
import com.sep.vox.infrastructure.persistence.DataConflictTranslation;

@Service
public class SpringRequiresNewTransactionService implements RequiresNewTransactionPort {

    private final TransactionTemplate requiresNewTransactionTemplate;

    public SpringRequiresNewTransactionService(PlatformTransactionManager transactionManager) {
        this.requiresNewTransactionTemplate = new TransactionTemplate(transactionManager);
        this.requiresNewTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public void runInNewTransaction(Runnable work) {
        DataConflictTranslation.translating(
            () -> requiresNewTransactionTemplate.executeWithoutResult(status -> work.run()));
    }

    @Override
    public <T> T callInNewTransaction(Supplier<T> work) {
        return DataConflictTranslation.translating(
            () -> requiresNewTransactionTemplate.execute(status -> work.get()));
    }
}

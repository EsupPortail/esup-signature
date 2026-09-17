package org.esupportail.esupsignature.playwright;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class PlaywrightFailureTracker implements AfterTestExecutionCallback {

    private Throwable failure;

    @Override
    public void afterTestExecution(ExtensionContext context) {
        failure = context.getExecutionException().orElse(null);
    }

    public Throwable getFailure() {
        return failure;
    }

    public void clearFailure() {
        failure = null;
    }
}


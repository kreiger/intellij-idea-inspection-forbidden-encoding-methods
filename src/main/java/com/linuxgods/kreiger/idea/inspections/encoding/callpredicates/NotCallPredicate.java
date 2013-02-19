package com.linuxgods.kreiger.idea.inspections.encoding.callpredicates;

import com.intellij.psi.PsiCallExpression;

public class NotCallPredicate implements CallPredicate {
    private final CallPredicate condition;

    public NotCallPredicate(CallPredicate callPredicate) {
        this.condition = callPredicate;
    }

    @Override
    public boolean matches(PsiCallExpression expression) {
        return !condition.matches(expression);
    }
}

package com.linuxgods.kreiger.idea.inspections.encoding.callpredicates;

import com.intellij.psi.PsiCallExpression;
import com.intellij.psi.PsiMethod;

public class NameCallPredicate implements CallPredicate {
    private final String methodName;

    public NameCallPredicate(String methodName) {
        this.methodName = methodName;
    }

    @Override
    public boolean matches(PsiCallExpression expression) {
        PsiMethod method = expression.resolveMethod();
        return null != method && methodName.equals(method.getName());
    }

    @Override
    public String toString() {
        return super.toString() + ":" + methodName;
    }
}

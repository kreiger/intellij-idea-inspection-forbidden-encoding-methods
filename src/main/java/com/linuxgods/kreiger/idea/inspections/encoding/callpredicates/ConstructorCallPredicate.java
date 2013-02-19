package com.linuxgods.kreiger.idea.inspections.encoding.callpredicates;

import com.intellij.psi.PsiCallExpression;
import com.intellij.psi.PsiMethod;

public class ConstructorCallPredicate implements CallPredicate {
    @Override
    public boolean matches(PsiCallExpression expression) {
        PsiMethod method = expression.resolveMethod();
        return null != method && method.isConstructor();
    }
}

package com.linuxgods.kreiger.idea.inspections.encoding.callpredicates;

import com.intellij.psi.*;

public class ClassCallPredicate implements CallPredicate {
    private final Class<?> clazz;

    public ClassCallPredicate(Class<?> clazz) {
        this.clazz = clazz;
    }

    @Override
    public boolean matches(PsiCallExpression expression) {
        String clazzCanonicalName = clazz.getCanonicalName();
        if (expression instanceof PsiMethodCallExpression) {
            PsiMethodCallExpression methodCallExpression = (PsiMethodCallExpression) expression;
            PsiExpression qualifierExpression = methodCallExpression.getMethodExpression().getQualifierExpression();
            if (qualifierExpression == null) {
                return false;
            }
            PsiType type = qualifierExpression.getType();
            if (type != null) {
                return clazz.getCanonicalName().equals(type.getCanonicalText());
            }
        }
        PsiMethod method = expression.resolveMethod();
        if (method == null) {
            return false;
        }
        final PsiClass aClass = method.getContainingClass();
        if (aClass == null) {
            return false;
        }
        final String expressionClassName = aClass.getQualifiedName();
        return expressionClassName != null && clazzCanonicalName.equals(expressionClassName);

    }

    @Override
    public String toString() {
        return super.toString() + ":" + clazz.getName();
    }

}

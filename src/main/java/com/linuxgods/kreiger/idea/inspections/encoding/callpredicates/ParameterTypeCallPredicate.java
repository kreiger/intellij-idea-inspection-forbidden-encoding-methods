package com.linuxgods.kreiger.idea.inspections.encoding.callpredicates;

import com.intellij.psi.PsiCallExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiType;
import com.linuxgods.kreiger.idea.inspections.encoding.ForbiddenEncodingMethod;

public class ParameterTypeCallPredicate implements CallPredicate {
    private final Class<?> parameterType;

    public ParameterTypeCallPredicate(Class<?> parameterType) {
        this.parameterType = parameterType;
    }

    @Override
    public boolean matches(PsiCallExpression expression) {
        PsiExpressionList argumentList = expression.getArgumentList();
        if (null == argumentList) {
            return false;
        }
        PsiType[] argumentTypes = argumentList.getExpressionTypes();
        for (PsiType argumentType : argumentTypes) {
            if (null != argumentType && ForbiddenEncodingMethod.isSubClassOrSameOf(argumentType, parameterType)) {
                return true;
            }
        }
        return false;
    }
}

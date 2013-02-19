package com.linuxgods.kreiger.idea.inspections.encoding.callpredicates;

import com.intellij.psi.PsiCallExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiType;
import com.linuxgods.kreiger.idea.inspections.encoding.ForbiddenEncodingMethod;

import static java.util.Arrays.asList;

public class ParameterTypesCallPredicate implements CallPredicate {
    private final Class[] parameterTypes;

    public ParameterTypesCallPredicate(Class... parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    @Override
    public boolean matches(PsiCallExpression expression) {
        PsiExpressionList argumentList = expression.getArgumentList();
        if (null == argumentList) {
            return false;
        }
        PsiType[] argumentTypes = argumentList.getExpressionTypes();
        if (argumentTypes.length != parameterTypes.length) {
            return false;
        }
        Class[] parameterTypes = this.parameterTypes;
        for (int i = 0; i < argumentTypes.length; i++) {
            PsiType argumentType = argumentTypes[i];
            Class parameterType = parameterTypes[i];
            if (null == argumentType) {
                return false;
            }
            if (!ForbiddenEncodingMethod.isSubClassOrSameOf(argumentType, parameterType)) {
                return false;
            }
        }
        return true;
    }

    @Override

    public String toString() {
        return super.toString() + ":" + asList(parameterTypes);
    }

}

package com.linuxgods.kreiger.idea.inspections.encoding.callpredicates;

import com.intellij.psi.*;
import com.linuxgods.kreiger.idea.inspections.encoding.AddEncodingParameter;
import com.linuxgods.kreiger.idea.inspections.encoding.ForbiddenEncodingMethod;

public class NeedParameterCallPredicate implements CallPredicate {
    private final AddEncodingParameter fix;
    private ForbiddenEncodingMethod forbiddenEncodingMethod;

    public NeedParameterCallPredicate(ForbiddenEncodingMethod forbiddenEncodingMethod, AddEncodingParameter fix) {
        this.forbiddenEncodingMethod = forbiddenEncodingMethod;
        this.fix = fix;
    }

    @Override
    public boolean matches(PsiCallExpression expression) {
        PsiMethod method = expression.resolveMethod();
        if (null == method) {
            return true;
        }
        PsiParameterList parameterList = method.getParameterList();
        if (parameterList.getParametersCount() >= 2) {
            boolean alreadyFixed = lastParameterIsOfType(parameterList, fix.getParameterType());
            if (alreadyFixed) {
                return false;
            }
        }
        PsiClass containingClass = method.getContainingClass();
        if (containingClass == null) {
            return true;
        }
        PsiMethod[] methodsWithSameName = containingClass.findMethodsByName(method.getName(), false);
        for (PsiMethod sameNameMethod : methodsWithSameName) {
            PsiParameterList sameNameParameterList = sameNameMethod.getParameterList();
            boolean sameNameMethodHasOneMoreParameter = parameterList.getParametersCount() + 1 == sameNameParameterList.getParametersCount();
            if (!sameNameMethodHasOneMoreParameter) {
                continue;
            }
            if (!isPrefixOf(parameterList, sameNameParameterList)) {
                continue;
            }
            if (lastParameterIsOfType(sameNameParameterList, fix.getParameterType())) {
                forbiddenEncodingMethod.canBeFixedBy(fix);
                return true;
            }
        }
        return true;
    }

    private boolean lastParameterIsOfType(PsiParameterList parameterList, Class<?> parameterType) {
        PsiParameter lastParameter = parameterList.getParameters()[parameterList.getParametersCount() - 1];
        return ForbiddenEncodingMethod.isSubClassOrSameOf(lastParameter.getType(), parameterType);
    }

    private boolean isPrefixOf(PsiParameterList shorterParameterList, PsiParameterList longerParameterList) {
        PsiParameter[] parameters = shorterParameterList.getParameters();
        PsiParameter[] sameNameParameters = longerParameterList.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            PsiType parameter1Type = parameters[i].getType();
            PsiType parameter2Type = sameNameParameters[i].getType();
            if (!parameter1Type.equals(parameter2Type)) {
                return false;
            }
        }
        return true;
    }
}

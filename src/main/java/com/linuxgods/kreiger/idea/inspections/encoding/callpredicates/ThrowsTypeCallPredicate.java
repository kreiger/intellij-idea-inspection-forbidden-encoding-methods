package com.linuxgods.kreiger.idea.inspections.encoding.callpredicates;

import com.intellij.psi.PsiCallExpression;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReferenceList;

public class ThrowsTypeCallPredicate implements CallPredicate {
    private final Class<? extends Throwable> throwableClass;

    public ThrowsTypeCallPredicate(Class<? extends Throwable> throwableClass) {
        this.throwableClass = throwableClass;
    }

    @Override
    public boolean matches(PsiCallExpression expression) {
        PsiMethod method = expression.resolveMethod();
        if (null == method) {
            return false;
        }
        PsiReferenceList throwsList = method.getThrowsList();
        PsiClassType[] referencedTypes = throwsList.getReferencedTypes();
        for (PsiClassType referencedType : referencedTypes) {
            if (referencedType.getClassName().equals(throwableClass.getName())) {
                return true;
            }
        }
        return false;
    }
}

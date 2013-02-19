package com.linuxgods.kreiger.idea.inspections.encoding.callpredicates;

import com.intellij.psi.PsiCallExpression;

public interface CallPredicate {
    public boolean matches(PsiCallExpression expression);
}

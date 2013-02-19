package com.linuxgods.kreiger.idea.inspections.encoding;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.psi.*;
import com.linuxgods.kreiger.idea.inspections.encoding.callpredicates.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ForbiddenEncodingMethod {

    private List<CallPredicate> callPredicates = new LinkedList<CallPredicate>();
    private List<LocalQuickFix> fixes = new ArrayList<LocalQuickFix>();

    public boolean matches(PsiCallExpression expression) {
        for (CallPredicate callPredicate : callPredicates) {
            if (!callPredicate.matches(expression)) {
                return false;
            }
        }
        return true;
    }

    public LocalQuickFix[] getFixes() {
        return fixes.toArray(new LocalQuickFix[fixes.size()]);
    }

    public static ForbiddenEncodingMethod methodsOf(final Class<?> clazz) {
        return new ForbiddenEncodingMethod().addCondition(new ClassCallPredicate(clazz));
    }

    public ForbiddenEncodingMethod named(final String methodName) {
        return addCondition(new NameCallPredicate(methodName));
    }

    public static ForbiddenEncodingMethod constructorsOf(final Class<?> clazz) {
        return methodsOf(clazz).addCondition(new ConstructorCallPredicate());
    }

    private ForbiddenEncodingMethod addCondition(CallPredicate callPredicate) {
        callPredicates.add(callPredicate);
        return this;
    }

    public ForbiddenEncodingMethod withParameterTypes(final Class... parameterTypes) {
        return addCondition(new ParameterTypesCallPredicate(parameterTypes));

    }

    public ForbiddenEncodingMethod whichDontThrow(final Class<? extends Throwable> throwableClass) {
        return addCondition(new NotCallPredicate(new ThrowsTypeCallPredicate(throwableClass)));
    }

    public ForbiddenEncodingMethod canBeFixedBy(LocalQuickFix fix) {
        this.fixes.add(fix);
        return this;
    }

    public ForbiddenEncodingMethod exceptWithParameterType(final Class<?> parameterType) {
        return addCondition(new NotCallPredicate(new ParameterTypeCallPredicate(parameterType)));
    }

    public ForbiddenEncodingMethod withParameterType(final Class<byte[]> parameterType) {
        return addCondition(new ParameterTypeCallPredicate(parameterType));
    }

    public ForbiddenEncodingMethod needParameter(final AddEncodingParameter fix) {
        return addCondition(new NeedParameterCallPredicate(this, fix));
    }

    public static boolean isSubClassOrSameOf(PsiType argumentType, Class parameterType) {
        if (argumentType.getCanonicalText().equals(parameterType.getCanonicalName())) {
            return true;
        }

        for (PsiType type : argumentType.getSuperTypes()) {
            if (type.getCanonicalText().equals(parameterType.getCanonicalName())) {
                return true;
            }
        }
        return false;
    }

    public ForbiddenEncodingMethod needEitherParameter(AddEncodingParameter... addEncodingParameters) {
        for (AddEncodingParameter addEncodingParameter : addEncodingParameters) {
            needParameter(addEncodingParameter);
        }
        return this;
    }

    public ForbiddenEncodingMethod withNoParameters() {
        return withParameterTypes();
    }
}

package com.linuxgods.kreiger;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.psi.*;
import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static java.util.Arrays.asList;

class ForbiddenEncodingMethod {

    private List<Condition> conditions = new LinkedList<Condition>();
    private List<LocalQuickFix> fixes = new ArrayList<LocalQuickFix>();

    private static boolean argumentsDiffer(PsiCall expression, Class[] parameterTypes) {
        PsiExpressionList argumentList = expression.getArgumentList();
        return null == argumentList || argumentsDiffer(argumentList, parameterTypes);
    }

    private static boolean argumentsDiffer(PsiExpressionList argumentList, Class[] parameterTypes) {
        PsiType[] argumentTypes = argumentList.getExpressionTypes();
        return argumentsDiffer(argumentTypes, parameterTypes);
    }

    private static boolean methodsDiffer(PsiMethod method, String methodName) {
        if (null == method) {
            return true;
        }
        @NonNls final String expressionMethodName =
                method.getName();
        return !methodName.equals(expressionMethodName);
    }

    private static boolean argumentsDiffer(PsiType[] argumentTypes, Class[] parameterTypes) {
        if (argumentTypes.length != parameterTypes.length) {
            return true;
        }
        for (int i = 0; i < argumentTypes.length; i++) {
            PsiType argumentType = argumentTypes[i];
            Class parameterType = parameterTypes[i];
            if (null == argumentType) {
                return true;
            }
            if (!isSubClassOrSameOf(argumentType, parameterType)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSubClassOrSameOf(PsiType argumentType, Class parameterType) {
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

    private static boolean isMethodOfClass(PsiCallExpression expression, Class<?> clazz) {
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


    public boolean matches(PsiCallExpression expression) {
        for (Condition condition : conditions) {
            if (!condition.matches(expression)) {
                return false;
            }
        }
        return true;
    }

    public LocalQuickFix[] getFixes() {
        return fixes.toArray(new LocalQuickFix[fixes.size()]);
    }

    public static ForbiddenEncodingMethod methodsOf(final Class<?> clazz) {
        return new ForbiddenEncodingMethod().addCondition(new ClassCondition(clazz));
    }

    public ForbiddenEncodingMethod named(final String methodName) {
        return addCondition(new NameCondition(methodName));
    }

    public static ForbiddenEncodingMethod constructorsOf(final Class<?> clazz) {
        return methodsOf(clazz).addCondition(new ConstructorCondition());
    }

    private ForbiddenEncodingMethod addCondition(Condition condition) {
        conditions.add(condition);
        return this;
    }

    public ForbiddenEncodingMethod withParameterTypes(final Class... parameterTypes) {
        return addCondition(new ParameterTypesCondition(parameterTypes));
        
    }

    public ForbiddenEncodingMethod whichDontThrow(final Class<? extends Throwable> throwableClass) {
        return addCondition(new Condition() {
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
                        return false;
                    }
                }
                return true;
            }
        });
    }

    public ForbiddenEncodingMethod withoutParameters() {
        return withParameterTypes();
    }

    public ForbiddenEncodingMethod hasFix(LocalQuickFix fix) {
        this.fixes.add(fix);
        return this;
    }


    public static interface Condition {

        public boolean matches(PsiCallExpression expression);

    }

    private static class ConstructorCondition implements Condition {
        @Override
        public boolean matches(PsiCallExpression expression) {
            PsiMethod method = expression.resolveMethod();
            return null != method && method.isConstructor();
        }
    }

    private static class ParameterTypesCondition implements Condition {
        private final Class[] parameterTypes;

        public ParameterTypesCondition(Class... parameterTypes) {
            this.parameterTypes = parameterTypes;
        }

        @Override
        public boolean matches(PsiCallExpression expression) {
            return !argumentsDiffer(expression, parameterTypes);
        }
        @Override

        public String toString() {
            return super.toString()+":"+asList(parameterTypes);
        }

    }

    private static class NameCondition implements Condition {
        private final String methodName;

        public NameCondition(String methodName) {
            this.methodName = methodName;
        }

        @Override
        public boolean matches(PsiCallExpression expression) {
            return !methodsDiffer(expression.resolveMethod(), methodName);
        }

        @Override
        public String toString() {
            return super.toString()+":"+methodName;
        }
    }

    private static class ClassCondition implements Condition {
        private final Class<?> clazz;

        public ClassCondition(Class<?> clazz) {
            this.clazz = clazz;
        }

        @Override
        public boolean matches(PsiCallExpression expression) {
            return isMethodOfClass(expression, clazz);
        }

        @Override
        public String toString() {
            return super.toString()+":"+clazz.getName();
        }

    }
}

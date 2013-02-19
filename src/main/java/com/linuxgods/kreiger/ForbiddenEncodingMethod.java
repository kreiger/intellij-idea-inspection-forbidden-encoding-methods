package com.linuxgods.kreiger;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.psi.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static java.util.Arrays.asList;

class ForbiddenEncodingMethod {

    private List<Condition> conditions = new LinkedList<Condition>();
    private List<LocalQuickFix> fixes = new ArrayList<LocalQuickFix>();

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

    public ForbiddenEncodingMethod canBeFixedBy(LocalQuickFix fix) {
        this.fixes.add(fix);
        return this;
    }

    public ForbiddenEncodingMethod exceptWithParameterType(final Class<?> parameterType) {
        return addCondition(new NotCondition(new ParameterTypeCondition(parameterType)));
    }

    public ForbiddenEncodingMethod withParameterType(final Class<byte[]> parameterType) {
        return addCondition(new ParameterTypeCondition(parameterType));
    }

    public ForbiddenEncodingMethod needParameter(final AddEncodingParameter fix) {
        return addCondition(new Condition() {
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
                        canBeFixedBy(fix);
                        return true;
                    }
                }
                return true;
            }

            private boolean lastParameterIsOfType(PsiParameterList parameterList, Class<?> parameterType) {
                PsiParameter lastParameter = parameterList.getParameters()[parameterList.getParametersCount() - 1];
                return isSubClassOrSameOf(lastParameter.getType(), parameterType);
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
        });
    }

    private static interface Condition {
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
                if (!isSubClassOrSameOf(argumentType, parameterType)) {
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

    private static class NameCondition implements Condition {
        private final String methodName;

        public NameCondition(String methodName) {
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

    private static class ClassCondition implements Condition {
        private final Class<?> clazz;

        public ClassCondition(Class<?> clazz) {
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

    private static class ParameterTypeCondition implements Condition {
        private final Class<?> parameterType;

        public ParameterTypeCondition(Class<?> parameterType) {
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
                if (null != argumentType && isSubClassOrSameOf(argumentType, parameterType)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static class NotCondition implements Condition {
        private final ParameterTypeCondition condition;

        public NotCondition(ParameterTypeCondition condition) {
            this.condition = condition;
        }

        @Override
        public boolean matches(PsiCallExpression expression) {
            return !condition.matches(expression);
        }
    }
}

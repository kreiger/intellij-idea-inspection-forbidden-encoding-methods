package com.linuxgods.kreiger;

import com.intellij.codeInspection.BaseJavaLocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import static com.intellij.codeInspection.ProblemHighlightType.*;
import static com.intellij.psi.CommonClassNames.JAVA_LANG_STRING;

public class ForbiddenEncodingMethodsInspection extends BaseJavaLocalInspectionTool {

    public static final AddEncodingParameter ADD_ENCODING_PARAMETER = new AddEncodingParameter();
    public static final ForbiddenEncodingMethod[] FORBIDDEN_ENCODING_METHODS = new ForbiddenEncodingMethod[]{
            new ForbiddenEncodingMethod(String.class, "getBytes"),
            new ForbiddenEncodingMethod(String.class, "String", "byte[]"),
            new ForbiddenEncodingMethod(InputStreamReader.class, "InputStreamReader", "InputStream"),
            new ForbiddenEncodingMethod(OutputStreamWriter.class, "OutputStreamWriter", "OutputStream"),
    };

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new ForbiddenEncodingMethodsVisitor(holder, isOnTheFly);
    }

    private static class ForbiddenEncodingMethodsVisitor extends JavaElementVisitor {
        private ProblemsHolder holder;
        private boolean onTheFly;

        public ForbiddenEncodingMethodsVisitor(ProblemsHolder holder, boolean onTheFly) {
            this.holder = holder;
            this.onTheFly = onTheFly;
        }

        @Override
        public void visitCallExpression(final PsiCallExpression expression) {
            super.visitCallExpression(expression);
            for (ForbiddenEncodingMethod forbiddenEncodingMethod : FORBIDDEN_ENCODING_METHODS) {
                if (isCallTo(expression, forbiddenEncodingMethod)) {
                    holder.registerProblem(expression, "Method uses platform default encoding", GENERIC_ERROR_OR_WARNING, (TextRange) null, ADD_ENCODING_PARAMETER);
                }
            }
        }

        private static boolean isCallTo(PsiCall expression, ForbiddenEncodingMethod forbiddenEncodingMethod) {
            PsiExpressionList argumentList = expression.getArgumentList();
            if (null == argumentList) {
                return false;
            }
            PsiType[] argumentTypes = argumentList.getExpressionTypes();
            String[] parameterTypes = forbiddenEncodingMethod.getParameterTypes();
            if (argumentTypes.length != parameterTypes.length) {
                return false;
            }
            for (int i = 0; i < argumentTypes.length; i++) {
                PsiType argumentType = argumentTypes[i];
                String parameterType = parameterTypes[i];
                if (null == argumentType) {
                    return false;
                }
                if (!parameterType.equals(argumentType.getCanonicalText())) {
                    return false;
                }
            }
            PsiMethod method = expression.resolveMethod();
            if (null == method) {
                return false;
            }
            @NonNls final String expressionMethodName =
                    method.getName();
            if (!forbiddenEncodingMethod.getMethodName().equals(expressionMethodName)) {
                return false;
            }
            return isMethodOfClass(expression.resolveMethod(), forbiddenEncodingMethod.getClassName());
        }

        private static boolean isMethodOfClass(PsiMethod method, String className) {
            if (method == null) {
                return false;
            }
            final PsiClass aClass = method.getContainingClass();
            if (aClass == null) {
                return false;
            }
            final String expressionClassName = aClass.getQualifiedName();
            if (expressionClassName == null) {
                return false;
            }
            return className.equals(expressionClassName);
        }

    }

    private static class AddEncodingParameter implements LocalQuickFix {

        @NotNull
        @Override
        public String getName() {
            return "Fix call to method using platform default encoding";
        }

        @NotNull
        @Override
        public String getFamilyName() {
            return "Fix call to method using platform default encoding";
        }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiCallExpression expression = (PsiCallExpression) descriptor.getPsiElement();
            PsiExpressionList argumentList = expression.getArgumentList();
            if (argumentList == null) {
                return;
            }
            argumentList.add(JavaPsiFacade.getElementFactory(project).createExpressionFromText("java.nio.charset.Charset.forName(\"UTF-8\")", expression));
        }
    }

    private static class ForbiddenEncodingMethod {
        private final String className;
        @NonNls
        private final String methodName;
        private final String[] parameterTypes;

        private ForbiddenEncodingMethod(String className, @NonNls String methodName, String... parameterTypes) {
            this.className = className;
            this.methodName = methodName;
            this.parameterTypes = parameterTypes;
        }

        public ForbiddenEncodingMethod(Class<?> aClass, String methodName, String... parameterTypes) {
            this(aClass.getName(), methodName, parameterTypes);
        }

        public String getClassName() {
            return className;
        }

        @NonNls
        public String getMethodName() {
            return methodName;
        }

        public String[] getParameterTypes() {
            return parameterTypes;
        }
    }
}

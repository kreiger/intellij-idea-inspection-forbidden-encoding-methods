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

import static com.intellij.codeInspection.ProblemHighlightType.*;

public class ForbiddenEncodingMethods extends BaseJavaLocalInspectionTool {

    public static final AddEncodingParameter ADD_ENCODING_PARAMETER = new AddEncodingParameter();

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
            if (isCallTo(expression, CommonClassNames.JAVA_LANG_STRING, "getBytes")) {
                holder.registerProblem(expression, "Method uses platform default encoding", GENERIC_ERROR_OR_WARNING, (TextRange)null, ADD_ENCODING_PARAMETER);
            } else if (isCallTo(expression, CommonClassNames.JAVA_LANG_STRING, "String", "byte[]")) {
                holder.registerProblem(expression, "Method uses platform default encoding", GENERIC_ERROR_OR_WARNING, (TextRange)null, ADD_ENCODING_PARAMETER);
            }
        }

        private static boolean isCallTo(PsiCall expression,
                                        String className, @NonNls String methodName, String... parameters) {
            PsiExpressionList argumentList = expression.getArgumentList();
            if (null == argumentList) {
                return false;
            }
            PsiType[] argumentTypes = argumentList.getExpressionTypes();
            if (argumentTypes.length != parameters.length) {
                return false;
            }
            for (int i = 0; i < argumentTypes.length; i++) {
                PsiType argumentType = argumentTypes[i];
                if (!parameters[i].equals(argumentType.getCanonicalText())) {
                    return false;
                }
            }
            PsiMethod method = expression.resolveMethod();
            if (null == method) {
                return false;
            }
            @NonNls final String expressionMethodName =
                    method.getName();
            if (!methodName.equals(expressionMethodName)) {
                return false;
            }
            return isMethodOfClass(expression.resolveMethod(), className);
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
            PsiExpression encodingString = JavaPsiFacade.getElementFactory(project).createExpressionFromText("Charset.forName(\"UTF-8\")", expression);
            expression.getArgumentList().add(encodingString);
        }
    }
}

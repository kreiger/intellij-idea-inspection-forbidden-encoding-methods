package com.linuxgods.kreiger;

import com.intellij.codeInspection.BaseJavaLocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.Charset;

import static com.intellij.codeInspection.ProblemHighlightType.*;
import static com.linuxgods.kreiger.ForbiddenEncodingMethod.methodsOf;
import static com.linuxgods.kreiger.ForbiddenEncodingMethod.constructorsOf;

public class ForbiddenEncodingMethodsInspection extends BaseJavaLocalInspectionTool {

    public static final AddEncodingParameter ADD_STRING = new AddEncodingParameter("\"UTF-8\"");
    public static final AddEncodingParameter ADD_CHARSET = new AddEncodingParameter("Charset.forName(\"UTF-8\")");

    public static final ForbiddenEncodingMethod[] FORBIDDEN_ENCODING_METHODS = new ForbiddenEncodingMethod[]{
            methodsOf(String.class).named("getBytes").withoutParameters().hasFix(ADD_CHARSET),
            constructorsOf(String.class).withParameterTypes(byte[].class).hasFix(ADD_CHARSET),
            methodsOf(Charset.class).named("defaultCharset"),
            methodsOf(FileWriter.class),
            constructorsOf(InputStreamReader.class).withParameterTypes(InputStream.class).hasFix(ADD_CHARSET),
            constructorsOf(OutputStreamWriter.class).withParameterTypes(OutputStream.class).hasFix(ADD_CHARSET),
            constructorsOf(PrintStream.class).whichDontThrow(UnsupportedEncodingException.class).hasFix(ADD_STRING),
            constructorsOf(PrintWriter.class).whichDontThrow(UnsupportedEncodingException.class).hasFix(ADD_STRING),

    };

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new ForbiddenEncodingMethodsVisitor(holder);
    }

    private static class ForbiddenEncodingMethodsVisitor extends JavaElementVisitor {
        private ProblemsHolder holder;

        public ForbiddenEncodingMethodsVisitor(ProblemsHolder holder) {
            this.holder = holder;
        }

        @Override
        public void visitCallExpression(final PsiCallExpression expression) {
            super.visitCallExpression(expression);
            for (ForbiddenEncodingMethod forbiddenEncodingMethod : FORBIDDEN_ENCODING_METHODS) {
                if (forbiddenEncodingMethod.matches(expression)) {
                    holder.registerProblem(expression, "Method uses platform default encoding", GENERIC_ERROR_OR_WARNING, forbiddenEncodingMethod.getFixes());
                }
            }
        }

    }

    private static class AddEncodingParameter implements LocalQuickFix {

        private String text;

        private AddEncodingParameter(String text) {
            this.text = text;
        }

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
            argumentList.add(JavaPsiFacade.getElementFactory(project).createExpressionFromText(text, expression));
        }
    }

}

package com.linuxgods.kreiger;

import com.intellij.codeInspection.BaseJavaLocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.Charset;

import static com.intellij.codeInspection.ProblemHighlightType.*;
import static com.linuxgods.kreiger.ForbiddenEncodingMethod.methodsOf;
import static com.linuxgods.kreiger.ForbiddenEncodingMethod.constructorsOf;

public class ForbiddenEncodingMethodsInspection extends BaseJavaLocalInspectionTool {

    public static final AddEncodingParameter STRING = new AddEncodingParameter(String.class, "\"UTF-8\"");
    public static final AddEncodingParameter CHARSET = new AddEncodingParameter(Charset.class, "Charset.forName(\"UTF-8\")");

    public static final ForbiddenEncodingMethod[] FORBIDDEN_ENCODING_METHODS = new ForbiddenEncodingMethod[]{
            methodsOf(String.class).named("getBytes").needParameter(CHARSET).needParameter(STRING),
            constructorsOf(String.class).withParameterType(byte[].class).needParameter(CHARSET).needParameter(STRING),
            methodsOf(Charset.class).named("defaultCharset"),
            methodsOf(FileWriter.class),
            constructorsOf(InputStreamReader.class).needParameter(CHARSET).needParameter(STRING),
            constructorsOf(OutputStreamWriter.class).needParameter(CHARSET).needParameter(STRING),
            constructorsOf(PrintStream.class).whichDontThrow(UnsupportedEncodingException.class).needParameter(STRING),
            constructorsOf(PrintWriter.class)
                    .exceptWithParameterType(Writer.class).whichDontThrow(UnsupportedEncodingException.class)
                    .needParameter(STRING)
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
                    break;
                }
            }
        }

    }

}

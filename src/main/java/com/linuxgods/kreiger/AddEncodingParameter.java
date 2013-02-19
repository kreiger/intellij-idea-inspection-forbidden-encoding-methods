package com.linuxgods.kreiger;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiCallExpression;
import com.intellij.psi.PsiExpressionList;
import org.jetbrains.annotations.NotNull;

class AddEncodingParameter implements LocalQuickFix {

    private String text;
    private Class<?> parameterType;

    AddEncodingParameter(Class<?> parameterType, String text) {
        this.parameterType = parameterType;
        this.text = text;
    }

    @NotNull
    @Override
    public String getName() {
        return "Add parameter "+text;
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

    public Class<?> getParameterType() {
        return parameterType;
    }
}

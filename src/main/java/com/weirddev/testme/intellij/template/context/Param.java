package com.weirddev.testme.intellij.template.context;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import com.weirddev.testme.intellij.common.utils.LanguageUtils;
import com.weirddev.testme.intellij.scala.resolvers.ScalaPsiTreeUtils;
import com.weirddev.testme.intellij.template.TypeDictionary;

import java.util.ArrayList;
import java.util.Optional;

/**
 * Date: 24/10/2016
 * @author Yaron Yamin
 */
public class Param {
    private final Type type;
    private String name;
    private final ArrayList<Field> assignedToFields;
    private boolean isPathVariable = false;
    private boolean isRequestHeader = false;
    private boolean isRequestBody = false;
    private boolean isRequestParam = false;
    private String requestName = "";
    private boolean optional = false;

    public Param(PsiParameter psiParameter, Optional<PsiType> substitutedType, TypeDictionary typeDictionary, int maxRecursionDepth, ArrayList<Field> assignedToFields, boolean shouldResolveAllMethods) {
        this(resolveType(psiParameter, substitutedType,shouldResolveAllMethods, typeDictionary, maxRecursionDepth), psiParameter.getName(),assignedToFields);
        PsiAnnotation[] annotations = psiParameter.getAnnotations();
        for (PsiAnnotation annotation : annotations) {
            if (annotation.getText().startsWith("@PathVariable")) {
                isPathVariable = true;
                break;
            }

            if (annotation.getText().startsWith("@RequestBody")) {
                isRequestBody = true;
                PsiAnnotationMemberValue value = annotation.findAttributeValue("required");
                if (value != null) {
                    optional = "false".equals(value.getText());
                }
                break;
            }

            if (annotation.getText().startsWith("@RequestParam")) {
                isRequestParam = true;
                PsiAnnotationMemberValue value = annotation.findAttributeValue("value");
                if (value != null) {
                    requestName = value.getText().replace("\"", "");
                }
                PsiAnnotationMemberValue required = annotation.findAttributeValue("required");
                if (required != null) {
                    optional = "false".equals(required.getText());
                }
                break;
            }

            if (annotation.getText().startsWith("@RequestHeader")) {
                isRequestHeader = true;
                PsiAnnotationMemberValue value = annotation.findAttributeValue("value");
                if (value != null) {
                    requestName = value.getText().replace("\"", "");
                }
                PsiAnnotationMemberValue required = annotation.findAttributeValue("required");
                if (required != null) {
                    optional = "false".equals(required.getText());
                }
                break;
            }
        }
    }

    private static Type resolveType(PsiParameter psiParameter, Optional<PsiType> substitutedType, boolean shouldResolveAllMethods, TypeDictionary typeDictionary, int maxRecursionDepth) {

        Object element = null;
        if (LanguageUtils.isScala(psiParameter.getLanguage())) {
            element = ScalaPsiTreeUtils.resolveRelatedTypeElement(psiParameter);
        }
        return typeDictionary.getType(substitutedType.orElse(psiParameter.getType()), maxRecursionDepth, shouldResolveAllMethods,element);
    }

    public Param(Type type, String name, ArrayList<Field> assignedToFields) {
        this.type = type;
        this.name = name;
        this.assignedToFields = assignedToFields;
    }

    public Type getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public ArrayList<Field> getAssignedToFields() {
        return assignedToFields;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Param)) return false;

        Param param = (Param) o;

        return type != null ? type.equals(param.type) : param.type == null;
    }

    @Override
    public int hashCode() {
        return type != null ? type.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Param{" + "name='" + name + ", type=" + type + '\'' + ", assignedToFields=" + assignedToFields + '}';
    }

    public boolean isPathVariable() {
        return isPathVariable;
    }

    public boolean isRequestBody() {
        return isRequestBody;
    }

    public boolean isRequestParam() {
        return isRequestParam;
    }

    public boolean isRequestHeader() {
        return isRequestHeader;
    }

    public String getRequestName() {
        return requestName;
    }

    public boolean isOptional() {
        return optional;
    }
}

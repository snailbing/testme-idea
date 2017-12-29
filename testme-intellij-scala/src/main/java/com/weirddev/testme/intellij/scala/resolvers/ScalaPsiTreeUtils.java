package com.weirddev.testme.intellij.scala.resolvers;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.weirddev.testme.intellij.scala.utils.GenericsExpressionParser;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.scala.lang.psi.api.base.ScPrimaryConstructor;
import org.jetbrains.plugins.scala.lang.psi.api.base.types.ScParameterizedTypeElement;
import org.jetbrains.plugins.scala.lang.psi.api.base.types.ScTypeElement;
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunction;
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScClassParameter;
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScParameter;
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScClass;
import org.jetbrains.plugins.scala.lang.psi.light.ScFunctionWrapper;
import org.jetbrains.plugins.scala.lang.psi.light.ScPrimaryConstructorWrapper;
import org.jetbrains.plugins.scala.lang.psi.types.ScParameterizedType;
import org.jetbrains.plugins.scala.lang.psi.types.ScType;
import org.jetbrains.plugins.scala.lang.psi.types.api.StdType;
import org.jetbrains.plugins.scala.lang.psi.types.result.Typeable;
import scala.Option;
import scala.collection.Seq;
import scala.util.Either;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Date: 08/12/2017
 *
 * @author Yaron Yamin
 */
public class ScalaPsiTreeUtils {
    private static final Logger LOG = Logger.getInstance(ScalaPsiTreeUtils.class.getName());
    public static PsiParameter[] resolveParameters(PsiMethod psiMethod) {
        PsiParameter[] psiParameters = null;
        if (psiMethod instanceof ScPrimaryConstructorWrapper) { //todo revise implementation
            final ScPrimaryConstructorWrapper scPrimaryConstructorWrapper = (ScPrimaryConstructorWrapper) psiMethod;
//            final ScPrimaryConstructor scPrimaryConstructor = scPrimaryConstructorWrapper.constr();
            ScPrimaryConstructor scPrimaryConstructor = resolvePrimaryConstructor(scPrimaryConstructorWrapper);
            if (scPrimaryConstructor != null) {
                final Seq<ScClassParameter> scClassParameterSeq = scPrimaryConstructor.effectiveFirstParameterSection();
                int len = scClassParameterSeq.length();
                psiParameters = new PsiParameter[len];
                for (int i = 0; i < len; i++) {
                    psiParameters[i] = scClassParameterSeq.apply(i);
                }
            }
        }
        else if (psiMethod instanceof ScFunctionWrapper) {
//            final ScFunction function = ((ScFunctionWrapper) psiMethod).function();
            final ScFunction function = resolveFunction(((ScFunctionWrapper) psiMethod));
            if (function != null) {
                final int length = function.parameters().length();
                psiParameters = new PsiParameter[length];
                for (int i = 0; i < length; i++) {
                    psiParameters[i] = function.parameters().apply(i);
                }
            }
        }
        if(psiParameters==null){
            psiParameters = psiMethod.getParameterList().getParameters();
        }
        return psiParameters;
    }

    /**
     * get ScPrimaryConstructor from ScPrimaryConstructorWrapper by reflection since method constr() has been renamed in succeeding versions to delegate()
     */
    @Nullable
    private static ScPrimaryConstructor resolvePrimaryConstructor(ScPrimaryConstructorWrapper object) {
        return getReturnTypeReflective(object, ScPrimaryConstructorWrapper.class, ScPrimaryConstructor.class);
    }
    @Nullable
    private static ScFunction resolveFunction(ScFunctionWrapper object) {
        return getReturnTypeReflective(object, ScFunctionWrapper.class, ScFunction.class);
    }

    @Nullable
    private static <U,T> T  getReturnTypeReflective(Object object, Class<U> ownerClass, Class<T> returnClass) {
        T returnInstance = null;
        try {
            Method delegateMethod = null;
            for (Method method : ownerClass.getDeclaredMethods()) {
                final Class<?>[] parameters = method.getParameterTypes();
                if (method.getReturnType()!=null && returnClass.isAssignableFrom( method.getReturnType()) && (parameters == null || parameters.length == 0)) {
                    delegateMethod = method;
                }
            }
            if (delegateMethod != null) {
                delegateMethod.setAccessible(true);
                final Object obj = delegateMethod.invoke(object);
                if (obj != null && returnClass.isInstance(obj)) {
                    returnInstance = (T) obj;
                }
            }

        } catch (Exception e) {
            LOG.error("Failed to invoke a method returning "+ returnClass.getSimpleName()+" on type "+ownerClass.getSimpleName(), e);
        }
        if (returnInstance == null) {
            LOG.warn("Method returning "+ returnClass.getSimpleName()+" not found on type "+ownerClass.getSimpleName());
        }
        return returnInstance;
    }

    public static Object resolveRelatedTypeElement(PsiParameter psiParameter) {
        if (psiParameter instanceof ScClassParameter) {
            final PsiType type = psiParameter.getType();
            final Option<ScTypeElement> scTypeElementOption = ((ScClassParameter) psiParameter).typeElement();
            if (!scTypeElementOption.isEmpty()) {
                return scTypeElementOption.get();
            }
        } else if (psiParameter instanceof ScParameter) {
            final ScParameter scParameter = (ScParameter) psiParameter;
//            final TypeResult<ScType> typeResult = scParameter.getRealParameterType(scParameter.getRealParameterType$default$1());
            return extractScType(scParameter);
        }
        return null;
    }

    private static PsiClass findClass(String qualifiedName, Module module, Project project) {
        PsiClass aClass;
        if (module != null) {
            aClass = JavaPsiFacade.getInstance(project).findClass(qualifiedName, GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module));
        } else {
            aClass = JavaPsiFacade.getInstance(project).findClass(qualifiedName, GlobalSearchScope.everythingScope(project));
        }
        return aClass;
    }

    private static List<PsiClass> resolveComposedTypes(PsiType psiType, PsiElement typePsiElement) {
        final List<PsiClass> psiClasses = new ArrayList<PsiClass>();
        if (typePsiElement instanceof ScParameterizedTypeElement) {
            String canonicalName = resolveParameterizedCanonicalName(typePsiElement);
            final ArrayList<String> genericTypes = GenericsExpressionParser.extractGenericTypes(canonicalName);
            for (String genericType : genericTypes) {
                if (genericType.length() > 0) {
                    PsiClass aClass = resolvePsiClass(typePsiElement, genericType);
                    if (aClass != null) {
                        psiClasses.add(aClass);
                    }
                }
            }
        }
        return psiClasses;
    }

    private static PsiClass resolvePsiClass(PsiElement typePsiElement, String genericType) {
        return resolvePsiClass(genericType, typePsiElement.getProject(), ModuleUtilCore.findModuleForPsiElement(typePsiElement));
    }

    @Nullable
    public static PsiClass resolvePsiClass(String qualifiedName, Project project, Module module) {
        PsiClass aClass = findClass(qualifiedName, module, project);
//        if (aClass == null && !qualifiedName.contains(".")) {//todo - consider removing . fallback may not be necessary anymore
//            final String scalaLangTypeName = "scala." + qualifiedName;
//            aClass = findClass(scalaLangTypeName, module, project);
//        }
        return aClass;
    }

    public static String resolveParameterizedCanonicalName(PsiElement typePsiElement) {
        String canonicalText = null;
        if (typePsiElement instanceof ScParameterizedTypeElement) {
            final ScParameterizedTypeElement parameterizedTypeElement = (ScParameterizedTypeElement) typePsiElement; //todo find alternative solution - no supported in future scala plugin versions
            final ScType scType = extractScType(parameterizedTypeElement);
            if (scType == null) {
                return null;
            } else {

                canonicalText = scType.canonicalText();
                final String sanitizedRoot = stripRootPrefixFromScalaCanonicalName(canonicalText);
                return normalizeGenericsRepresentation(sanitizedRoot);
            }
        } else if (typePsiElement instanceof ScClass) {
            final ScClass scClass = (ScClass) typePsiElement;
            final ScType scType = extractScType(scClass);
            if (scType!=null) {
                canonicalText = scType.canonicalText();
            }
        }
        if (null != canonicalText) {
            final String sanitizedRoot = stripRootPrefixFromScalaCanonicalName(canonicalText);
            return normalizeGenericsRepresentation(sanitizedRoot);
        } else {
            return null;
        }
    }

    public static String normalizeGenericsRepresentation(String sanitizedRoot) {
        return sanitizedRoot.replaceAll("\\[", "<").replaceAll("]", ">");
    }

    public static String resolveCanonicalNameOfObject(Object typeElement) {
        String canonicalText = null;
        if (typeElement instanceof ScParameterizedType) {
            final ScParameterizedType scParameterizedType = (ScParameterizedType) typeElement;
            canonicalText = scParameterizedType.canonicalText();
            final String designatorCanonicalText = scParameterizedType.designator().canonicalText();
            if (canonicalText.startsWith("(") && canonicalText.endsWith(")")) {
                canonicalText = designatorCanonicalText + ("<"+canonicalText.substring(1, canonicalText.length() - 2)+">");
            }
        } else if(typeElement instanceof ScType){
            final ScType scType = (ScType) typeElement;
            if (scType instanceof StdType) {
                canonicalText = ((StdType) scType).fullName();
            } else {
                canonicalText = scType.canonicalText();
            }
        }
        canonicalText = stripRootPrefixFromScalaCanonicalName(canonicalText);
        return normalizeGenericsRepresentation(canonicalText);
    }

    public static String stripRootPrefixFromScalaCanonicalName(String canonicalText) {
        return canonicalText.replaceAll("_root_.", "");
    }


    public static List<Object> resolveComposedTypeElementsForObject(PsiType psiType, Object typeElement) {
        ArrayList<Object> typeElements= new ArrayList<Object>();
        Seq<ScType> scTypeSeq = null;
        if (typeElement instanceof ScParameterizedType) {
            final ScParameterizedType scParameterizedType = (ScParameterizedType) typeElement;
            scTypeSeq = scParameterizedType.typeArguments();
        }
        else if(typeElement instanceof ScParameterizedTypeElement){
            final ScParameterizedTypeElement scParameterizedTypeElement = (ScParameterizedTypeElement) typeElement;
            ScType scType = extractScType(scParameterizedTypeElement);
            if (scType instanceof ScParameterizedType) {
                scTypeSeq = ((ScParameterizedType) scType).typeArguments();
            }
        }
        if (scTypeSeq != null) {
            for (int i = 0; i < scTypeSeq.length(); i++) {
                final ScType scType = scTypeSeq.apply(i);
                typeElements.add(scType);
            }
        }
        else if (typeElement instanceof PsiElement) {//todo - consider removing . fallback may not be necessary anymore
            final List<PsiClass> psiClasses = resolveComposedTypes(psiType, ((PsiElement) typeElement));
            final ArrayList<Object> objects = new ArrayList<>();
            objects.addAll(psiClasses);
            typeElements = objects;
        }
        return typeElements;
    }

    @Nullable
    private static ScType extractScType(Typeable typeable) {
        try {
            final Class<?> typingContextClass = Class.forName("org.jetbrains.plugins.scala.lang.psi.types.result.TypingContext");
            final Object typingContext = getReturnTypeReflective(typeable, Typeable.class, typingContextClass);
            if (typingContext != null) {
                final Method getTypeMethod = typeable.getClass().getMethod("getType", typingContextClass);
                final Object result = getTypeMethod.invoke(typeable, typingContext);
                if (result != null && result.getClass().getCanonicalName().equals("org.jetbrains.plugins.scala.lang.psi.types.result.Success")) {
                    final Method getMethod = result.getClass().getMethod("get");
                    if (getMethod != null) {
                        final Object type = getMethod.invoke(result);
                        if (type instanceof ScType) {
                            return (ScType) type;
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.info("could not find Typeable.getType(TypingContext) method. this seams to be an advanced installation of idea scala plugin", e);
        }

        try {
            final Object returnTypeReflective = getReturnTypeReflective(typeable, Typeable.class, Class.forName("scala.util.Either"));
            if (returnTypeReflective instanceof Either) {
                final Either returnTypeReflectiveEither = (Either) returnTypeReflective;
                if (((Either) returnTypeReflective).isRight()) {
                    final Object scTypeObj = returnTypeReflectiveEither.right().get();
                    if (scTypeObj instanceof ScType) {
                        return (ScType) scTypeObj;
                    }
                }
            }
        } catch (Exception e) {
            LOG.info("could not find Typeable.type(Either) method. this seams to be an an old installation of idea scala plugin", e);
        }
        //Deprecated api since 2017.x:
//        final org.jetbrains.plugins.scala.lang.psi.types.result.TypeResult<ScType> typeResult = typeable.getType(typeable.getType$default$1());
//        if (!typeResult.isEmpty()) {
//            scType = typeResult.get();
//        }
        return null;
    }
}
package com.weirddev.testme.intellij.template.context;

import com.intellij.openapi.module.Module;
import com.weirddev.testme.intellij.template.FileTemplateConfig;
import com.weirddev.testme.intellij.template.LangTestBuilderFactory;
import com.weirddev.testme.intellij.template.TypeDictionary;

import java.util.List;
import java.util.Map;

/**
 * Date: 22/04/2017
 *
 * @author Yaron Yamin
 */
public class TestBuilderImpl implements TestBuilder{

    private final LangTestBuilderFactory langTestBuilderFactory;

    public TestBuilderImpl(Language language, Module srcModule, TypeDictionary typeDictionary, FileTemplateConfig fileTemplateConfig) {
        langTestBuilderFactory = new LangTestBuilderFactory(language, srcModule, fileTemplateConfig,typeDictionary);
    }

    @Override
    public String renderMockMvcMethodCall(Method method, Type type, Map<String, String> replacementTypes, Map<String, String> defaultTypeValues) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append(method.getHttpOperation()).append("(\"").append(type.getMockMvcUrl()).append(method.getMockMvcUrl()).append("\"");

        List<Param> methodParams = method.getMethodParams();
        for (Param param : methodParams) {
            if (param.isPathVariable()) {
                sb.append(",").append(param.getName());
            }
        }

        sb.append(")\n");

        for (Param param : methodParams) {
            if (param.isRequestBody()) {
                sb.append(".contentType(MediaType.APPLICATION_JSON_UTF8)\n");
                sb.append(".content(objectMapper.writeValueAsString(new ").append(param.getType().getCanonicalName()).append("()))\n");
            }else if (param.isRequestParam()) {
                sb.append(".param(").append(param.getRequestName()).append(",").append(param.getName()).append(")\n");
            } else if (param.isRequestHeader()) {
                sb.append(".header(").append(param.getRequestName()).append(",").append(param.getName()).append(")\n");
            }
        }

        sb.append(".accept(MediaType.APPLICATION_JSON))");

        return sb.toString();
    }

    @Override
    public String renderMethodParams(Method method, Map<String, String> replacementTypes, Map<String, String> defaultTypeValues) throws Exception {
        return langTestBuilderFactory.createTestBuilder(method, ParamRole.Input).renderJavaCallParams(method.getMethodParams(), replacementTypes, defaultTypeValues);
    }

    @Override
    public ParameterizedTestComponents buildPrameterizedTestComponents(Method method, Map<String, String> replacementTypesForReturn, Map<String, String> replacementTypes, Map<String, String> defaultTypeValues) throws Exception {
        //a temp solution for single dimmension parameters
        final LangTestBuilder testBuilder = langTestBuilderFactory.createTestBuilder(method, ParamRole.Input);
        final ParameterizedTestComponents parameterizedTestComponents = new ParameterizedTestComponents();
        StringBuilder sb=new StringBuilder();
        for (Param param : method.getMethodParams()) {
            final String value = testBuilder.renderJavaCallParam(param.getType(), param.getName(), replacementTypes, defaultTypeValues);
            sb.append(param.getName()).append(", ");
            parameterizedTestComponents.getParamsMap().put(param.getName(), value);
        }
        if (sb.length() > 0) {
            sb.delete(sb.length() - LangTestBuilder.PARAMS_SEPARATOR.length(),sb.length());
        }

        if (method.hasReturn()) {
            final String value = testBuilder.renderJavaCallParam(method.getReturnType(), RESULT_VARIABLE_NAME, replacementTypesForReturn, defaultTypeValues);
            parameterizedTestComponents.getParamsMap().put(RESULT_VARIABLE_NAME, value);
        }
        parameterizedTestComponents.setMethodClassParamsStr(sb.toString());
        return parameterizedTestComponents;
    }

    @Override
    public String renderReturnParam(Method testedMethod, Type type, String defaultName, Map<String, String> replacementTypes, Map<String, String> defaultTypeValues) throws Exception {
        return langTestBuilderFactory.createTestBuilder(testedMethod, ParamRole.Output).renderJavaCallParam(type,defaultName,replacementTypes, defaultTypeValues);
    }
    @Override
    public String renderInitType(Type type, String defaultName, Map<String, String> replacementTypes, Map<String, String> defaultTypeValues) throws Exception {
        return langTestBuilderFactory.createTestBuilder(null,null).renderJavaCallParam(type,defaultName,replacementTypes, defaultTypeValues);
    }
}

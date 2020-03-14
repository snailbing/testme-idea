package com.weirddev.testme.intellij.template.context;

import java.util.Map;

/**
 * Date: 22/04/2017
 *
 * @author Yaron Yamin
 */
@SuppressWarnings("unused")
public interface TestBuilder {
    String RESULT_VARIABLE_NAME = "expectedResult";

    String renderMockMvcParamOptionalCall(Method method, Type type, Map<String, String> replacementTypes, Map<String, String> defaultTypeValues) throws Exception;

    String renderMockMvcMethodCall(Method method, Type type, Map<String, String> replacementTypes, Map<String, String> defaultTypeValues) throws Exception;

    String renderMethodParams(Method method, Map<String, String> replacementTypes, Map<String, String> defaultTypeValues) throws Exception;

    ParameterizedTestComponents buildPrameterizedTestComponents(Method method, Map<String, String> replacementTypesForReturn, Map<String, String> replacementTypes, Map<String, String> defaultTypeValues) throws Exception;

    String renderReturnParam(Method testedMethod, Type type, String defaultName, Map<String, String> replacementTypes, Map<String, String> defaultTypeValues) throws Exception;

    String renderInitType(Type type, String defaultName, Map<String, String> replacementTypes, Map<String, String> defaultTypeValues) throws Exception;
    enum ParamRole {
        Input, Output
    }
}

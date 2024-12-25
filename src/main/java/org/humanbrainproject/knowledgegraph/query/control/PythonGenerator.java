/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This open source software code was developed in part or in whole in the
 * Human Brain Project, funded from the European Union's Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 *
 */

package org.humanbrainproject.knowledgegraph.query.control;

import org.apache.commons.lang3.StringUtils;
import org.humanbrainproject.knowledgegraph.commons.labels.SemanticsToHumanTranslator;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.entity.PythonClass;
import org.humanbrainproject.knowledgegraph.query.entity.PythonField;
import org.humanbrainproject.knowledgegraph.query.entity.SpecField;
import org.humanbrainproject.knowledgegraph.query.entity.Specification;
import org.humanbrainproject.knowledgegraph.query.entity.fieldFilter.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class PythonGenerator {

    private static Pattern PARAMETER_PATTERN = Pattern.compile("^\\$\\{(.+)\\}$");

    @Autowired
    SemanticsToHumanTranslator semanticsToHumanTranslator;

    public String generate(NexusSchemaReference schemaReference, Specification specification) {
        StringBuilder sb = new StringBuilder();
        sb.append("from typing import Sequence\n");
        sb.append("\n");
        sb.append("from kgquery.queryApi import Query, KGClient");
        sb.append("\n");
        sb.append("\n");

        List<PythonClass> pythonClasses = new ArrayList<>();
        List<Parameter> filterParameters = new ArrayList<>();

        extractPythonClasses(pythonClasses, specification.getFields(), filterParameters, schemaReference.getSchema());



        for (PythonClass pythonClass : pythonClasses) {
            sb.append("\nclass %s:\n".formatted(pythonClass.getName()));
            for (PythonField pythonField : pythonClass.getFields()) {
                sb.append("    %s: %s\n".formatted(pythonField.getName(), pythonField.getType() != null ? "Sequence[%s]".formatted(pythonField.getType()) : "any"));
            }
            sb.append("\n");
            String fieldName = getPythonFieldName(pythonClass.getName());
            sb.append("\ndef _%s_from_payload(payload: dict) -> %s:\n".formatted(fieldName, pythonClass.getName()));
            sb.append("    %s = %s()".formatted(fieldName, pythonClass.getName()));
            for (PythonField pythonField : pythonClass.getFields()) {
                if(pythonField.getType()==null) {
                    sb.append("\n    %s.%s = payload[\"%s\"] if \"%s\" in payload else None".formatted(fieldName, pythonField.getName(), pythonField.getKey(), pythonField.getKey()));
                }
                else{
                    sb.append("\n    %s.%s = []\n".formatted(fieldName, pythonField.getName()));
                    sb.append("    if \"%s\" in payload and isinstance(payload[\"%s\"], list):\n".formatted(pythonField.getKey(), pythonField.getKey()));
                    sb.append("        for c in payload[\"%s\"]:\n".formatted(pythonField.getKey()));
                    sb.append(("            %s.%s.append(_%s_from_payload(c))" +
                            "").formatted(fieldName, pythonField.getName(), semanticsToHumanTranslator.simplePluralToSingular(pythonField.getName()).toLowerCase()));
                }
            }
            sb.append("\n    return %s".formatted(fieldName));
            sb.append("\n\n");
        }

        List<String> filterParametersPython = filterParameters.stream().map(p -> getPythonFieldName(p.getName())).collect(Collectors.toList());
        String filterParametersPythonWithNone = filterParametersPython.isEmpty() ? "" : ", "+String.join(", ", filterParametersPython.stream().map(f -> f+"=None").collect(Collectors.toList()));


        String rootClassName = StringUtils.capitalize(semanticsToHumanTranslator.simplePluralToSingular(schemaReference.getSchema()));

        sb.append("\nclass %s(Query[%s]):\n\n".formatted(StringUtils.capitalize(specification.getSpecificationId()) + rootClassName, rootClassName));
        sb.append("    def __init__(self, client: KGClient%s):\n".formatted(filterParametersPythonWithNone));
        sb.append("        super().__init__(client, \"%s\", \"%s\")\n".formatted(schemaReference.getRelativeUrl().getUrl(), specification.getSpecificationId()));
        for (String filterParameter : filterParametersPython) {
            sb.append("        self._%s = %s\n".formatted(filterParameter, filterParameter));
        }
        sb.append("\n");
        sb.append("    def create_result(self, payload: dict) -> %s:\n".formatted(rootClassName));
        sb.append("        return _%s_from_payload(payload)\n".formatted(rootClassName.toLowerCase()));
        sb.append("\n");
        sb.append("    def create_filter_params(self) -> str:\n".formatted(rootClassName));
        StringBuilder queryConcat = new StringBuilder();
        sb.append("        filter = \"\"\n");
        for(int i=0; i<filterParameters.size(); i++){
            sb.append("        if self._%s is not None:\n".formatted(filterParametersPython.get(i)));
            sb.append("                filter = filter + \"&%s=\" + self._%s\n".formatted(filterParameters.get(i).getName(), filterParametersPython.get(i)));
        }
        sb.append("        return filter\n\n");

        return sb.toString();

    }



    public void extractPythonClasses(List<PythonClass> classes, List<SpecField> fields, List<Parameter> filterParameters, String name) {
        if (!fields.isEmpty()) {
            List<PythonField> pythonFields = new ArrayList<>();
            for (SpecField field : fields) {
                String pythonFieldName = getPythonFieldName(field.fieldName);
                List<SpecField> fieldsToBeProcessed = field.fields;
                if(field.fieldFilter!=null && field.fieldFilter.getParameter()!=null) {
                    filterParameters.add(field.fieldFilter.getParameter());
                }
                if(field.isMerge()){
                    fieldsToBeProcessed = new ArrayList<>();
                    Set<String> handledFields = new HashSet<>();
                    for (SpecField specField : field.fields) {
                        for (SpecField subfield : specField.fields) {
                            if(!handledFields.contains(subfield.fieldName)){
                                fieldsToBeProcessed.add(subfield);
                                handledFields.add(subfield.fieldName);
                                if(subfield.fieldFilter!=null && subfield.fieldFilter.getParameter()!=null){
                                    filterParameters.add(subfield.fieldFilter.getParameter());
                                }
                            }
                        }
                    }
                }
                extractPythonClasses(classes, fieldsToBeProcessed, filterParameters, semanticsToHumanTranslator.extractSimpleAttributeName(field.fieldName));
                pythonFields.add(new PythonField(pythonFieldName, fieldsToBeProcessed.isEmpty() ? null : StringUtils.capitalize(semanticsToHumanTranslator.simplePluralToSingular(semanticsToHumanTranslator.extractSimpleAttributeName(field.fieldName))), field.fieldName));
            }
            PythonClass python = new PythonClass(StringUtils.capitalize(semanticsToHumanTranslator.simplePluralToSingular(name)), pythonFields);
            classes.add(python);
        }
    }


    private String getPythonFieldName(String fieldName) {
        String label = semanticsToHumanTranslator.extractSimpleAttributeName(fieldName);
        return label.replaceAll("[^\\sA-Za-z0-9]", "").replaceAll("(.)(\\p{Upper})", "$1_$2").toLowerCase();

    }


}

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

package org.humanbrainproject.knowledgegraph.query.entity;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.ArangoVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.NexusVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.SchemaOrgVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.entity.Alternative;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.AbsoluteNexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;

import java.util.*;
import java.util.function.Consumer;

@ToBeTested
public class JsonDocument extends LinkedHashMap<String, Object>{

    public JsonDocument(Map<? extends String, ?> map) {
        super(map);
    }

    public JsonDocument() {
    }

    public void addReference(String propertyName, String url){
        Map<String, String> reference = new HashMap<>();
        reference.put(JsonLdConsts.ID, url);
        addToProperty(propertyName, reference);
    }

    public JsonDocument addToProperty(String propertyName, Object value){
        addToProperty(propertyName, value, this);
        return this;
    }

    public boolean isOfType(String lookupType){
        Object type = get(JsonLdConsts.TYPE);
        if(type!=null && lookupType!=null){
            if(type instanceof String){
                return type.equals(lookupType);
            }
            else if(type instanceof Collection<?> collection){
                return collection.contains(lookupType);
            }
        }
        return false;
    }


    public void addType(String type){
        addToProperty(JsonLdConsts.TYPE, type);
    }

    public String getPrimaryIdentifier(){
        if(this.containsKey(SchemaOrgVocabulary.IDENTIFIER)){
            Object identifier = get(SchemaOrgVocabulary.IDENTIFIER);
            if(identifier instanceof List<?> list && !list.isEmpty()){
                for (Object o : list) {
                    if(o instanceof String string){
                        return string;
                    }
                }
            }
            else if(identifier instanceof String string){
                return string;
            }
        }
        return getNexusId();
    }

    public Integer getNexusRevision(){
        if(this.containsKey(NexusVocabulary.REVISION_ALIAS)){
            return (Integer)get(NexusVocabulary.REVISION_ALIAS);
        }
        else if(this.containsKey(ArangoVocabulary.NEXUS_REV)){
            Long rev = (Long) get(ArangoVocabulary.NEXUS_REV);
            return rev!=null ? rev.intValue() : null;
        }
        return null;
    }

    public String getNexusId(){
        return (String) get(ArangoVocabulary.NEXUS_UUID);
    }



    public NexusInstanceReference getReference(){
        if (this.containsKey(JsonLdConsts.ID)) {
            NexusInstanceReference fromUrl = NexusInstanceReference.createFromUrl((String) get(JsonLdConsts.ID));
            fromUrl.setRevision((Integer) get(NexusVocabulary.REVISION_ALIAS));
            return fromUrl;
        }
        return null;
    }


    public void addAlternative(String propertyName, Alternative value){
        Map<String, Object> alternatives = (Map<String, Object>)get(HBPVocabulary.INFERENCE_ALTERNATIVES);
        if(alternatives==null){
            alternatives = new LinkedHashMap<>();
            put(HBPVocabulary.INFERENCE_ALTERNATIVES, alternatives);
        }
        Object v;
        if(alternatives.isEmpty()){
            v = new ArrayList<Alternative>();
            ((List) v).add(value);
        }else{
            v = value;
        }
        if(!value.getUserIds().isEmpty() && !value.getUserIds().stream().allMatch(Objects::isNull)){
            addToProperty(propertyName, v, alternatives);
        }
    }

    private static void addToProperty(String propertyName, Object value, Map map){
        Object o = map.get(propertyName);
        if(o==null){
            map.put(propertyName, value);
        }
        else if(o instanceof Collection<?> collection){
            if(!collection.contains(value)) {
                collection.add(value);
            }
        }
        else if(!o.equals(value)){
            List<Object> list = new ArrayList<>();
            list.add(o);
            list.add(value);
            map.put(propertyName, list);
        }
    }


    public JsonDocument removeAllInternalKeys(){
        this.keySet().removeIf(k -> k.startsWith("_"));
        return this;
    }


    public void processLinks(Consumer<Map> referenceConsumer){
        processLinks(referenceConsumer, this, true);
    }

    private void processLinks(Consumer<Map> referenceConsumer, Map currentMap, boolean root){
        //Skip root-id
        if(!root && currentMap.containsKey(JsonLdConsts.ID)){
            Object id = currentMap.get(JsonLdConsts.ID);
            if(id!=null){
                referenceConsumer.accept(currentMap);
            }
        }
        else {
            for (Object key : currentMap.keySet()) {
                Object value = currentMap.get(key);
                if(value instanceof Map<?,?> map){
                    processLinks(referenceConsumer, map, false);
                }
            }
        }
    }

    public void replaceNamespace(String oldNamespace, String newNamespace){
        replaceNamespace(oldNamespace, newNamespace, this);
    }

    private void replaceNamespace(String oldNamespace, String newNamespace, Map currentMap){
        HashSet keyList = new HashSet<>(currentMap.keySet());
        for (Object key : keyList) {
            if(key instanceof String string){
                if(string.startsWith(oldNamespace)){
                    Object value = currentMap.remove(key);
                    if(value instanceof Map<?,?> map){
                        replaceNamespace(oldNamespace, newNamespace, map);
                    }
                    currentMap.put(newNamespace+string.substring(oldNamespace.length()), value);
                }
            }
        }
    }



}

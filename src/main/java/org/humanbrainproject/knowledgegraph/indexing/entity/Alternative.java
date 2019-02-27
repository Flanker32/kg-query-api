package org.humanbrainproject.knowledgegraph.indexing.entity;


import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;

import java.util.LinkedHashMap;
import java.util.Set;

public class Alternative extends LinkedHashMap<String, Object> {

    public Alternative(Object value,  Set<String> userIds) {
        this.put(HBPVocabulary.INFERENCE_ALTERNATIVES_VALUE, value);
        this.put(HBPVocabulary.INFERENCE_ALTERNATIVES_USERIDS, userIds);
    }

    public Object getValue() {
        return this.get(HBPVocabulary.INFERENCE_ALTERNATIVES_VALUE);
    }

    public Set<String> getUserIds() {
        return (Set<String>)this.get(HBPVocabulary.INFERENCE_ALTERNATIVES_USERIDS);
    }
}
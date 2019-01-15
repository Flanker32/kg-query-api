package org.humanbrainproject.knowledgegraph.query.entity;

import org.humanbrainproject.knowledgegraph.annotations.NoTests;

@NoTests(NoTests.NO_LOGIC)
public class StoredLibraryReference {

    private final String name;

    private final String template;

    public StoredLibraryReference(String name, String template) {
        this.name = name;
        this.template = template;
    }

    public String getName() {
        return name;
    }

    public String getTemplate() {
        return template;
    }
}

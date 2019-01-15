package org.humanbrainproject.knowledgegraph.commons.vocabulary;

import org.humanbrainproject.knowledgegraph.annotations.NoTests;

@NoTests(NoTests.NO_LOGIC)
public class HBPVocabulary {

    public static final String ALIAS = "hbp";
    public static final String NAMESPACE = "https://schema.hbp.eu/";
    public static final String GRAPH_QUERY = NAMESPACE+"graphQuery/";
    public static final String RELATIVE_URL_OF_INTERNAL_LINK = NAMESPACE+"relativeUrl";

    // FOR LINKING
    public static final String LINKING_INSTANCE_TYPE = NAMESPACE+"LinkingInstance";
    public static final String LINKING_INSTANCE_FROM = LINKING_INSTANCE_TYPE.toLowerCase()+"/from";
    public static final String LINKING_INSTANCE_TO = LINKING_INSTANCE_TYPE.toLowerCase()+"/to";


    // FOR PROVENANCE
    private static final String PROVENANCE = NAMESPACE+"provenance/";
    public static final String PROVENANCE_MODIFIED_AT = PROVENANCE + "modifiedAt";
    public static final String PROVENANCE_CREATED_AT = PROVENANCE + "createdAt";
    public static final String PROVENANCE_LAST_MODIFICATION_USER_ID = PROVENANCE + "lastModificationUserId";
    public static final String PROVENANCE_REVISION = PROVENANCE + "revision";
    public static final String PROVENANCE_IMMEDIATE_INDEX = PROVENANCE + "immediateIndex";
    public static final String PROVENANCE_CREATED_BY = PROVENANCE + "createdBy";


    // FOR RELEASING
    public static final String RELEASE_TYPE = HBPVocabulary.NAMESPACE + "Release";
    public static final String RELEASE_INSTANCE = RELEASE_TYPE.toLowerCase() + "/instance";
    public static final String RELEASE_REVISION = RELEASE_TYPE.toLowerCase() + "/revision";
    public static final String RELEASE_STATE = RELEASE_TYPE.toLowerCase() + "/state";


    //FOR INFERENCE
    public final static String INFERENCE_TYPE = HBPVocabulary.NAMESPACE + "Inference";
    public final static String INFERENCE_SOURCE = INFERENCE_TYPE.toLowerCase() + "/source";
    public final static String INFERENCE_OF = INFERENCE_TYPE.toLowerCase()+"/inferenceOf";

    /**
     * declares the relationship of e.g. an editor instance which extends another (original) entity
     */
    public final static String INFERENCE_EXTENDS = INFERENCE_TYPE.toLowerCase()+"/extends";
    public final static String INFERENCE_ALTERNATIVES = INFERENCE_TYPE.toLowerCase()+"/alternatives";


    //FOR SPATIAL
    public static final String SPATIAL_TYPE = HBPVocabulary.NAMESPACE+ "SpatialAnchoring";
    public static final String SPATIAL_COORDINATES = SPATIAL_TYPE.toLowerCase()+"/coordinates";
    public static final String SPATIAL_REFERENCESPACE =  SPATIAL_TYPE.toLowerCase()+ "/referenceSpace";
    public static final String SPATIAL_LOCATED_INSTANCE =  SPATIAL_TYPE.toLowerCase()+"/locatedInstance";


}

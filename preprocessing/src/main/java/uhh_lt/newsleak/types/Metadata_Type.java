package uhh_lt.newsleak.types;

/* First created by JCasGen Wed Nov 22 15:48:08 CET 2017 */

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.Feature;
import org.apache.uima.jcas.tcas.DocumentAnnotation_Type;

/** 
 * Updated by JCasGen Wed Apr 04 11:24:03 CEST 2018
 * @generated */
public class Metadata_Type extends DocumentAnnotation_Type {
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = Metadata.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("uhh_lt.newsleak.types.Metadata");
 
  /** @generated */
  final Feature casFeat_metaTripletsNames;
  /** @generated */
  final int     casFeatCode_metaTripletsNames;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getMetaTripletsNames(int addr) {
        if (featOkTst && casFeat_metaTripletsNames == null)
      jcas.throwFeatMissing("metaTripletsNames", "uhh_lt.newsleak.types.Metadata");
    return ll_cas.ll_getRefValue(addr, casFeatCode_metaTripletsNames);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setMetaTripletsNames(int addr, int v) {
        if (featOkTst && casFeat_metaTripletsNames == null)
      jcas.throwFeatMissing("metaTripletsNames", "uhh_lt.newsleak.types.Metadata");
    ll_cas.ll_setRefValue(addr, casFeatCode_metaTripletsNames, v);}
    
   /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @return value at index i in the array 
   */
  public String getMetaTripletsNames(int addr, int i) {
        if (featOkTst && casFeat_metaTripletsNames == null)
      jcas.throwFeatMissing("metaTripletsNames", "uhh_lt.newsleak.types.Metadata");
    if (lowLevelTypeChecks)
      return ll_cas.ll_getStringArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_metaTripletsNames), i, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_metaTripletsNames), i);
  return ll_cas.ll_getStringArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_metaTripletsNames), i);
  }
   
  /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @param v value to set
   */ 
  public void setMetaTripletsNames(int addr, int i, String v) {
        if (featOkTst && casFeat_metaTripletsNames == null)
      jcas.throwFeatMissing("metaTripletsNames", "uhh_lt.newsleak.types.Metadata");
    if (lowLevelTypeChecks)
      ll_cas.ll_setStringArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_metaTripletsNames), i, v, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_metaTripletsNames), i);
    ll_cas.ll_setStringArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_metaTripletsNames), i, v);
  }
 
 
  /** @generated */
  final Feature casFeat_metaTripletsValues;
  /** @generated */
  final int     casFeatCode_metaTripletsValues;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getMetaTripletsValues(int addr) {
        if (featOkTst && casFeat_metaTripletsValues == null)
      jcas.throwFeatMissing("metaTripletsValues", "uhh_lt.newsleak.types.Metadata");
    return ll_cas.ll_getRefValue(addr, casFeatCode_metaTripletsValues);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setMetaTripletsValues(int addr, int v) {
        if (featOkTst && casFeat_metaTripletsValues == null)
      jcas.throwFeatMissing("metaTripletsValues", "uhh_lt.newsleak.types.Metadata");
    ll_cas.ll_setRefValue(addr, casFeatCode_metaTripletsValues, v);}
    
   /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @return value at index i in the array 
   */
  public String getMetaTripletsValues(int addr, int i) {
        if (featOkTst && casFeat_metaTripletsValues == null)
      jcas.throwFeatMissing("metaTripletsValues", "uhh_lt.newsleak.types.Metadata");
    if (lowLevelTypeChecks)
      return ll_cas.ll_getStringArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_metaTripletsValues), i, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_metaTripletsValues), i);
  return ll_cas.ll_getStringArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_metaTripletsValues), i);
  }
   
  /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @param v value to set
   */ 
  public void setMetaTripletsValues(int addr, int i, String v) {
        if (featOkTst && casFeat_metaTripletsValues == null)
      jcas.throwFeatMissing("metaTripletsValues", "uhh_lt.newsleak.types.Metadata");
    if (lowLevelTypeChecks)
      ll_cas.ll_setStringArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_metaTripletsValues), i, v, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_metaTripletsValues), i);
    ll_cas.ll_setStringArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_metaTripletsValues), i, v);
  }
 
 
  /** @generated */
  final Feature casFeat_metaTripletsTypes;
  /** @generated */
  final int     casFeatCode_metaTripletsTypes;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getMetaTripletsTypes(int addr) {
        if (featOkTst && casFeat_metaTripletsTypes == null)
      jcas.throwFeatMissing("metaTripletsTypes", "uhh_lt.newsleak.types.Metadata");
    return ll_cas.ll_getRefValue(addr, casFeatCode_metaTripletsTypes);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setMetaTripletsTypes(int addr, int v) {
        if (featOkTst && casFeat_metaTripletsTypes == null)
      jcas.throwFeatMissing("metaTripletsTypes", "uhh_lt.newsleak.types.Metadata");
    ll_cas.ll_setRefValue(addr, casFeatCode_metaTripletsTypes, v);}
    
   /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @return value at index i in the array 
   */
  public String getMetaTripletsTypes(int addr, int i) {
        if (featOkTst && casFeat_metaTripletsTypes == null)
      jcas.throwFeatMissing("metaTripletsTypes", "uhh_lt.newsleak.types.Metadata");
    if (lowLevelTypeChecks)
      return ll_cas.ll_getStringArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_metaTripletsTypes), i, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_metaTripletsTypes), i);
  return ll_cas.ll_getStringArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_metaTripletsTypes), i);
  }
   
  /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @param v value to set
   */ 
  public void setMetaTripletsTypes(int addr, int i, String v) {
        if (featOkTst && casFeat_metaTripletsTypes == null)
      jcas.throwFeatMissing("metaTripletsTypes", "uhh_lt.newsleak.types.Metadata");
    if (lowLevelTypeChecks)
      ll_cas.ll_setStringArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_metaTripletsTypes), i, v, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_metaTripletsTypes), i);
    ll_cas.ll_setStringArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_metaTripletsTypes), i, v);
  }
 
 
  /** @generated */
  final Feature casFeat_docId;
  /** @generated */
  final int     casFeatCode_docId;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getDocId(int addr) {
        if (featOkTst && casFeat_docId == null)
      jcas.throwFeatMissing("docId", "uhh_lt.newsleak.types.Metadata");
    return ll_cas.ll_getStringValue(addr, casFeatCode_docId);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setDocId(int addr, String v) {
        if (featOkTst && casFeat_docId == null)
      jcas.throwFeatMissing("docId", "uhh_lt.newsleak.types.Metadata");
    ll_cas.ll_setStringValue(addr, casFeatCode_docId, v);}
    
  
 
  /** @generated */
  final Feature casFeat_timestamp;
  /** @generated */
  final int     casFeatCode_timestamp;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getTimestamp(int addr) {
        if (featOkTst && casFeat_timestamp == null)
      jcas.throwFeatMissing("timestamp", "uhh_lt.newsleak.types.Metadata");
    return ll_cas.ll_getStringValue(addr, casFeatCode_timestamp);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setTimestamp(int addr, String v) {
        if (featOkTst && casFeat_timestamp == null)
      jcas.throwFeatMissing("timestamp", "uhh_lt.newsleak.types.Metadata");
    ll_cas.ll_setStringValue(addr, casFeatCode_timestamp, v);}
    
  
 
  /** @generated */
  final Feature casFeat_keyterms;
  /** @generated */
  final int     casFeatCode_keyterms;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getKeyterms(int addr) {
        if (featOkTst && casFeat_keyterms == null)
      jcas.throwFeatMissing("keyterms", "uhh_lt.newsleak.types.Metadata");
    return ll_cas.ll_getStringValue(addr, casFeatCode_keyterms);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setKeyterms(int addr, String v) {
        if (featOkTst && casFeat_keyterms == null)
      jcas.throwFeatMissing("keyterms", "uhh_lt.newsleak.types.Metadata");
    ll_cas.ll_setStringValue(addr, casFeatCode_keyterms, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public Metadata_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_metaTripletsNames = jcas.getRequiredFeatureDE(casType, "metaTripletsNames", "uima.cas.StringArray", featOkTst);
    casFeatCode_metaTripletsNames  = (null == casFeat_metaTripletsNames) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_metaTripletsNames).getCode();

 
    casFeat_metaTripletsValues = jcas.getRequiredFeatureDE(casType, "metaTripletsValues", "uima.cas.StringArray", featOkTst);
    casFeatCode_metaTripletsValues  = (null == casFeat_metaTripletsValues) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_metaTripletsValues).getCode();

 
    casFeat_metaTripletsTypes = jcas.getRequiredFeatureDE(casType, "metaTripletsTypes", "uima.cas.StringArray", featOkTst);
    casFeatCode_metaTripletsTypes  = (null == casFeat_metaTripletsTypes) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_metaTripletsTypes).getCode();

 
    casFeat_docId = jcas.getRequiredFeatureDE(casType, "docId", "uima.cas.String", featOkTst);
    casFeatCode_docId  = (null == casFeat_docId) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_docId).getCode();

 
    casFeat_timestamp = jcas.getRequiredFeatureDE(casType, "timestamp", "uima.cas.String", featOkTst);
    casFeatCode_timestamp  = (null == casFeat_timestamp) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_timestamp).getCode();

 
    casFeat_keyterms = jcas.getRequiredFeatureDE(casType, "keyterms", "uima.cas.String", featOkTst);
    casFeatCode_keyterms  = (null == casFeat_keyterms) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_keyterms).getCode();

  }
}



    
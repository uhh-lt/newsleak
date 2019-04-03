
/* First created by JCasGen Thu Jan 04 14:37:05 CET 2018 */
package de.unihd.dbs.uima.types.heideltime;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.Feature;
import org.apache.uima.jcas.tcas.Annotation_Type;

/** 
 * Updated by JCasGen Wed Apr 03 17:15:09 CEST 2019
 * @generated */
public class Event_Type extends Annotation_Type {
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = Event.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("de.unihd.dbs.uima.types.heideltime.Event");
 
  /** @generated */
  final Feature casFeat_filename;
  /** @generated */
  final int     casFeatCode_filename;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getFilename(int addr) {
        if (featOkTst && casFeat_filename == null)
      jcas.throwFeatMissing("filename", "de.unihd.dbs.uima.types.heideltime.Event");
    return ll_cas.ll_getStringValue(addr, casFeatCode_filename);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setFilename(int addr, String v) {
        if (featOkTst && casFeat_filename == null)
      jcas.throwFeatMissing("filename", "de.unihd.dbs.uima.types.heideltime.Event");
    ll_cas.ll_setStringValue(addr, casFeatCode_filename, v);}
    
  
 
  /** @generated */
  final Feature casFeat_sentId;
  /** @generated */
  final int     casFeatCode_sentId;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getSentId(int addr) {
        if (featOkTst && casFeat_sentId == null)
      jcas.throwFeatMissing("sentId", "de.unihd.dbs.uima.types.heideltime.Event");
    return ll_cas.ll_getIntValue(addr, casFeatCode_sentId);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setSentId(int addr, int v) {
        if (featOkTst && casFeat_sentId == null)
      jcas.throwFeatMissing("sentId", "de.unihd.dbs.uima.types.heideltime.Event");
    ll_cas.ll_setIntValue(addr, casFeatCode_sentId, v);}
    
  
 
  /** @generated */
  final Feature casFeat_tokId;
  /** @generated */
  final int     casFeatCode_tokId;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getTokId(int addr) {
        if (featOkTst && casFeat_tokId == null)
      jcas.throwFeatMissing("tokId", "de.unihd.dbs.uima.types.heideltime.Event");
    return ll_cas.ll_getIntValue(addr, casFeatCode_tokId);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setTokId(int addr, int v) {
        if (featOkTst && casFeat_tokId == null)
      jcas.throwFeatMissing("tokId", "de.unihd.dbs.uima.types.heideltime.Event");
    ll_cas.ll_setIntValue(addr, casFeatCode_tokId, v);}
    
  
 
  /** @generated */
  final Feature casFeat_eventId;
  /** @generated */
  final int     casFeatCode_eventId;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getEventId(int addr) {
        if (featOkTst && casFeat_eventId == null)
      jcas.throwFeatMissing("eventId", "de.unihd.dbs.uima.types.heideltime.Event");
    return ll_cas.ll_getStringValue(addr, casFeatCode_eventId);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setEventId(int addr, String v) {
        if (featOkTst && casFeat_eventId == null)
      jcas.throwFeatMissing("eventId", "de.unihd.dbs.uima.types.heideltime.Event");
    ll_cas.ll_setStringValue(addr, casFeatCode_eventId, v);}
    
  
 
  /** @generated */
  final Feature casFeat_eventInstanceId;
  /** @generated */
  final int     casFeatCode_eventInstanceId;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getEventInstanceId(int addr) {
        if (featOkTst && casFeat_eventInstanceId == null)
      jcas.throwFeatMissing("eventInstanceId", "de.unihd.dbs.uima.types.heideltime.Event");
    return ll_cas.ll_getIntValue(addr, casFeatCode_eventInstanceId);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setEventInstanceId(int addr, int v) {
        if (featOkTst && casFeat_eventInstanceId == null)
      jcas.throwFeatMissing("eventInstanceId", "de.unihd.dbs.uima.types.heideltime.Event");
    ll_cas.ll_setIntValue(addr, casFeatCode_eventInstanceId, v);}
    
  
 
  /** @generated */
  final Feature casFeat_aspect;
  /** @generated */
  final int     casFeatCode_aspect;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getAspect(int addr) {
        if (featOkTst && casFeat_aspect == null)
      jcas.throwFeatMissing("aspect", "de.unihd.dbs.uima.types.heideltime.Event");
    return ll_cas.ll_getStringValue(addr, casFeatCode_aspect);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setAspect(int addr, String v) {
        if (featOkTst && casFeat_aspect == null)
      jcas.throwFeatMissing("aspect", "de.unihd.dbs.uima.types.heideltime.Event");
    ll_cas.ll_setStringValue(addr, casFeatCode_aspect, v);}
    
  
 
  /** @generated */
  final Feature casFeat_modality;
  /** @generated */
  final int     casFeatCode_modality;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getModality(int addr) {
        if (featOkTst && casFeat_modality == null)
      jcas.throwFeatMissing("modality", "de.unihd.dbs.uima.types.heideltime.Event");
    return ll_cas.ll_getStringValue(addr, casFeatCode_modality);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setModality(int addr, String v) {
        if (featOkTst && casFeat_modality == null)
      jcas.throwFeatMissing("modality", "de.unihd.dbs.uima.types.heideltime.Event");
    ll_cas.ll_setStringValue(addr, casFeatCode_modality, v);}
    
  
 
  /** @generated */
  final Feature casFeat_polarity;
  /** @generated */
  final int     casFeatCode_polarity;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getPolarity(int addr) {
        if (featOkTst && casFeat_polarity == null)
      jcas.throwFeatMissing("polarity", "de.unihd.dbs.uima.types.heideltime.Event");
    return ll_cas.ll_getStringValue(addr, casFeatCode_polarity);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setPolarity(int addr, String v) {
        if (featOkTst && casFeat_polarity == null)
      jcas.throwFeatMissing("polarity", "de.unihd.dbs.uima.types.heideltime.Event");
    ll_cas.ll_setStringValue(addr, casFeatCode_polarity, v);}
    
  
 
  /** @generated */
  final Feature casFeat_tense;
  /** @generated */
  final int     casFeatCode_tense;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getTense(int addr) {
        if (featOkTst && casFeat_tense == null)
      jcas.throwFeatMissing("tense", "de.unihd.dbs.uima.types.heideltime.Event");
    return ll_cas.ll_getStringValue(addr, casFeatCode_tense);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setTense(int addr, String v) {
        if (featOkTst && casFeat_tense == null)
      jcas.throwFeatMissing("tense", "de.unihd.dbs.uima.types.heideltime.Event");
    ll_cas.ll_setStringValue(addr, casFeatCode_tense, v);}
    
  
 
  /** @generated */
  final Feature casFeat_token;
  /** @generated */
  final int     casFeatCode_token;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getToken(int addr) {
        if (featOkTst && casFeat_token == null)
      jcas.throwFeatMissing("token", "de.unihd.dbs.uima.types.heideltime.Event");
    return ll_cas.ll_getRefValue(addr, casFeatCode_token);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setToken(int addr, int v) {
        if (featOkTst && casFeat_token == null)
      jcas.throwFeatMissing("token", "de.unihd.dbs.uima.types.heideltime.Event");
    ll_cas.ll_setRefValue(addr, casFeatCode_token, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public Event_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_filename = jcas.getRequiredFeatureDE(casType, "filename", "uima.cas.String", featOkTst);
    casFeatCode_filename  = (null == casFeat_filename) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_filename).getCode();

 
    casFeat_sentId = jcas.getRequiredFeatureDE(casType, "sentId", "uima.cas.Integer", featOkTst);
    casFeatCode_sentId  = (null == casFeat_sentId) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_sentId).getCode();

 
    casFeat_tokId = jcas.getRequiredFeatureDE(casType, "tokId", "uima.cas.Integer", featOkTst);
    casFeatCode_tokId  = (null == casFeat_tokId) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_tokId).getCode();

 
    casFeat_eventId = jcas.getRequiredFeatureDE(casType, "eventId", "uima.cas.String", featOkTst);
    casFeatCode_eventId  = (null == casFeat_eventId) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_eventId).getCode();

 
    casFeat_eventInstanceId = jcas.getRequiredFeatureDE(casType, "eventInstanceId", "uima.cas.Integer", featOkTst);
    casFeatCode_eventInstanceId  = (null == casFeat_eventInstanceId) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_eventInstanceId).getCode();

 
    casFeat_aspect = jcas.getRequiredFeatureDE(casType, "aspect", "uima.cas.String", featOkTst);
    casFeatCode_aspect  = (null == casFeat_aspect) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_aspect).getCode();

 
    casFeat_modality = jcas.getRequiredFeatureDE(casType, "modality", "uima.cas.String", featOkTst);
    casFeatCode_modality  = (null == casFeat_modality) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_modality).getCode();

 
    casFeat_polarity = jcas.getRequiredFeatureDE(casType, "polarity", "uima.cas.String", featOkTst);
    casFeatCode_polarity  = (null == casFeat_polarity) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_polarity).getCode();

 
    casFeat_tense = jcas.getRequiredFeatureDE(casType, "tense", "uima.cas.String", featOkTst);
    casFeatCode_tense  = (null == casFeat_tense) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_tense).getCode();

 
    casFeat_token = jcas.getRequiredFeatureDE(casType, "token", "de.unihd.dbs.uima.types.heideltime.Token", featOkTst);
    casFeatCode_token  = (null == casFeat_token) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_token).getCode();

  }
}



    
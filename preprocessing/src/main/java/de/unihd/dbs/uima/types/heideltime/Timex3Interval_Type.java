
/* First created by JCasGen Thu Jan 04 14:37:05 CET 2018 */
package de.unihd.dbs.uima.types.heideltime;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.Feature;

/** 
 * Updated by JCasGen Thu Mar 22 16:33:35 CET 2018
 * @generated */
public class Timex3Interval_Type extends Timex3_Type {
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = Timex3Interval.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("de.unihd.dbs.uima.types.heideltime.Timex3Interval");
 
  /** @generated */
  final Feature casFeat_TimexValueEB;
  /** @generated */
  final int     casFeatCode_TimexValueEB;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getTimexValueEB(int addr) {
        if (featOkTst && casFeat_TimexValueEB == null)
      jcas.throwFeatMissing("TimexValueEB", "de.unihd.dbs.uima.types.heideltime.Timex3Interval");
    return ll_cas.ll_getStringValue(addr, casFeatCode_TimexValueEB);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setTimexValueEB(int addr, String v) {
        if (featOkTst && casFeat_TimexValueEB == null)
      jcas.throwFeatMissing("TimexValueEB", "de.unihd.dbs.uima.types.heideltime.Timex3Interval");
    ll_cas.ll_setStringValue(addr, casFeatCode_TimexValueEB, v);}
    
  
 
  /** @generated */
  final Feature casFeat_TimexValueLE;
  /** @generated */
  final int     casFeatCode_TimexValueLE;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getTimexValueLE(int addr) {
        if (featOkTst && casFeat_TimexValueLE == null)
      jcas.throwFeatMissing("TimexValueLE", "de.unihd.dbs.uima.types.heideltime.Timex3Interval");
    return ll_cas.ll_getStringValue(addr, casFeatCode_TimexValueLE);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setTimexValueLE(int addr, String v) {
        if (featOkTst && casFeat_TimexValueLE == null)
      jcas.throwFeatMissing("TimexValueLE", "de.unihd.dbs.uima.types.heideltime.Timex3Interval");
    ll_cas.ll_setStringValue(addr, casFeatCode_TimexValueLE, v);}
    
  
 
  /** @generated */
  final Feature casFeat_TimexValueEE;
  /** @generated */
  final int     casFeatCode_TimexValueEE;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getTimexValueEE(int addr) {
        if (featOkTst && casFeat_TimexValueEE == null)
      jcas.throwFeatMissing("TimexValueEE", "de.unihd.dbs.uima.types.heideltime.Timex3Interval");
    return ll_cas.ll_getStringValue(addr, casFeatCode_TimexValueEE);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setTimexValueEE(int addr, String v) {
        if (featOkTst && casFeat_TimexValueEE == null)
      jcas.throwFeatMissing("TimexValueEE", "de.unihd.dbs.uima.types.heideltime.Timex3Interval");
    ll_cas.ll_setStringValue(addr, casFeatCode_TimexValueEE, v);}
    
  
 
  /** @generated */
  final Feature casFeat_TimexValueLB;
  /** @generated */
  final int     casFeatCode_TimexValueLB;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getTimexValueLB(int addr) {
        if (featOkTst && casFeat_TimexValueLB == null)
      jcas.throwFeatMissing("TimexValueLB", "de.unihd.dbs.uima.types.heideltime.Timex3Interval");
    return ll_cas.ll_getStringValue(addr, casFeatCode_TimexValueLB);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setTimexValueLB(int addr, String v) {
        if (featOkTst && casFeat_TimexValueLB == null)
      jcas.throwFeatMissing("TimexValueLB", "de.unihd.dbs.uima.types.heideltime.Timex3Interval");
    ll_cas.ll_setStringValue(addr, casFeatCode_TimexValueLB, v);}
    
  
 
  /** @generated */
  final Feature casFeat_emptyValue;
  /** @generated */
  final int     casFeatCode_emptyValue;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getEmptyValue(int addr) {
        if (featOkTst && casFeat_emptyValue == null)
      jcas.throwFeatMissing("emptyValue", "de.unihd.dbs.uima.types.heideltime.Timex3Interval");
    return ll_cas.ll_getStringValue(addr, casFeatCode_emptyValue);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setEmptyValue(int addr, String v) {
        if (featOkTst && casFeat_emptyValue == null)
      jcas.throwFeatMissing("emptyValue", "de.unihd.dbs.uima.types.heideltime.Timex3Interval");
    ll_cas.ll_setStringValue(addr, casFeatCode_emptyValue, v);}
    
  
 
  /** @generated */
  final Feature casFeat_beginTimex;
  /** @generated */
  final int     casFeatCode_beginTimex;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getBeginTimex(int addr) {
        if (featOkTst && casFeat_beginTimex == null)
      jcas.throwFeatMissing("beginTimex", "de.unihd.dbs.uima.types.heideltime.Timex3Interval");
    return ll_cas.ll_getStringValue(addr, casFeatCode_beginTimex);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setBeginTimex(int addr, String v) {
        if (featOkTst && casFeat_beginTimex == null)
      jcas.throwFeatMissing("beginTimex", "de.unihd.dbs.uima.types.heideltime.Timex3Interval");
    ll_cas.ll_setStringValue(addr, casFeatCode_beginTimex, v);}
    
  
 
  /** @generated */
  final Feature casFeat_endTimex;
  /** @generated */
  final int     casFeatCode_endTimex;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getEndTimex(int addr) {
        if (featOkTst && casFeat_endTimex == null)
      jcas.throwFeatMissing("endTimex", "de.unihd.dbs.uima.types.heideltime.Timex3Interval");
    return ll_cas.ll_getStringValue(addr, casFeatCode_endTimex);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setEndTimex(int addr, String v) {
        if (featOkTst && casFeat_endTimex == null)
      jcas.throwFeatMissing("endTimex", "de.unihd.dbs.uima.types.heideltime.Timex3Interval");
    ll_cas.ll_setStringValue(addr, casFeatCode_endTimex, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public Timex3Interval_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_TimexValueEB = jcas.getRequiredFeatureDE(casType, "TimexValueEB", "uima.cas.String", featOkTst);
    casFeatCode_TimexValueEB  = (null == casFeat_TimexValueEB) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_TimexValueEB).getCode();

 
    casFeat_TimexValueLE = jcas.getRequiredFeatureDE(casType, "TimexValueLE", "uima.cas.String", featOkTst);
    casFeatCode_TimexValueLE  = (null == casFeat_TimexValueLE) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_TimexValueLE).getCode();

 
    casFeat_TimexValueEE = jcas.getRequiredFeatureDE(casType, "TimexValueEE", "uima.cas.String", featOkTst);
    casFeatCode_TimexValueEE  = (null == casFeat_TimexValueEE) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_TimexValueEE).getCode();

 
    casFeat_TimexValueLB = jcas.getRequiredFeatureDE(casType, "TimexValueLB", "uima.cas.String", featOkTst);
    casFeatCode_TimexValueLB  = (null == casFeat_TimexValueLB) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_TimexValueLB).getCode();

 
    casFeat_emptyValue = jcas.getRequiredFeatureDE(casType, "emptyValue", "uima.cas.String", featOkTst);
    casFeatCode_emptyValue  = (null == casFeat_emptyValue) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_emptyValue).getCode();

 
    casFeat_beginTimex = jcas.getRequiredFeatureDE(casType, "beginTimex", "uima.cas.String", featOkTst);
    casFeatCode_beginTimex  = (null == casFeat_beginTimex) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_beginTimex).getCode();

 
    casFeat_endTimex = jcas.getRequiredFeatureDE(casType, "endTimex", "uima.cas.String", featOkTst);
    casFeatCode_endTimex  = (null == casFeat_endTimex) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_endTimex).getCode();

  }
}



    
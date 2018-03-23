
/* First created by JCasGen Thu Nov 23 14:25:03 CET 2017 */
package opennlp.uima;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.Feature;
import org.apache.uima.jcas.tcas.Annotation_Type;

/** 
 * Updated by JCasGen Thu Mar 22 16:33:35 CET 2018
 * @generated */
public class Parse_Type extends Annotation_Type {
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = Parse.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("opennlp.uima.Parse");
 
  /** @generated */
  final Feature casFeat_parseType;
  /** @generated */
  final int     casFeatCode_parseType;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getParseType(int addr) {
        if (featOkTst && casFeat_parseType == null)
      jcas.throwFeatMissing("parseType", "opennlp.uima.Parse");
    return ll_cas.ll_getStringValue(addr, casFeatCode_parseType);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setParseType(int addr, String v) {
        if (featOkTst && casFeat_parseType == null)
      jcas.throwFeatMissing("parseType", "opennlp.uima.Parse");
    ll_cas.ll_setStringValue(addr, casFeatCode_parseType, v);}
    
  
 
  /** @generated */
  final Feature casFeat_children;
  /** @generated */
  final int     casFeatCode_children;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getChildren(int addr) {
        if (featOkTst && casFeat_children == null)
      jcas.throwFeatMissing("children", "opennlp.uima.Parse");
    return ll_cas.ll_getRefValue(addr, casFeatCode_children);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setChildren(int addr, int v) {
        if (featOkTst && casFeat_children == null)
      jcas.throwFeatMissing("children", "opennlp.uima.Parse");
    ll_cas.ll_setRefValue(addr, casFeatCode_children, v);}
    
   /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @return value at index i in the array 
   */
  public int getChildren(int addr, int i) {
        if (featOkTst && casFeat_children == null)
      jcas.throwFeatMissing("children", "opennlp.uima.Parse");
    if (lowLevelTypeChecks)
      return ll_cas.ll_getRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_children), i, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_children), i);
  return ll_cas.ll_getRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_children), i);
  }
   
  /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @param v value to set
   */ 
  public void setChildren(int addr, int i, int v) {
        if (featOkTst && casFeat_children == null)
      jcas.throwFeatMissing("children", "opennlp.uima.Parse");
    if (lowLevelTypeChecks)
      ll_cas.ll_setRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_children), i, v, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_children), i);
    ll_cas.ll_setRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_children), i, v);
  }
 
 
  /** @generated */
  final Feature casFeat_prob;
  /** @generated */
  final int     casFeatCode_prob;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public double getProb(int addr) {
        if (featOkTst && casFeat_prob == null)
      jcas.throwFeatMissing("prob", "opennlp.uima.Parse");
    return ll_cas.ll_getDoubleValue(addr, casFeatCode_prob);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setProb(int addr, double v) {
        if (featOkTst && casFeat_prob == null)
      jcas.throwFeatMissing("prob", "opennlp.uima.Parse");
    ll_cas.ll_setDoubleValue(addr, casFeatCode_prob, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public Parse_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_parseType = jcas.getRequiredFeatureDE(casType, "parseType", "uima.cas.String", featOkTst);
    casFeatCode_parseType  = (null == casFeat_parseType) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_parseType).getCode();

 
    casFeat_children = jcas.getRequiredFeatureDE(casType, "children", "uima.cas.FSArray", featOkTst);
    casFeatCode_children  = (null == casFeat_children) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_children).getCode();

 
    casFeat_prob = jcas.getRequiredFeatureDE(casType, "prob", "uima.cas.Double", featOkTst);
    casFeatCode_prob  = (null == casFeat_prob) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_prob).getCode();

  }
}



    
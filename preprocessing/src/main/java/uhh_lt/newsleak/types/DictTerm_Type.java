
/* First created by JCasGen Thu Mar 22 16:30:30 CET 2018 */
package uhh_lt.newsleak.types;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.Feature;
import org.apache.uima.jcas.tcas.Annotation_Type;

/** Dictionary terms
 * Updated by JCasGen Thu Mar 22 16:33:35 CET 2018
 * @generated */
public class DictTerm_Type extends Annotation_Type {
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = DictTerm.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("uhh_lt.newsleak.types.DictTerm");



  /** @generated */
  final Feature casFeat_dictType;
  /** @generated */
  final int     casFeatCode_dictType;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getDictType(int addr) {
        if (featOkTst && casFeat_dictType == null)
      jcas.throwFeatMissing("dictType", "uhh_lt.newsleak.types.DictTerm");
    return ll_cas.ll_getRefValue(addr, casFeatCode_dictType);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setDictType(int addr, int v) {
        if (featOkTst && casFeat_dictType == null)
      jcas.throwFeatMissing("dictType", "uhh_lt.newsleak.types.DictTerm");
    ll_cas.ll_setRefValue(addr, casFeatCode_dictType, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public DictTerm_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_dictType = jcas.getRequiredFeatureDE(casType, "dictType", "uima.cas.StringList", featOkTst);
    casFeatCode_dictType  = (null == casFeat_dictType) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_dictType).getCode();

  }
}



    
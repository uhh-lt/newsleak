
/* First created by JCasGen Thu Jan 04 16:52:18 CET 2018 */

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;
import org.apache.uima.jcas.cas.StringList_Type;

/** 
 * Updated by JCasGen Thu Jan 04 16:52:18 CET 2018
 * @generated */
public class keytermList_Type extends StringList_Type {
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = keytermList.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("keytermList");



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public keytermList_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

  }
}



    
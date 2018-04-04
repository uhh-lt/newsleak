

/* First created by JCasGen Thu Mar 22 16:30:30 CET 2018 */
package uhh_lt.newsleak.types;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.cas.StringList;
import org.apache.uima.jcas.tcas.Annotation;


/** Dictionary terms
 * Updated by JCasGen Wed Apr 04 11:24:03 CEST 2018
 * XML source: /Users/gwiedemann/Projects/newsleak-frontend/preprocessing/desc/NewsleakDocument.xml
 * @generated */
public class DictTerm extends Annotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(DictTerm.class);
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int type = typeIndexID;
  /** @generated
   * @return index of the type  
   */
  @Override
  public              int getTypeIndexID() {return typeIndexID;}
 
  /** Never called.  Disable default constructor
   * @generated */
  protected DictTerm() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public DictTerm(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public DictTerm(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public DictTerm(JCas jcas, int begin, int end) {
    super(jcas);
    setBegin(begin);
    setEnd(end);
    readObject();
  }   

  /** 
   * <!-- begin-user-doc -->
   * Write your own initialization here
   * <!-- end-user-doc -->
   *
   * @generated modifiable 
   */
  private void readObject() {/*default - does nothing empty block */}
     
  //*--------------*
  //* Feature: dictType

  /** getter for dictType - gets Type of dictionary added
   * @generated
   * @return value of the feature 
   */
  public StringList getDictType() {
    if (DictTerm_Type.featOkTst && ((DictTerm_Type)jcasType).casFeat_dictType == null)
      jcasType.jcas.throwFeatMissing("dictType", "uhh_lt.newsleak.types.DictTerm");
    return (StringList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((DictTerm_Type)jcasType).casFeatCode_dictType)));}
    
  /** setter for dictType - sets Type of dictionary added 
   * @generated
   * @param v value to set into the feature 
   */
  public void setDictType(StringList v) {
    if (DictTerm_Type.featOkTst && ((DictTerm_Type)jcasType).casFeat_dictType == null)
      jcasType.jcas.throwFeatMissing("dictType", "uhh_lt.newsleak.types.DictTerm");
    jcasType.ll_cas.ll_setRefValue(addr, ((DictTerm_Type)jcasType).casFeatCode_dictType, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: dictTerm

  /** getter for dictTerm - gets Base word types from dictionary list
   * @generated
   * @return value of the feature 
   */
  public StringList getDictTerm() {
    if (DictTerm_Type.featOkTst && ((DictTerm_Type)jcasType).casFeat_dictTerm == null)
      jcasType.jcas.throwFeatMissing("dictTerm", "uhh_lt.newsleak.types.DictTerm");
    return (StringList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((DictTerm_Type)jcasType).casFeatCode_dictTerm)));}
    
  /** setter for dictTerm - sets Base word types from dictionary list 
   * @generated
   * @param v value to set into the feature 
   */
  public void setDictTerm(StringList v) {
    if (DictTerm_Type.featOkTst && ((DictTerm_Type)jcasType).casFeat_dictTerm == null)
      jcasType.jcas.throwFeatMissing("dictTerm", "uhh_lt.newsleak.types.DictTerm");
    jcasType.ll_cas.ll_setRefValue(addr, ((DictTerm_Type)jcasType).casFeatCode_dictTerm, jcasType.ll_cas.ll_getFSRef(v));}    
  }

    
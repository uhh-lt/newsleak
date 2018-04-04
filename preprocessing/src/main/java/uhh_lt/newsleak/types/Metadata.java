package uhh_lt.newsleak.types;

/* First created by JCasGen Wed Nov 22 15:48:08 CET 2017 */

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.tcas.DocumentAnnotation;


/** 
 * Updated by JCasGen Wed Apr 04 11:24:03 CEST 2018
 * XML source: /Users/gwiedemann/Projects/newsleak-frontend/preprocessing/desc/NewsleakDocument.xml
 * @generated */
public class Metadata extends DocumentAnnotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(Metadata.class);
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
  protected Metadata() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public Metadata(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public Metadata(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public Metadata(JCas jcas, int begin, int end) {
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
  //* Feature: metaTripletsNames

  /** getter for metaTripletsNames - gets 
   * @generated
   * @return value of the feature 
   */
  public StringArray getMetaTripletsNames() {
    if (Metadata_Type.featOkTst && ((Metadata_Type)jcasType).casFeat_metaTripletsNames == null)
      jcasType.jcas.throwFeatMissing("metaTripletsNames", "uhh_lt.newsleak.types.Metadata");
    return (StringArray)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((Metadata_Type)jcasType).casFeatCode_metaTripletsNames)));}
    
  /** setter for metaTripletsNames - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setMetaTripletsNames(StringArray v) {
    if (Metadata_Type.featOkTst && ((Metadata_Type)jcasType).casFeat_metaTripletsNames == null)
      jcasType.jcas.throwFeatMissing("metaTripletsNames", "uhh_lt.newsleak.types.Metadata");
    jcasType.ll_cas.ll_setRefValue(addr, ((Metadata_Type)jcasType).casFeatCode_metaTripletsNames, jcasType.ll_cas.ll_getFSRef(v));}    
    
  /** indexed getter for metaTripletsNames - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public String getMetaTripletsNames(int i) {
    if (Metadata_Type.featOkTst && ((Metadata_Type)jcasType).casFeat_metaTripletsNames == null)
      jcasType.jcas.throwFeatMissing("metaTripletsNames", "uhh_lt.newsleak.types.Metadata");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((Metadata_Type)jcasType).casFeatCode_metaTripletsNames), i);
    return jcasType.ll_cas.ll_getStringArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((Metadata_Type)jcasType).casFeatCode_metaTripletsNames), i);}

  /** indexed setter for metaTripletsNames - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setMetaTripletsNames(int i, String v) { 
    if (Metadata_Type.featOkTst && ((Metadata_Type)jcasType).casFeat_metaTripletsNames == null)
      jcasType.jcas.throwFeatMissing("metaTripletsNames", "uhh_lt.newsleak.types.Metadata");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((Metadata_Type)jcasType).casFeatCode_metaTripletsNames), i);
    jcasType.ll_cas.ll_setStringArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((Metadata_Type)jcasType).casFeatCode_metaTripletsNames), i, v);}
   
    
  //*--------------*
  //* Feature: metaTripletsValues

  /** getter for metaTripletsValues - gets 
   * @generated
   * @return value of the feature 
   */
  public StringArray getMetaTripletsValues() {
    if (Metadata_Type.featOkTst && ((Metadata_Type)jcasType).casFeat_metaTripletsValues == null)
      jcasType.jcas.throwFeatMissing("metaTripletsValues", "uhh_lt.newsleak.types.Metadata");
    return (StringArray)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((Metadata_Type)jcasType).casFeatCode_metaTripletsValues)));}
    
  /** setter for metaTripletsValues - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setMetaTripletsValues(StringArray v) {
    if (Metadata_Type.featOkTst && ((Metadata_Type)jcasType).casFeat_metaTripletsValues == null)
      jcasType.jcas.throwFeatMissing("metaTripletsValues", "uhh_lt.newsleak.types.Metadata");
    jcasType.ll_cas.ll_setRefValue(addr, ((Metadata_Type)jcasType).casFeatCode_metaTripletsValues, jcasType.ll_cas.ll_getFSRef(v));}    
    
  /** indexed getter for metaTripletsValues - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public String getMetaTripletsValues(int i) {
    if (Metadata_Type.featOkTst && ((Metadata_Type)jcasType).casFeat_metaTripletsValues == null)
      jcasType.jcas.throwFeatMissing("metaTripletsValues", "uhh_lt.newsleak.types.Metadata");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((Metadata_Type)jcasType).casFeatCode_metaTripletsValues), i);
    return jcasType.ll_cas.ll_getStringArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((Metadata_Type)jcasType).casFeatCode_metaTripletsValues), i);}

  /** indexed setter for metaTripletsValues - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setMetaTripletsValues(int i, String v) { 
    if (Metadata_Type.featOkTst && ((Metadata_Type)jcasType).casFeat_metaTripletsValues == null)
      jcasType.jcas.throwFeatMissing("metaTripletsValues", "uhh_lt.newsleak.types.Metadata");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((Metadata_Type)jcasType).casFeatCode_metaTripletsValues), i);
    jcasType.ll_cas.ll_setStringArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((Metadata_Type)jcasType).casFeatCode_metaTripletsValues), i, v);}
   
    
  //*--------------*
  //* Feature: metaTripletsTypes

  /** getter for metaTripletsTypes - gets 
   * @generated
   * @return value of the feature 
   */
  public StringArray getMetaTripletsTypes() {
    if (Metadata_Type.featOkTst && ((Metadata_Type)jcasType).casFeat_metaTripletsTypes == null)
      jcasType.jcas.throwFeatMissing("metaTripletsTypes", "uhh_lt.newsleak.types.Metadata");
    return (StringArray)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((Metadata_Type)jcasType).casFeatCode_metaTripletsTypes)));}
    
  /** setter for metaTripletsTypes - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setMetaTripletsTypes(StringArray v) {
    if (Metadata_Type.featOkTst && ((Metadata_Type)jcasType).casFeat_metaTripletsTypes == null)
      jcasType.jcas.throwFeatMissing("metaTripletsTypes", "uhh_lt.newsleak.types.Metadata");
    jcasType.ll_cas.ll_setRefValue(addr, ((Metadata_Type)jcasType).casFeatCode_metaTripletsTypes, jcasType.ll_cas.ll_getFSRef(v));}    
    
  /** indexed getter for metaTripletsTypes - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public String getMetaTripletsTypes(int i) {
    if (Metadata_Type.featOkTst && ((Metadata_Type)jcasType).casFeat_metaTripletsTypes == null)
      jcasType.jcas.throwFeatMissing("metaTripletsTypes", "uhh_lt.newsleak.types.Metadata");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((Metadata_Type)jcasType).casFeatCode_metaTripletsTypes), i);
    return jcasType.ll_cas.ll_getStringArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((Metadata_Type)jcasType).casFeatCode_metaTripletsTypes), i);}

  /** indexed setter for metaTripletsTypes - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setMetaTripletsTypes(int i, String v) { 
    if (Metadata_Type.featOkTst && ((Metadata_Type)jcasType).casFeat_metaTripletsTypes == null)
      jcasType.jcas.throwFeatMissing("metaTripletsTypes", "uhh_lt.newsleak.types.Metadata");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((Metadata_Type)jcasType).casFeatCode_metaTripletsTypes), i);
    jcasType.ll_cas.ll_setStringArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((Metadata_Type)jcasType).casFeatCode_metaTripletsTypes), i, v);}
   
    
  //*--------------*
  //* Feature: docId

  /** getter for docId - gets 
   * @generated
   * @return value of the feature 
   */
  public String getDocId() {
    if (Metadata_Type.featOkTst && ((Metadata_Type)jcasType).casFeat_docId == null)
      jcasType.jcas.throwFeatMissing("docId", "uhh_lt.newsleak.types.Metadata");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Metadata_Type)jcasType).casFeatCode_docId);}
    
  /** setter for docId - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setDocId(String v) {
    if (Metadata_Type.featOkTst && ((Metadata_Type)jcasType).casFeat_docId == null)
      jcasType.jcas.throwFeatMissing("docId", "uhh_lt.newsleak.types.Metadata");
    jcasType.ll_cas.ll_setStringValue(addr, ((Metadata_Type)jcasType).casFeatCode_docId, v);}    
   
    
  //*--------------*
  //* Feature: timestamp

  /** getter for timestamp - gets 
   * @generated
   * @return value of the feature 
   */
  public String getTimestamp() {
    if (Metadata_Type.featOkTst && ((Metadata_Type)jcasType).casFeat_timestamp == null)
      jcasType.jcas.throwFeatMissing("timestamp", "uhh_lt.newsleak.types.Metadata");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Metadata_Type)jcasType).casFeatCode_timestamp);}
    
  /** setter for timestamp - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setTimestamp(String v) {
    if (Metadata_Type.featOkTst && ((Metadata_Type)jcasType).casFeat_timestamp == null)
      jcasType.jcas.throwFeatMissing("timestamp", "uhh_lt.newsleak.types.Metadata");
    jcasType.ll_cas.ll_setStringValue(addr, ((Metadata_Type)jcasType).casFeatCode_timestamp, v);}    
   
    
  //*--------------*
  //* Feature: keyterms

  /** getter for keyterms - gets List of key terms
   * @generated
   * @return value of the feature 
   */
  public String getKeyterms() {
    if (Metadata_Type.featOkTst && ((Metadata_Type)jcasType).casFeat_keyterms == null)
      jcasType.jcas.throwFeatMissing("keyterms", "uhh_lt.newsleak.types.Metadata");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Metadata_Type)jcasType).casFeatCode_keyterms);}
    
  /** setter for keyterms - sets List of key terms 
   * @generated
   * @param v value to set into the feature 
   */
  public void setKeyterms(String v) {
    if (Metadata_Type.featOkTst && ((Metadata_Type)jcasType).casFeat_keyterms == null)
      jcasType.jcas.throwFeatMissing("keyterms", "uhh_lt.newsleak.types.Metadata");
    jcasType.ll_cas.ll_setStringValue(addr, ((Metadata_Type)jcasType).casFeatCode_keyterms, v);}    
  }

    
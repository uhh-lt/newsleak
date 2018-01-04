

/* First created by JCasGen Thu Nov 23 16:42:40 CET 2017 */
package de.unihd.dbs.uima.types.heideltime;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.tcas.Annotation;


/** 
 * Updated by JCasGen Thu Jan 04 17:10:59 CET 2018
 * XML source: /Users/gwiedemann/Projects/newsleak-frontend/preprocessing/desc/NewsleakDocument.xml
 * @generated */
public class Dct extends Annotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(Dct.class);
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
  protected Dct() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public Dct(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public Dct(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public Dct(JCas jcas, int begin, int end) {
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
  //* Feature: filename

  /** getter for filename - gets 
   * @generated
   * @return value of the feature 
   */
  public String getFilename() {
    if (Dct_Type.featOkTst && ((Dct_Type)jcasType).casFeat_filename == null)
      jcasType.jcas.throwFeatMissing("filename", "de.unihd.dbs.uima.types.heideltime.Dct");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Dct_Type)jcasType).casFeatCode_filename);}
    
  /** setter for filename - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setFilename(String v) {
    if (Dct_Type.featOkTst && ((Dct_Type)jcasType).casFeat_filename == null)
      jcasType.jcas.throwFeatMissing("filename", "de.unihd.dbs.uima.types.heideltime.Dct");
    jcasType.ll_cas.ll_setStringValue(addr, ((Dct_Type)jcasType).casFeatCode_filename, v);}    
   
    
  //*--------------*
  //* Feature: value

  /** getter for value - gets 
   * @generated
   * @return value of the feature 
   */
  public String getValue() {
    if (Dct_Type.featOkTst && ((Dct_Type)jcasType).casFeat_value == null)
      jcasType.jcas.throwFeatMissing("value", "de.unihd.dbs.uima.types.heideltime.Dct");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Dct_Type)jcasType).casFeatCode_value);}
    
  /** setter for value - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setValue(String v) {
    if (Dct_Type.featOkTst && ((Dct_Type)jcasType).casFeat_value == null)
      jcasType.jcas.throwFeatMissing("value", "de.unihd.dbs.uima.types.heideltime.Dct");
    jcasType.ll_cas.ll_setStringValue(addr, ((Dct_Type)jcasType).casFeatCode_value, v);}    
   
    
  //*--------------*
  //* Feature: timexId

  /** getter for timexId - gets 
   * @generated
   * @return value of the feature 
   */
  public String getTimexId() {
    if (Dct_Type.featOkTst && ((Dct_Type)jcasType).casFeat_timexId == null)
      jcasType.jcas.throwFeatMissing("timexId", "de.unihd.dbs.uima.types.heideltime.Dct");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Dct_Type)jcasType).casFeatCode_timexId);}
    
  /** setter for timexId - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setTimexId(String v) {
    if (Dct_Type.featOkTst && ((Dct_Type)jcasType).casFeat_timexId == null)
      jcasType.jcas.throwFeatMissing("timexId", "de.unihd.dbs.uima.types.heideltime.Dct");
    jcasType.ll_cas.ll_setStringValue(addr, ((Dct_Type)jcasType).casFeatCode_timexId, v);}    
  }

    
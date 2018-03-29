

/* First created by JCasGen Thu Jan 04 14:37:05 CET 2018 */
package de.unihd.dbs.uima.types.heideltime;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;



/** 
 * Updated by JCasGen Thu Mar 29 13:29:03 CEST 2018
 * XML source: /Users/gwiedemann/Projects/newsleak-frontend/preprocessing/desc/NewsleakDocument.xml
 * @generated */
public class Timex3Interval extends Timex3 {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(Timex3Interval.class);
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
  protected Timex3Interval() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public Timex3Interval(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public Timex3Interval(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public Timex3Interval(JCas jcas, int begin, int end) {
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
  //* Feature: TimexValueEB

  /** getter for TimexValueEB - gets 
   * @generated
   * @return value of the feature 
   */
  public String getTimexValueEB() {
    if (Timex3Interval_Type.featOkTst && ((Timex3Interval_Type)jcasType).casFeat_TimexValueEB == null)
      jcasType.jcas.throwFeatMissing("TimexValueEB", "de.unihd.dbs.uima.types.heideltime.Timex3Interval");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Timex3Interval_Type)jcasType).casFeatCode_TimexValueEB);}
    
  /** setter for TimexValueEB - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setTimexValueEB(String v) {
    if (Timex3Interval_Type.featOkTst && ((Timex3Interval_Type)jcasType).casFeat_TimexValueEB == null)
      jcasType.jcas.throwFeatMissing("TimexValueEB", "de.unihd.dbs.uima.types.heideltime.Timex3Interval");
    jcasType.ll_cas.ll_setStringValue(addr, ((Timex3Interval_Type)jcasType).casFeatCode_TimexValueEB, v);}    
   
    
  //*--------------*
  //* Feature: TimexValueLE

  /** getter for TimexValueLE - gets 
   * @generated
   * @return value of the feature 
   */
  public String getTimexValueLE() {
    if (Timex3Interval_Type.featOkTst && ((Timex3Interval_Type)jcasType).casFeat_TimexValueLE == null)
      jcasType.jcas.throwFeatMissing("TimexValueLE", "de.unihd.dbs.uima.types.heideltime.Timex3Interval");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Timex3Interval_Type)jcasType).casFeatCode_TimexValueLE);}
    
  /** setter for TimexValueLE - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setTimexValueLE(String v) {
    if (Timex3Interval_Type.featOkTst && ((Timex3Interval_Type)jcasType).casFeat_TimexValueLE == null)
      jcasType.jcas.throwFeatMissing("TimexValueLE", "de.unihd.dbs.uima.types.heideltime.Timex3Interval");
    jcasType.ll_cas.ll_setStringValue(addr, ((Timex3Interval_Type)jcasType).casFeatCode_TimexValueLE, v);}    
   
    
  //*--------------*
  //* Feature: TimexValueEE

  /** getter for TimexValueEE - gets 
   * @generated
   * @return value of the feature 
   */
  public String getTimexValueEE() {
    if (Timex3Interval_Type.featOkTst && ((Timex3Interval_Type)jcasType).casFeat_TimexValueEE == null)
      jcasType.jcas.throwFeatMissing("TimexValueEE", "de.unihd.dbs.uima.types.heideltime.Timex3Interval");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Timex3Interval_Type)jcasType).casFeatCode_TimexValueEE);}
    
  /** setter for TimexValueEE - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setTimexValueEE(String v) {
    if (Timex3Interval_Type.featOkTst && ((Timex3Interval_Type)jcasType).casFeat_TimexValueEE == null)
      jcasType.jcas.throwFeatMissing("TimexValueEE", "de.unihd.dbs.uima.types.heideltime.Timex3Interval");
    jcasType.ll_cas.ll_setStringValue(addr, ((Timex3Interval_Type)jcasType).casFeatCode_TimexValueEE, v);}    
   
    
  //*--------------*
  //* Feature: TimexValueLB

  /** getter for TimexValueLB - gets 
   * @generated
   * @return value of the feature 
   */
  public String getTimexValueLB() {
    if (Timex3Interval_Type.featOkTst && ((Timex3Interval_Type)jcasType).casFeat_TimexValueLB == null)
      jcasType.jcas.throwFeatMissing("TimexValueLB", "de.unihd.dbs.uima.types.heideltime.Timex3Interval");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Timex3Interval_Type)jcasType).casFeatCode_TimexValueLB);}
    
  /** setter for TimexValueLB - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setTimexValueLB(String v) {
    if (Timex3Interval_Type.featOkTst && ((Timex3Interval_Type)jcasType).casFeat_TimexValueLB == null)
      jcasType.jcas.throwFeatMissing("TimexValueLB", "de.unihd.dbs.uima.types.heideltime.Timex3Interval");
    jcasType.ll_cas.ll_setStringValue(addr, ((Timex3Interval_Type)jcasType).casFeatCode_TimexValueLB, v);}    
   
    
  //*--------------*
  //* Feature: emptyValue

  /** getter for emptyValue - gets 
   * @generated
   * @return value of the feature 
   */
  public String getEmptyValue() {
    if (Timex3Interval_Type.featOkTst && ((Timex3Interval_Type)jcasType).casFeat_emptyValue == null)
      jcasType.jcas.throwFeatMissing("emptyValue", "de.unihd.dbs.uima.types.heideltime.Timex3Interval");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Timex3Interval_Type)jcasType).casFeatCode_emptyValue);}
    
  /** setter for emptyValue - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setEmptyValue(String v) {
    if (Timex3Interval_Type.featOkTst && ((Timex3Interval_Type)jcasType).casFeat_emptyValue == null)
      jcasType.jcas.throwFeatMissing("emptyValue", "de.unihd.dbs.uima.types.heideltime.Timex3Interval");
    jcasType.ll_cas.ll_setStringValue(addr, ((Timex3Interval_Type)jcasType).casFeatCode_emptyValue, v);}    
   
    
  //*--------------*
  //* Feature: beginTimex

  /** getter for beginTimex - gets 
   * @generated
   * @return value of the feature 
   */
  public String getBeginTimex() {
    if (Timex3Interval_Type.featOkTst && ((Timex3Interval_Type)jcasType).casFeat_beginTimex == null)
      jcasType.jcas.throwFeatMissing("beginTimex", "de.unihd.dbs.uima.types.heideltime.Timex3Interval");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Timex3Interval_Type)jcasType).casFeatCode_beginTimex);}
    
  /** setter for beginTimex - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setBeginTimex(String v) {
    if (Timex3Interval_Type.featOkTst && ((Timex3Interval_Type)jcasType).casFeat_beginTimex == null)
      jcasType.jcas.throwFeatMissing("beginTimex", "de.unihd.dbs.uima.types.heideltime.Timex3Interval");
    jcasType.ll_cas.ll_setStringValue(addr, ((Timex3Interval_Type)jcasType).casFeatCode_beginTimex, v);}    
   
    
  //*--------------*
  //* Feature: endTimex

  /** getter for endTimex - gets 
   * @generated
   * @return value of the feature 
   */
  public String getEndTimex() {
    if (Timex3Interval_Type.featOkTst && ((Timex3Interval_Type)jcasType).casFeat_endTimex == null)
      jcasType.jcas.throwFeatMissing("endTimex", "de.unihd.dbs.uima.types.heideltime.Timex3Interval");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Timex3Interval_Type)jcasType).casFeatCode_endTimex);}
    
  /** setter for endTimex - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setEndTimex(String v) {
    if (Timex3Interval_Type.featOkTst && ((Timex3Interval_Type)jcasType).casFeat_endTimex == null)
      jcasType.jcas.throwFeatMissing("endTimex", "de.unihd.dbs.uima.types.heideltime.Timex3Interval");
    jcasType.ll_cas.ll_setStringValue(addr, ((Timex3Interval_Type)jcasType).casFeatCode_endTimex, v);}    
  }

    
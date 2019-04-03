

/* First created by JCasGen Thu Nov 23 16:42:40 CET 2017 */
package de.unihd.dbs.uima.types.heideltime;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.tcas.Annotation;


/** 
 * Updated by JCasGen Wed Apr 03 17:15:09 CEST 2019
 * XML source: /Users/gwiedemann/Projects/newsleak-frontend/preprocessing/desc/NewsleakDocument.xml
 * @generated */
public class Token extends Annotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(Token.class);
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
  protected Token() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public Token(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public Token(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public Token(JCas jcas, int begin, int end) {
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
    if (Token_Type.featOkTst && ((Token_Type)jcasType).casFeat_filename == null)
      jcasType.jcas.throwFeatMissing("filename", "de.unihd.dbs.uima.types.heideltime.Token");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Token_Type)jcasType).casFeatCode_filename);}
    
  /** setter for filename - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setFilename(String v) {
    if (Token_Type.featOkTst && ((Token_Type)jcasType).casFeat_filename == null)
      jcasType.jcas.throwFeatMissing("filename", "de.unihd.dbs.uima.types.heideltime.Token");
    jcasType.ll_cas.ll_setStringValue(addr, ((Token_Type)jcasType).casFeatCode_filename, v);}    
   
    
  //*--------------*
  //* Feature: tokenId

  /** getter for tokenId - gets 
   * @generated
   * @return value of the feature 
   */
  public int getTokenId() {
    if (Token_Type.featOkTst && ((Token_Type)jcasType).casFeat_tokenId == null)
      jcasType.jcas.throwFeatMissing("tokenId", "de.unihd.dbs.uima.types.heideltime.Token");
    return jcasType.ll_cas.ll_getIntValue(addr, ((Token_Type)jcasType).casFeatCode_tokenId);}
    
  /** setter for tokenId - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setTokenId(int v) {
    if (Token_Type.featOkTst && ((Token_Type)jcasType).casFeat_tokenId == null)
      jcasType.jcas.throwFeatMissing("tokenId", "de.unihd.dbs.uima.types.heideltime.Token");
    jcasType.ll_cas.ll_setIntValue(addr, ((Token_Type)jcasType).casFeatCode_tokenId, v);}    
   
    
  //*--------------*
  //* Feature: sentId

  /** getter for sentId - gets 
   * @generated
   * @return value of the feature 
   */
  public int getSentId() {
    if (Token_Type.featOkTst && ((Token_Type)jcasType).casFeat_sentId == null)
      jcasType.jcas.throwFeatMissing("sentId", "de.unihd.dbs.uima.types.heideltime.Token");
    return jcasType.ll_cas.ll_getIntValue(addr, ((Token_Type)jcasType).casFeatCode_sentId);}
    
  /** setter for sentId - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setSentId(int v) {
    if (Token_Type.featOkTst && ((Token_Type)jcasType).casFeat_sentId == null)
      jcasType.jcas.throwFeatMissing("sentId", "de.unihd.dbs.uima.types.heideltime.Token");
    jcasType.ll_cas.ll_setIntValue(addr, ((Token_Type)jcasType).casFeatCode_sentId, v);}    
   
    
  //*--------------*
  //* Feature: pos

  /** getter for pos - gets 
   * @generated
   * @return value of the feature 
   */
  public String getPos() {
    if (Token_Type.featOkTst && ((Token_Type)jcasType).casFeat_pos == null)
      jcasType.jcas.throwFeatMissing("pos", "de.unihd.dbs.uima.types.heideltime.Token");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Token_Type)jcasType).casFeatCode_pos);}
    
  /** setter for pos - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setPos(String v) {
    if (Token_Type.featOkTst && ((Token_Type)jcasType).casFeat_pos == null)
      jcasType.jcas.throwFeatMissing("pos", "de.unihd.dbs.uima.types.heideltime.Token");
    jcasType.ll_cas.ll_setStringValue(addr, ((Token_Type)jcasType).casFeatCode_pos, v);}    
  }

    
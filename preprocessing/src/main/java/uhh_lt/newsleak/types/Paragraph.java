

/* First created by JCasGen Wed Apr 04 11:24:03 CEST 2018 */
package uhh_lt.newsleak.types;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.tcas.Annotation;


/** 
 * Updated by JCasGen Wed Apr 03 17:15:10 CEST 2019
 * XML source: /Users/gwiedemann/Projects/newsleak-frontend/preprocessing/desc/NewsleakDocument.xml
 * @generated */
public class Paragraph extends Annotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(Paragraph.class);
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
  protected Paragraph() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public Paragraph(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public Paragraph(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public Paragraph(JCas jcas, int begin, int end) {
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
  //* Feature: language

  /** getter for language - gets Language of the paragraph
   * @generated
   * @return value of the feature 
   */
  public String getLanguage() {
    if (Paragraph_Type.featOkTst && ((Paragraph_Type)jcasType).casFeat_language == null)
      jcasType.jcas.throwFeatMissing("language", "uhh_lt.newsleak.types.Paragraph");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Paragraph_Type)jcasType).casFeatCode_language);}
    
  /** setter for language - sets Language of the paragraph 
   * @generated
   * @param v value to set into the feature 
   */
  public void setLanguage(String v) {
    if (Paragraph_Type.featOkTst && ((Paragraph_Type)jcasType).casFeat_language == null)
      jcasType.jcas.throwFeatMissing("language", "uhh_lt.newsleak.types.Paragraph");
    jcasType.ll_cas.ll_setStringValue(addr, ((Paragraph_Type)jcasType).casFeatCode_language, v);}    
  }

    
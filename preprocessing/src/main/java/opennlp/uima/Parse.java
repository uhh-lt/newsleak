

/* First created by JCasGen Thu Nov 23 14:25:03 CET 2017 */
package opennlp.uima;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;


/** 
 * Updated by JCasGen Wed Apr 03 17:15:10 CEST 2019
 * XML source: /Users/gwiedemann/Projects/newsleak-frontend/preprocessing/desc/NewsleakDocument.xml
 * @generated */
public class Parse extends Annotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(Parse.class);
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
  protected Parse() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public Parse(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public Parse(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public Parse(JCas jcas, int begin, int end) {
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
  //* Feature: parseType

  /** getter for parseType - gets Type of the parse node
   * @generated
   * @return value of the feature 
   */
  public String getParseType() {
    if (Parse_Type.featOkTst && ((Parse_Type)jcasType).casFeat_parseType == null)
      jcasType.jcas.throwFeatMissing("parseType", "opennlp.uima.Parse");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Parse_Type)jcasType).casFeatCode_parseType);}
    
  /** setter for parseType - sets Type of the parse node 
   * @generated
   * @param v value to set into the feature 
   */
  public void setParseType(String v) {
    if (Parse_Type.featOkTst && ((Parse_Type)jcasType).casFeat_parseType == null)
      jcasType.jcas.throwFeatMissing("parseType", "opennlp.uima.Parse");
    jcasType.ll_cas.ll_setStringValue(addr, ((Parse_Type)jcasType).casFeatCode_parseType, v);}    
   
    
  //*--------------*
  //* Feature: children

  /** getter for children - gets Leaf nodes
   * @generated
   * @return value of the feature 
   */
  public FSArray getChildren() {
    if (Parse_Type.featOkTst && ((Parse_Type)jcasType).casFeat_children == null)
      jcasType.jcas.throwFeatMissing("children", "opennlp.uima.Parse");
    return (FSArray)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((Parse_Type)jcasType).casFeatCode_children)));}
    
  /** setter for children - sets Leaf nodes 
   * @generated
   * @param v value to set into the feature 
   */
  public void setChildren(FSArray v) {
    if (Parse_Type.featOkTst && ((Parse_Type)jcasType).casFeat_children == null)
      jcasType.jcas.throwFeatMissing("children", "opennlp.uima.Parse");
    jcasType.ll_cas.ll_setRefValue(addr, ((Parse_Type)jcasType).casFeatCode_children, jcasType.ll_cas.ll_getFSRef(v));}    
    
  /** indexed getter for children - gets an indexed value - Leaf nodes
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public TOP getChildren(int i) {
    if (Parse_Type.featOkTst && ((Parse_Type)jcasType).casFeat_children == null)
      jcasType.jcas.throwFeatMissing("children", "opennlp.uima.Parse");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((Parse_Type)jcasType).casFeatCode_children), i);
    return (TOP)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((Parse_Type)jcasType).casFeatCode_children), i)));}

  /** indexed setter for children - sets an indexed value - Leaf nodes
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setChildren(int i, TOP v) { 
    if (Parse_Type.featOkTst && ((Parse_Type)jcasType).casFeat_children == null)
      jcasType.jcas.throwFeatMissing("children", "opennlp.uima.Parse");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((Parse_Type)jcasType).casFeatCode_children), i);
    jcasType.ll_cas.ll_setRefArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((Parse_Type)jcasType).casFeatCode_children), i, jcasType.ll_cas.ll_getFSRef(v));}
   
    
  //*--------------*
  //* Feature: prob

  /** getter for prob - gets Leaf nodes
   * @generated
   * @return value of the feature 
   */
  public double getProb() {
    if (Parse_Type.featOkTst && ((Parse_Type)jcasType).casFeat_prob == null)
      jcasType.jcas.throwFeatMissing("prob", "opennlp.uima.Parse");
    return jcasType.ll_cas.ll_getDoubleValue(addr, ((Parse_Type)jcasType).casFeatCode_prob);}
    
  /** setter for prob - sets Leaf nodes 
   * @generated
   * @param v value to set into the feature 
   */
  public void setProb(double v) {
    if (Parse_Type.featOkTst && ((Parse_Type)jcasType).casFeat_prob == null)
      jcasType.jcas.throwFeatMissing("prob", "opennlp.uima.Parse");
    jcasType.ll_cas.ll_setDoubleValue(addr, ((Parse_Type)jcasType).casFeatCode_prob, v);}    
  }

    
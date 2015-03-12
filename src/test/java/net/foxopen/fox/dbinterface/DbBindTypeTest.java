package net.foxopen.fox.dbinterface;

//import java.sql.Timestamp;
//
//import java.util.Calendar;
//
//import net.foxopen.fox.ContextUElem;
//import net.foxopen.fox.dom.DOM;
//import net.foxopen.fox.ex.ExActionFailed;
//import net.foxopen.fox.ex.ExInternal;
//
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertTrue;
//import org.junit.BeforeClass;
//import org.junit.Test;
//
//import static org.mockito.Mockito.any;
//import static org.mockito.Mockito.anyString;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.when;


import org.junit.Ignore;

/**
 * Test the behaviour of the DbBindType class (parsed binds for
 * a db-interface2 api or query).
 */
@Ignore
public class DbBindTypeTest {
//
//  private static InterfaceStatement mDbStatement;
//  private static String mBindName = "test";
//  private static DOM mDummyDOM;
//
//  /**
//   * Set up a quick mock DbStatement so that we don't get a null pointer when
//   * DbBindType throws an exception using the description of the parent
//   * object. Mock DOM is for bind evaluation tests.
//   */
//  @BeforeClass
//  public static void setUpMocks() {
//    mDbStatement = mock(InterfaceStatement.class);
//    when(mDbStatement.getDBInterfaceName()).thenReturn("dbint-test");
//    when(mDbStatement.getStatementName()).thenReturn("api-test");
//    when(mDbStatement.getStatementType()).thenReturn(InterfaceStatement.API);
//    mDummyDOM = mock(DOM.class);
//  }
//
//  /**
//   * Validate that the initial argument isn't null.
//   */
//  @Test(expected=ExInternal.class)
//  public void testConstructorNullNameCheck() {
//    new InterfaceParameter(null, null, null, null, 1, mDbStatement);
//  }
//
//  /**
//   * Validate that the initial argument isn't an empty string.
//   */
//  @Test(expected=ExInternal.class)
//  public void testConstructorEmptyStringNameCheck() {
//    new InterfaceParameter("", null, null, null, 1, mDbStatement);
//  }
//
//  /**
//   * Validate the final empty argument isn't empty.
//   */
//  @Test(expected=ExInternal.class)
//  public void testConstructorNullDbStatement() {
//    new InterfaceParameter(mBindName, null, null, null, 1, null);
//  }
//
//  /**
//   * Test an exception is thrown when passing in a datadom-type value in the sql-type parameter.
//   */
//  @Test(expected=ExInternal.class)
//  public void testConstructorValidatesSqlTypes1() {
//    new InterfaceParameter(mBindName, InterfaceParameter.FOXBIND_STRING, null, null, 1, mDbStatement);
//  }
//
//  /**
//   * Test an exception is thrown when passing in a datadom-type value in the sql-type parameter.
//   */
//  @Test(expected=ExInternal.class)
//  public void testConstructorValidatesSqlTypes2() {
//    new InterfaceParameter(mBindName, InterfaceParameter.FOXBIND_DOM, null, null, 1, mDbStatement);
//  }
//
//  /**
//   * Test an exception is thrown when passing in a datadom-type value in the sql-type parameter.
//   */
//  @Test(expected=ExInternal.class)
//  public void testConstructorValidatesSqlTypes3() {
//    new InterfaceParameter(mBindName, InterfaceParameter.FOXBIND_DATE, null, null, 1, mDbStatement);
//  }
//
//  /**
//   * Test an exception is thrown when passing in a datadom-type value in the sql-type parameter.
//   */
//  @Test(expected=ExInternal.class)
//  public void testConstructorValidatesSqlTypes4() {
//    new InterfaceParameter(mBindName, InterfaceParameter.FOXBIND_DATETIME, null, null, 1, mDbStatement);
//  }
//
//  /**
//   * Test an exception is thrown when passing in a sql-type value in the datadom-type parameter.
//   */
//  @Test(expected=ExInternal.class)
//  public void testConstructorValidatesFoxTypes1() {
//    new InterfaceParameter(mBindName, null, InterfaceParameter.SQLBIND_STRING, null, 1, mDbStatement);
//  }
//
//  /**
//   * Test an exception is thrown when passing in a sql-type value in the datadom-type parameter.
//   */
//  @Test(expected=ExInternal.class)
//  public void testConstructorValidatesFoxTypes2() {
//    new InterfaceParameter(mBindName, null, InterfaceParameter.SQLBIND_CLOB, null, 1, mDbStatement);
//  }
//
//  /**
//   * Test an exception is thrown when passing in a sql-type value in the datadom-type parameter.
//   */
//  @Test(expected=ExInternal.class)
//  public void testConstructorValidatesFoxTypes3() {
//    new InterfaceParameter(mBindName, null, InterfaceParameter.SQLBIND_DATE, null, 1, mDbStatement);
//  }
//
//  /**
//   * Test an exception is thrown when passing in a sql-type value in the datadom-type parameter.
//   */
//  @Test(expected=ExInternal.class)
//  public void testConstructorValidatesFoxTypes4() {
//    new InterfaceParameter(mBindName, null, InterfaceParameter.SQLBIND_XML, null, 1, mDbStatement);
//  }
//
//  /**
//   * If nullable parameters are not provided, check defaults.
//   */
//  @Test
//  public void testConstructorDefaultsAllNulls() {
//    InterfaceParameter lDbBind = new InterfaceParameter(mBindName, null, null, null, 1, mDbStatement);
//
//    // Check mandatory items
//    assertEquals("Bind name was not as expected", mBindName, lDbBind.getBindName());
//    assertEquals("Index was not as expected", 1, lDbBind.getIndex());
//
//    // Check nullable items that should be defaulted
//    assertEquals("Default sql-type was not as expected", InterfaceParameter.SQLBIND_STRING, lDbBind.getBindSQLType());
//    assertEquals("Default datadom-type was not as expected", InterfaceParameter.FOXBIND_STRING, lDbBind.getDOMDataType());
//    assertEquals("Default datadom-location was not as expected", ".", lDbBind.getRelativeXPath());
//  }
//
//  /**
//   * If SQL types are provided, correct FOX types should be derived.
//   */
//  @Test
//  public void testConstructorDefaultsSqlTypesProvided() {
//    InterfaceParameter lDbBind1 = new InterfaceParameter(mBindName, InterfaceParameter.SQLBIND_STRING, null, null, 1, mDbStatement);
//    assertEquals("Defaulted datadom-type for sql-type " + InterfaceParameter.SQLBIND_STRING + " was not correct", InterfaceParameter.FOXBIND_STRING, lDbBind1.getDOMDataType());
//
//    InterfaceParameter lDbBind2 = new InterfaceParameter(mBindName, InterfaceParameter.SQLBIND_CLOB, null, null, 1, mDbStatement);
//    assertEquals("Defaulted datadom-type for sql-type " + InterfaceParameter.SQLBIND_CLOB + " was not correct", InterfaceParameter.FOXBIND_STRING, lDbBind2.getDOMDataType());
//
//    InterfaceParameter lDbBind3 = new InterfaceParameter(mBindName, InterfaceParameter.SQLBIND_DATE, null, null, 1, mDbStatement);
//    assertEquals("Defaulted datadom-type for sql-type " + InterfaceParameter.SQLBIND_DATE + " was not correct", InterfaceParameter.FOXBIND_DATE, lDbBind3.getDOMDataType());
//
//    InterfaceParameter lDbBind4 = new InterfaceParameter(mBindName, InterfaceParameter.SQLBIND_XML, null, null, 1, mDbStatement);
//    assertEquals("Defaulted datadom-type for sql-type " + InterfaceParameter.SQLBIND_XML + " was not correct", InterfaceParameter.FOXBIND_DOM, lDbBind4.getDOMDataType());
//  }
//
//  /**
//   * If FOX types are provided, correct SQL types should be derived.
//   */
//  @Test
//  public void testConstructorDefaultsFoxTypesProvided() {
//    InterfaceParameter lDbBind1 = new InterfaceParameter(mBindName, null, InterfaceParameter.FOXBIND_STRING, null, 1, mDbStatement);
//    assertEquals("Defaulted sql-type for datadom-type " + InterfaceParameter.FOXBIND_STRING + " was not correct", InterfaceParameter.SQLBIND_STRING, lDbBind1.getBindSQLType());
//
//    InterfaceParameter lDbBind2 = new InterfaceParameter(mBindName, null, InterfaceParameter.FOXBIND_DOM, null, 1, mDbStatement);
//    assertEquals("Defaulted sql-type for datadom-type " + InterfaceParameter.FOXBIND_DOM + " was not correct", InterfaceParameter.SQLBIND_XML, lDbBind2.getBindSQLType());
//
//    InterfaceParameter lDbBind3 = new InterfaceParameter(mBindName, null, InterfaceParameter.FOXBIND_DATE, null, 1, mDbStatement);
//    assertEquals("Defaulted sql-type for datadom-type " + InterfaceParameter.FOXBIND_DATE + " was not correct", InterfaceParameter.SQLBIND_DATE, lDbBind3.getBindSQLType());
//
//    InterfaceParameter lDbBind4 = new InterfaceParameter(mBindName, null, InterfaceParameter.FOXBIND_DATETIME, null, 1, mDbStatement);
//    assertEquals("Defaulted sql-type for datadom-type " + InterfaceParameter.FOXBIND_DATETIME + " was not correct", InterfaceParameter.SQLBIND_DATE, lDbBind4.getBindSQLType());
//  }
//
//  /**
//   * If FOX types are provided, and SQL types are provided, they should both be respected.
//   */
//  @Test
//  public void testConstructorDoesNotChangeValuesDate1() {
//    InterfaceParameter lDbBind = new InterfaceParameter(mBindName, InterfaceParameter.SQLBIND_DATE, InterfaceParameter.FOXBIND_STRING, null, 1, mDbStatement);
//    assertEquals("DbBindType overwrote provided sql-type", InterfaceParameter.SQLBIND_DATE, lDbBind.getBindSQLType());
//    assertEquals("DbBindType overwrote provided datadom-type", InterfaceParameter.FOXBIND_STRING, lDbBind.getDOMDataType());
//  }
//
//  /**
//   * If FOX types are provided, and SQL types are provided, they should both be respected.
//   */
//  @Test
//  public void testConstructorDoesNotChangeValuesDate2() {
//    InterfaceParameter lDbBind = new InterfaceParameter(mBindName, InterfaceParameter.SQLBIND_DATE, InterfaceParameter.FOXBIND_DOM, null, 1, mDbStatement);
//    assertEquals("DbBindType overwrote provided sql-type", InterfaceParameter.SQLBIND_DATE, lDbBind.getBindSQLType());
//    assertEquals("DbBindType overwrote provided datadom-type", InterfaceParameter.FOXBIND_DOM, lDbBind.getDOMDataType());
//  }
//
//  /**
//   * If FOX types are provided, and SQL types are provided, they should both be respected.
//   */
//  @Test
//  public void testConstructorDoesNotChangeValuesDate3() {
//    InterfaceParameter lDbBind = new InterfaceParameter(mBindName, InterfaceParameter.SQLBIND_DATE, InterfaceParameter.FOXBIND_DATE, null, 1, mDbStatement);
//    assertEquals("DbBindType overwrote provided sql-type", InterfaceParameter.SQLBIND_DATE, lDbBind.getBindSQLType());
//    assertEquals("DbBindType overwrote provided datadom-type", InterfaceParameter.FOXBIND_DATE, lDbBind.getDOMDataType());
//  }
//
//  /**
//   * If FOX types are provided, and SQL types are provided, they should both be respected.
//   */
//  @Test
//  public void testConstructorDoesNotChangeValuesDate4() {
//    InterfaceParameter lDbBind = new InterfaceParameter(mBindName, InterfaceParameter.SQLBIND_DATE, InterfaceParameter.FOXBIND_DATETIME, null, 1, mDbStatement);
//    assertEquals("DbBindType overwrote provided sql-type", InterfaceParameter.SQLBIND_DATE, lDbBind.getBindSQLType());
//    assertEquals("DbBindType overwrote provided datadom-type", InterfaceParameter.FOXBIND_DATETIME, lDbBind.getDOMDataType());
//  }
//
//  /**
//   * If FOX types are provided, and SQL types are provided, they should both be respected.
//   */
//  @Test
//  public void testConstructorDoesNotChangeValuesString1() {
//    InterfaceParameter lDbBind = new InterfaceParameter(mBindName, InterfaceParameter.SQLBIND_STRING, InterfaceParameter.FOXBIND_STRING, null, 1, mDbStatement);
//    assertEquals("DbBindType overwrote provided sql-type", InterfaceParameter.SQLBIND_STRING, lDbBind.getBindSQLType());
//    assertEquals("DbBindType overwrote provided datadom-type", InterfaceParameter.FOXBIND_STRING, lDbBind.getDOMDataType());
//  }
//
//  /**
//   * If FOX types are provided, and SQL types are provided, they should both be respected.
//   */
//  @Test
//  public void testConstructorDoesNotChangeValuesString2() {
//    InterfaceParameter lDbBind = new InterfaceParameter(mBindName, InterfaceParameter.SQLBIND_STRING, InterfaceParameter.FOXBIND_DOM, null, 1, mDbStatement);
//    assertEquals("DbBindType overwrote provided sql-type", InterfaceParameter.SQLBIND_STRING, lDbBind.getBindSQLType());
//    assertEquals("DbBindType overwrote provided datadom-type", InterfaceParameter.FOXBIND_DOM, lDbBind.getDOMDataType());
//  }
//
//  /**
//   * If FOX types are provided, and SQL types are provided, they should both be respected.
//   */
//  @Test
//  public void testConstructorDoesNotChangeValuesString3() {
//    InterfaceParameter lDbBind = new InterfaceParameter(mBindName, InterfaceParameter.SQLBIND_STRING, InterfaceParameter.FOXBIND_DATE, null, 1, mDbStatement);
//    assertEquals("DbBindType overwrote provided sql-type", InterfaceParameter.SQLBIND_STRING, lDbBind.getBindSQLType());
//    assertEquals("DbBindType overwrote provided datadom-type", InterfaceParameter.FOXBIND_DATE, lDbBind.getDOMDataType());
//  }
//
//  /**
//   * If FOX types are provided, and SQL types are provided, they should both be respected.
//   */
//  @Test
//  public void testConstructorDoesNotChangeValuesString4() {
//    InterfaceParameter lDbBind = new InterfaceParameter(mBindName, InterfaceParameter.SQLBIND_STRING, InterfaceParameter.FOXBIND_DATETIME, null, 1, mDbStatement);
//    assertEquals("DbBindType overwrote provided sql-type", InterfaceParameter.SQLBIND_STRING, lDbBind.getBindSQLType());
//    assertEquals("DbBindType overwrote provided datadom-type", InterfaceParameter.FOXBIND_DATETIME, lDbBind.getDOMDataType());
//  }
//
//  /**
//   * If FOX types are provided, and SQL types are provided, they should both be respected.
//   */
//  @Test
//  public void testConstructorDoesNotChangeValuesClob1() {
//    InterfaceParameter lDbBind = new InterfaceParameter(mBindName, InterfaceParameter.SQLBIND_CLOB, InterfaceParameter.FOXBIND_STRING, null, 1, mDbStatement);
//    assertEquals("DbBindType overwrote provided sql-type", InterfaceParameter.SQLBIND_CLOB, lDbBind.getBindSQLType());
//    assertEquals("DbBindType overwrote provided datadom-type", InterfaceParameter.FOXBIND_STRING, lDbBind.getDOMDataType());
//  }
//
//  /**
//   * If FOX types are provided, and SQL types are provided, they should both be respected.
//   */
//  @Test
//  public void testConstructorDoesNotChangeValuesClob2() {
//    InterfaceParameter lDbBind = new InterfaceParameter(mBindName, InterfaceParameter.SQLBIND_CLOB, InterfaceParameter.FOXBIND_DOM, null, 1, mDbStatement);
//    assertEquals("DbBindType overwrote provided sql-type", InterfaceParameter.SQLBIND_CLOB, lDbBind.getBindSQLType());
//    assertEquals("DbBindType overwrote provided datadom-type", InterfaceParameter.FOXBIND_DOM, lDbBind.getDOMDataType());
//  }
//
//  /**
//   * If FOX types are provided, and SQL types are provided, they should both be respected.
//   */
//  @Test
//  public void testConstructorDoesNotChangeValuesClob3() {
//    InterfaceParameter lDbBind = new InterfaceParameter(mBindName, InterfaceParameter.SQLBIND_CLOB, InterfaceParameter.FOXBIND_DATETIME, null, 1, mDbStatement);
//    assertEquals("DbBindType overwrote provided sql-type", InterfaceParameter.SQLBIND_CLOB, lDbBind.getBindSQLType());
//    assertEquals("DbBindType overwrote provided datadom-type", InterfaceParameter.FOXBIND_DATETIME, lDbBind.getDOMDataType());
//  }
//
//  /**
//   * If FOX types are provided, and SQL types are provided, they should both be respected.
//   */
//  @Test
//  public void testConstructorDoesNotChangeValuesClob4() {
//    InterfaceParameter lDbBind = new InterfaceParameter(mBindName, InterfaceParameter.SQLBIND_CLOB, InterfaceParameter.FOXBIND_DATE, null, 1, mDbStatement);
//    assertEquals("DbBindType overwrote provided sql-type", InterfaceParameter.SQLBIND_CLOB, lDbBind.getBindSQLType());
//    assertEquals("DbBindType overwrote provided datadom-type", InterfaceParameter.FOXBIND_DATE, lDbBind.getDOMDataType());
//  }
//
//  /**
//   * If FOX types are provided, and SQL types are provided, they should both be respected.
//   */
//  @Test
//  public void testConstructorDoesNotChangeValuesXml1() {
//    InterfaceParameter lDbBind = new InterfaceParameter(mBindName, InterfaceParameter.SQLBIND_XML, InterfaceParameter.FOXBIND_STRING, null, 1, mDbStatement);
//    assertEquals("DbBindType overwrote provided sql-type", InterfaceParameter.SQLBIND_XML, lDbBind.getBindSQLType());
//    assertEquals("DbBindType overwrote provided datadom-type", InterfaceParameter.FOXBIND_STRING, lDbBind.getDOMDataType());
//  }
//
//  /**
//   * If FOX types are provided, and SQL types are provided, they should both be respected.
//   */
//  @Test
//  public void testConstructorDoesNotChangeValuesXml2() {
//    InterfaceParameter lDbBind = new InterfaceParameter(mBindName, InterfaceParameter.SQLBIND_XML, InterfaceParameter.FOXBIND_DOM, null, 1, mDbStatement);
//    assertEquals("DbBindType overwrote provided sql-type", InterfaceParameter.SQLBIND_XML, lDbBind.getBindSQLType());
//    assertEquals("DbBindType overwrote provided datadom-type", InterfaceParameter.FOXBIND_DOM, lDbBind.getDOMDataType());
//  }
//
//  /**
//   * If FOX types are provided, and SQL types are provided, they should both be respected.
//   */
//  @Test
//  public void testConstructorDoesNotChangeValuesXml3() {
//    InterfaceParameter lDbBind = new InterfaceParameter(mBindName, InterfaceParameter.SQLBIND_XML, InterfaceParameter.FOXBIND_DATETIME, null, 1, mDbStatement);
//    assertEquals("DbBindType overwrote provided sql-type", InterfaceParameter.SQLBIND_XML, lDbBind.getBindSQLType());
//    assertEquals("DbBindType overwrote provided datadom-type", InterfaceParameter.FOXBIND_DATETIME, lDbBind.getDOMDataType());
//  }
//
//  /**
//   * If FOX types are provided, and SQL types are provided, they should both be respected.
//   */
//  @Test
//  public void testConstructorDoesNotChangeValuesXml4() {
//    InterfaceParameter lDbBind = new InterfaceParameter(mBindName, InterfaceParameter.SQLBIND_XML, InterfaceParameter.FOXBIND_DATE, null, 1, mDbStatement);
//    assertEquals("DbBindType overwrote provided sql-type", InterfaceParameter.SQLBIND_XML, lDbBind.getBindSQLType());
//    assertEquals("DbBindType overwrote provided datadom-type", InterfaceParameter.FOXBIND_DATE, lDbBind.getDOMDataType());
//  }
//
//  /**
//   * Test date conversion occurs as expected.
//   * @throws ExActionFailed bind failed to process
//   */
//  @Test
//  public void testProcessBindStringToDateConversion1()
//  throws ExActionFailed {
//    InterfaceParameter lDbBind = new InterfaceParameter(mBindName, InterfaceParameter.SQLBIND_DATE, InterfaceParameter.FOXBIND_STRING, ".", 1, mDbStatement);
//    ContextUElem lContextUElem = mock(ContextUElem.class);
//    when(lContextUElem.extendedXPathString(any(DOM.class), anyString())).thenReturn("2000-01-02");
//
//    Object lObj = lDbBind.processBind(mDummyDOM, lContextUElem);
//    assertTrue("Wrong type returned", lObj instanceof Timestamp);
//
//    Calendar lCal = Calendar.getInstance();
//    lCal.setTime((Timestamp) lObj);
//
//    assertEquals("Wrong year returned", 2000, lCal.get(Calendar.YEAR));
//    assertEquals("Wrong month returned", 0, lCal.get(Calendar.MONTH));
//    assertEquals("Wrong day returned", 2, lCal.get(Calendar.DAY_OF_MONTH));
//  }
//
//  /**
//   * Test date conversion occurs as expected.
//   * @throws ExActionFailed bind failed to process
//   */
//  @Test
//  public void testProcessBindStringToDateConversion2()
//  throws ExActionFailed {
//    InterfaceParameter lDbBind = new InterfaceParameter(mBindName, InterfaceParameter.SQLBIND_DATE, InterfaceParameter.FOXBIND_STRING, ".", 1, mDbStatement);
//    ContextUElem lContextUElem = mock(ContextUElem.class);
//    when(lContextUElem.extendedXPathString(any(DOM.class), anyString())).thenReturn("2000-01-02T01:02:03");
//
//    Object lObj = lDbBind.processBind(mDummyDOM, lContextUElem);
//    assertTrue("Wrong type returned", lObj instanceof Timestamp);
//
//    Calendar lCal = Calendar.getInstance();
//    lCal.setTime((Timestamp) lObj);
//
//    assertEquals("Wrong year returned", 2000, lCal.get(Calendar.YEAR));
//    assertEquals("Wrong month returned", 0, lCal.get(Calendar.MONTH));
//    assertEquals("Wrong day returned", 2, lCal.get(Calendar.DAY_OF_MONTH));
//    assertEquals("Wrong hour returned", 1, lCal.get(Calendar.HOUR_OF_DAY));
//    assertEquals("Wrong minute returned", 2, lCal.get(Calendar.MINUTE));
//    assertEquals("Wrong second returned", 3, lCal.get(Calendar.SECOND));
//  }
//
//  /**
//   * Test date conversion occurs as expected.
//   * @throws ExActionFailed bind failed to process
//   */
//  @Test
//  public void testProcessBindDateToDateConversion1()
//  throws ExActionFailed {
//    InterfaceParameter lDbBind = new InterfaceParameter(mBindName, InterfaceParameter.SQLBIND_DATE, InterfaceParameter.FOXBIND_DATE, ".", 1, mDbStatement);
//    ContextUElem lContextUElem = mock(ContextUElem.class);
//    when(lContextUElem.extendedXPathString(any(DOM.class), anyString())).thenReturn("2000-01-02");
//
//    Object lObj = lDbBind.processBind(mDummyDOM, lContextUElem);
//    assertTrue("Wrong type returned", lObj instanceof Timestamp);
//
//    Calendar lCal = Calendar.getInstance();
//    lCal.setTime((Timestamp) lObj);
//
//    assertEquals("Wrong year returned", 2000, lCal.get(Calendar.YEAR));
//    assertEquals("Wrong month returned", 0, lCal.get(Calendar.MONTH));
//    assertEquals("Wrong day returned", 2, lCal.get(Calendar.DAY_OF_MONTH));
//  }
//
//  /**
//   * Test date conversion occurs as expected.
//   * @throws ExActionFailed bind failed to process
//   */
//  @Test
//  public void testProcessBindDateToDateConversion2()
//  throws ExActionFailed {
//    InterfaceParameter lDbBind = new InterfaceParameter(mBindName, InterfaceParameter.SQLBIND_DATE, InterfaceParameter.FOXBIND_DATE, ".", 1, mDbStatement);
//    ContextUElem lContextUElem = mock(ContextUElem.class);
//    when(lContextUElem.extendedXPathString(any(DOM.class), anyString())).thenReturn("2000-01-02T01:02:03");
//
//    Object lObj = lDbBind.processBind(mDummyDOM, lContextUElem);
//    assertTrue("Wrong type returned", lObj instanceof Timestamp);
//
//    Calendar lCal = Calendar.getInstance();
//    lCal.setTime((Timestamp) lObj);
//
//    assertEquals("Wrong year returned", 2000, lCal.get(Calendar.YEAR));
//    assertEquals("Wrong month returned", 0, lCal.get(Calendar.MONTH));
//    assertEquals("Wrong day returned", 2, lCal.get(Calendar.DAY_OF_MONTH));
//    assertEquals("Wrong hour returned", 1, lCal.get(Calendar.HOUR_OF_DAY));
//    assertEquals("Wrong minute returned", 2, lCal.get(Calendar.MINUTE));
//    assertEquals("Wrong second returned", 3, lCal.get(Calendar.SECOND));
//  }
//
//
//  /**
//   * Test date conversion occurs as expected.
//   * @throws ExActionFailed bind failed to process
//   */
//  @Test
//  public void testProcessBindDatetimeToDateConversion1()
//  throws ExActionFailed {
//    InterfaceParameter lDbBind = new InterfaceParameter(mBindName, InterfaceParameter.SQLBIND_DATE, InterfaceParameter.FOXBIND_DATETIME, ".", 1, mDbStatement);
//    ContextUElem lContextUElem = mock(ContextUElem.class);
//    when(lContextUElem.extendedXPathString(any(DOM.class), anyString())).thenReturn("2000-01-02");
//
//    Object lObj = lDbBind.processBind(mDummyDOM, lContextUElem);
//    assertTrue("Wrong type returned", lObj instanceof Timestamp);
//
//    Calendar lCal = Calendar.getInstance();
//    lCal.setTime((Timestamp) lObj);
//
//    assertEquals("Wrong year returned", 2000, lCal.get(Calendar.YEAR));
//    assertEquals("Wrong month returned", 0, lCal.get(Calendar.MONTH));
//    assertEquals("Wrong day returned", 2, lCal.get(Calendar.DAY_OF_MONTH));
//  }
//
//  /**
//   * Test date conversion occurs as expected.
//   * @throws ExActionFailed bind failed to process
//   */
//  @Test
//  public void testProcessBindDatetimeToDateConversion2()
//  throws ExActionFailed {
//    InterfaceParameter lDbBind = new InterfaceParameter(mBindName, InterfaceParameter.SQLBIND_DATE, InterfaceParameter.FOXBIND_DATETIME, ".", 1, mDbStatement);
//    ContextUElem lContextUElem = mock(ContextUElem.class);
//    when(lContextUElem.extendedXPathString(any(DOM.class), anyString())).thenReturn("2000-01-02T01:02:03");
//
//    Object lObj = lDbBind.processBind(mDummyDOM, lContextUElem);
//    assertTrue("Wrong type returned", lObj instanceof Timestamp);
//
//    Calendar lCal = Calendar.getInstance();
//    lCal.setTime((Timestamp) lObj);
//
//    assertEquals("Wrong year returned", 2000, lCal.get(Calendar.YEAR));
//    assertEquals("Wrong month returned", 0, lCal.get(Calendar.MONTH));
//    assertEquals("Wrong day returned", 2, lCal.get(Calendar.DAY_OF_MONTH));
//    assertEquals("Wrong hour returned", 1, lCal.get(Calendar.HOUR_OF_DAY));
//    assertEquals("Wrong minute returned", 2, lCal.get(Calendar.MINUTE));
//    assertEquals("Wrong second returned", 3, lCal.get(Calendar.SECOND));
//  }
//
//  /**
//   * Test date conversion fails as expected.
//   * @throws ExActionFailed bind failed to process
//   */
//  @Test(expected=ExInternal.class)
//  public void testProcessBindDateConversionFailure1()
//  throws ExActionFailed {
//    InterfaceParameter lDbBind = new InterfaceParameter(mBindName, InterfaceParameter.SQLBIND_DATE, InterfaceParameter.FOXBIND_STRING, ".", 1, mDbStatement);
//    ContextUElem lContextUElem = mock(ContextUElem.class);
//    when(lContextUElem.extendedXPathString(any(DOM.class), anyString())).thenReturn("not-a-date");
//    Object lObj = lDbBind.processBind(mDummyDOM, lContextUElem);
//  }
//
//  /**
//   * Test date conversion fails as expected.
//   * @throws ExActionFailed bind failed to process
//   */
//  @Test(expected=ExInternal.class)
//  public void testProcessBindDateConversionFailure2()
//  throws ExActionFailed {
//    InterfaceParameter lDbBind = new InterfaceParameter(mBindName, InterfaceParameter.SQLBIND_DATE, InterfaceParameter.FOXBIND_DATE, ".", 1, mDbStatement);
//    ContextUElem lContextUElem = mock(ContextUElem.class);
//    when(lContextUElem.extendedXPathString(any(DOM.class), anyString())).thenReturn("not-a-date");
//    Object lObj = lDbBind.processBind(mDummyDOM, lContextUElem);
//  }
//
//  /**
//   * Test date conversion fails as expected.
//   * @throws ExActionFailed bind failed to process
//   */
//  @Test(expected=ExInternal.class)
//  public void testProcessBindDateConversionFailure3()
//  throws ExActionFailed {
//    InterfaceParameter lDbBind = new InterfaceParameter(mBindName, InterfaceParameter.SQLBIND_DATE, InterfaceParameter.FOXBIND_DATETIME, ".", 1, mDbStatement);
//    ContextUElem lContextUElem = mock(ContextUElem.class);
//    when(lContextUElem.extendedXPathString(any(DOM.class), anyString())).thenReturn("not-a-date");
//    Object lObj = lDbBind.processBind(mDummyDOM, lContextUElem);
//  }
}

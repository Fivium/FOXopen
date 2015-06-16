package net.foxopen.fox.module;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * Test that displayOrder, displayBefore and displayAfter all work
 */
public class DisplayOrderTest {
  TestDisplayOrderSortableImpl field1;
  TestDisplayOrderSortableImpl field2;
  TestDisplayOrderSortableImpl field3;
  TestDisplayOrderSortableImpl field4;
  TestDisplayOrderSortableImpl field5;
  TestDisplayOrderSortableImpl field6;
  TestDisplayOrderSortableImpl phantom_35;
  TestDisplayOrderSortableImpl field_2_75;
  TestDisplayOrderSortableImpl field_2_5;
  TestDisplayOrderSortableImpl field_4_5;
  TestDisplayOrderSortableImpl field_3_75;

  /**
   * Init some commonly used sortable items for use in subsequent tests
   */
  @Before
  public void before(){
    field1 = new TestDisplayOrderSortableImpl("FIELD_1");
    field1.mDisplayOrder = "10";

    field2 = new TestDisplayOrderSortableImpl("FIELD_2");
    field2.mDisplayOrder = "20";

    field3 = new TestDisplayOrderSortableImpl("FIELD_3");
    field3.mDisplayOrder = "30";

    field4 = new TestDisplayOrderSortableImpl("FIELD_4");
    field4.mDisplayOrder = "40";

    field5 = new TestDisplayOrderSortableImpl("FIELD_5");
    field5.mDisplayOrder = "50";

    field6 = new TestDisplayOrderSortableImpl("FIELD_6");
    field6.mDisplayOrder = "60";

    field_2_5 = new TestDisplayOrderSortableImpl("FIELD_2_5");
    field_2_5.mDisplayOrder = "1";
    field_2_5.mDisplayAfter = "FIELD_2";

    field_2_75 = new TestDisplayOrderSortableImpl("FIELD_2_75");
    field_2_75.mDisplayOrder = "2";
    field_2_75.mDisplayAfter = "FIELD_2";

    phantom_35 = new TestDisplayOrderSortableImpl("FIELD_3_5");
    phantom_35.mDisplayOrder = "35";

    field_3_75 = new TestDisplayOrderSortableImpl("FIELD_3_75");
    field_3_75.mDisplayAfter = "FIELD_3_5";

    field_4_5 = new TestDisplayOrderSortableImpl("FIELD_4_5");
    field_4_5.mDisplayBefore = "FIELD_5";
  }

  /**
   * Test that if all items have displayOrder they come out in order
   */
  @Test
  public void testBasicDisplayOrder(){
    List<DisplayOrderSortable> lSortableItemList = new ArrayList<DisplayOrderSortable>();

    lSortableItemList.add(field1);
    lSortableItemList.add(field2);
    lSortableItemList.add(field3);
    lSortableItemList.add(field4);
    lSortableItemList.add(field5);
    lSortableItemList.add(field6);

    DisplayOrder.sort(lSortableItemList);

    assertEquals("FIELD_1", lSortableItemList.get(0).getName());
    assertEquals("FIELD_2", lSortableItemList.get(1).getName());
    assertEquals("FIELD_3", lSortableItemList.get(2).getName());
    assertEquals("FIELD_4", lSortableItemList.get(3).getName());
    assertEquals("FIELD_5", lSortableItemList.get(4).getName());
    assertEquals("FIELD_6", lSortableItemList.get(5).getName());
  }

  /**
   * Test that if all items have displayOrder they come out in order despite being randomly shuffled on the way in
   */
  @Test
  public void testShuffledBasicDisplayOrder(){
    List<DisplayOrderSortable> lSortableItemList = new ArrayList<DisplayOrderSortable>();

    lSortableItemList.add(field1);
    lSortableItemList.add(field2);
    lSortableItemList.add(field3);
    lSortableItemList.add(field4);
    lSortableItemList.add(field5);
    lSortableItemList.add(field6);

    for (int i = 0; i < 100; i++) {
      Collections.shuffle(lSortableItemList, new Random());

      DisplayOrder.sort(lSortableItemList);

      assertEquals("FIELD_1", lSortableItemList.get(0).getName());
      assertEquals("FIELD_2", lSortableItemList.get(1).getName());
      assertEquals("FIELD_3", lSortableItemList.get(2).getName());
      assertEquals("FIELD_4", lSortableItemList.get(3).getName());
      assertEquals("FIELD_5", lSortableItemList.get(4).getName());
      assertEquals("FIELD_6", lSortableItemList.get(5).getName());
    }
  }

  /**
   * Items with no display order should show up after item with display order. They should be sorted in the order they
   * were in the list originally
   */
  @Test
  public void testAutoDisplayOrder(){
    List<DisplayOrderSortable> lSortableItemList = new ArrayList<DisplayOrderSortable>();

    lSortableItemList.add(field1);
    lSortableItemList.add(new TestDisplayOrderSortableImpl("AUTO_1"));
    lSortableItemList.add(new TestDisplayOrderSortableImpl("AUTO_2"));
    lSortableItemList.add(new TestDisplayOrderSortableImpl("AUTO_3"));
    lSortableItemList.add(new TestDisplayOrderSortableImpl("AUTO_4"));
    lSortableItemList.add(field6);

    DisplayOrder.sort(lSortableItemList);

    assertEquals("FIELD_1", lSortableItemList.get(0).getName());
    assertEquals("FIELD_6", lSortableItemList.get(1).getName());
    assertEquals("AUTO_1", lSortableItemList.get(2).getName());
    assertEquals("AUTO_2", lSortableItemList.get(3).getName());
    assertEquals("AUTO_3", lSortableItemList.get(4).getName());
    assertEquals("AUTO_4", lSortableItemList.get(5).getName());
  }

  /**
   * Items with displayBefore should show up before the named item they target
   */
  @Test
  public void testDisplayBefore(){
    List<DisplayOrderSortable> lSortableItemList = new ArrayList<DisplayOrderSortable>();

    lSortableItemList.add(field1);
    lSortableItemList.add(field2);
    lSortableItemList.add(field3);

    // Add item to show before field2
    TestDisplayOrderSortableImpl lBeforeField2 = new TestDisplayOrderSortableImpl("BEFORE_FIELD_2");
    lBeforeField2.mDisplayBefore = "FIELD_2";
    lSortableItemList.add(lBeforeField2);

    DisplayOrder.sort(lSortableItemList);

    assertEquals("FIELD_1", lSortableItemList.get(0).getName());
    assertEquals("BEFORE_FIELD_2", lSortableItemList.get(1).getName());
    assertEquals("FIELD_2", lSortableItemList.get(2).getName());
    assertEquals("FIELD_3", lSortableItemList.get(3).getName());
  }

  /**
   * Items with displayAfter should show up before the named item they target
   */
  @Test
  public void testDisplayAfter(){
    List<DisplayOrderSortable> lSortableItemList = new ArrayList<DisplayOrderSortable>();

    lSortableItemList.add(field1);
    lSortableItemList.add(field2);
    lSortableItemList.add(field3);

    // Add item to show after field2
    TestDisplayOrderSortableImpl lAfterField2 = new TestDisplayOrderSortableImpl("AFTER_FIELD_2");
    lAfterField2.mDisplayAfter = "FIELD_2";
    lSortableItemList.add(lAfterField2);

    DisplayOrder.sort(lSortableItemList);

    assertEquals("FIELD_1", lSortableItemList.get(0).getName());
    assertEquals("FIELD_2", lSortableItemList.get(1).getName());
    assertEquals("AFTER_FIELD_2", lSortableItemList.get(2).getName());
    assertEquals("FIELD_3", lSortableItemList.get(3).getName());
  }

  /**
   * Items with displayBefore should show up before the named item they target. If more than one item has the same
   * displayBefore they can be sorted by a displayOrder as well.
   */
  @Test
  public void testDisplayBeforeMultiple(){
    List<DisplayOrderSortable> lSortableItemList = new ArrayList<DisplayOrderSortable>();

    lSortableItemList.add(field1);
    lSortableItemList.add(field2);
    lSortableItemList.add(field3);

    // Add items to show before field2
    TestDisplayOrderSortableImpl lBeforeField2A = new TestDisplayOrderSortableImpl("BEFORE_FIELD_2_A");
    lBeforeField2A.mDisplayBefore = "FIELD_2";
    lBeforeField2A.mDisplayOrder = "1";
    lSortableItemList.add(lBeforeField2A);

    TestDisplayOrderSortableImpl lBeforeField2B = new TestDisplayOrderSortableImpl("BEFORE_FIELD_2_B");
    lBeforeField2B.mDisplayBefore = "FIELD_2";
    lBeforeField2B.mDisplayOrder = "2";
    lSortableItemList.add(lBeforeField2B);

    // Test this multiple times with a shuffled list
    for (int i = 0; i < 50; i++) {
      Collections.shuffle(lSortableItemList, new Random());

      DisplayOrder.sort(lSortableItemList);

      assertEquals("FIELD_1", lSortableItemList.get(0).getName());
      assertEquals("BEFORE_FIELD_2_A", lSortableItemList.get(1).getName()); // A comes before B as it has a lower displayOrder
      assertEquals("BEFORE_FIELD_2_B", lSortableItemList.get(2).getName());
      assertEquals("FIELD_2", lSortableItemList.get(3).getName());
      assertEquals("FIELD_3", lSortableItemList.get(4).getName());
    }
  }

  /**
   * Items with displayAfter should show up after the named item they target. If more than one item has the same
   * displayAfter they can be sorted by a displayOrder as well.
   */
  @Test
  public void testDisplayAfterMultiple(){
    List<DisplayOrderSortable> lSortableItemList = new ArrayList<DisplayOrderSortable>();

    lSortableItemList.add(field1);
    lSortableItemList.add(field2);
    lSortableItemList.add(field3);

    // Add items to show after field2
    TestDisplayOrderSortableImpl lAfterField2A = new TestDisplayOrderSortableImpl("AFTER_FIELD_2_A");
    lAfterField2A.mDisplayAfter = "FIELD_2";
    lAfterField2A.mDisplayOrder = "1";
    lSortableItemList.add(lAfterField2A);

    TestDisplayOrderSortableImpl lAfterField2B = new TestDisplayOrderSortableImpl("AFTER_FIELD_2_B");
    lAfterField2B.mDisplayAfter = "FIELD_2";
    lAfterField2B.mDisplayOrder = "2";
    lSortableItemList.add(lAfterField2B);

    // Test this multiple times with a shuffled list
    for (int i = 0; i < 50; i++) {
      Collections.shuffle(lSortableItemList, new Random());

      DisplayOrder.sort(lSortableItemList);

      assertEquals("FIELD_1", lSortableItemList.get(0).getName());
      assertEquals("FIELD_2", lSortableItemList.get(1).getName());
      assertEquals("AFTER_FIELD_2_A", lSortableItemList.get(2).getName()); // A comes before B as it has a lower displayOrder
      assertEquals("AFTER_FIELD_2_B", lSortableItemList.get(3).getName());
      assertEquals("FIELD_3", lSortableItemList.get(4).getName());
    }
  }

  /**
   * Test that items come out in the right order that I'd expect them to come out in based on the schema in the
   * test module: FOX5_FORM_LAYOUT
   */
  @Test
  public void testFullExample(){
    List<DisplayOrderSortable> lSortableItemList = new ArrayList<DisplayOrderSortable>();

    // This is the same order as the schema in FOX5_FORM_LAYOUT :{theme}/DISPLAY_ORDER_FORM
    lSortableItemList.add(field1);
    lSortableItemList.add(field2);
    lSortableItemList.add(field3);
    lSortableItemList.add(field6);
    lSortableItemList.add(field5);
    lSortableItemList.add(field4);
    lSortableItemList.add(phantom_35);
    lSortableItemList.add(field_2_75);
    lSortableItemList.add(field_2_5);
    lSortableItemList.add(field_4_5);
    lSortableItemList.add(field_3_75);

    DisplayOrder.sort(lSortableItemList);

    assertEquals("FIELD_1", lSortableItemList.get(0).getName());
    assertEquals("FIELD_2", lSortableItemList.get(1).getName());
    assertEquals("FIELD_2_5", lSortableItemList.get(2).getName());
    assertEquals("FIELD_2_75", lSortableItemList.get(3).getName());
    assertEquals("FIELD_3", lSortableItemList.get(4).getName());
    assertEquals("FIELD_3_5", lSortableItemList.get(5).getName());
    assertEquals("FIELD_3_75", lSortableItemList.get(6).getName());
    assertEquals("FIELD_4", lSortableItemList.get(7).getName());
    assertEquals("FIELD_4_5", lSortableItemList.get(8).getName());
    assertEquals("FIELD_5", lSortableItemList.get(9).getName());
    assertEquals("FIELD_6", lSortableItemList.get(10).getName());
  }

  /**
   * Simple implementation of the DisplayOrderSortable interface to test with
   */
  private class TestDisplayOrderSortableImpl implements DisplayOrderSortable {
    public static final String AUTO_ATTR_VALUE = "auto";

    public String mDisplayBefore = null;
    public String mDisplayAfter = null;
    public String mDisplayOrder = AUTO_ATTR_VALUE;
    public String mName;

    public TestDisplayOrderSortableImpl(String pName) {
      mName = pName;
    }

    @Override
    public String getDisplayBeforeAttribute() {
      return mDisplayBefore;
    }

    @Override
    public String getDisplayAfterAttribute() {
      return mDisplayAfter;
    }

    @Override
    public String getDisplayOrder() {
      return mDisplayOrder;
    }

    @Override
    public String getName() {
      return mName;
    }
  }

}

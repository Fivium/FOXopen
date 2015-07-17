package net.foxopen.fox.module.fieldset.transformer.html;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DefaultHTMLValueTransformerTest {

  @Test
  public void testDefaultHTMLValueTransformer()
  throws Exception {

    HTMLTransformConfig lTransformConfig = HTMLTransformConfig.DEFAULT_STANDARD_INSTANCE;

    String lInput = "<b onclick=\"javascript\">Hello world</b>";
    String lOutput = DefaultHTMLValueTransformer.parseSubmittedValue(lInput, lTransformConfig).outputNodeContentsToString(false);
    assertEquals("Disallowed attribute removed from tags", "<b>Hello world</b>", lOutput);

    lInput = "Hello&nbsp;world";
    lOutput = DefaultHTMLValueTransformer.parseSubmittedValue(lInput, lTransformConfig).outputNodeContentsToString(false);
    assertEquals("NBSP transformed to space character", "Hello world", lOutput);

    lInput = "Hello<em> </em>world, hello<strong> </strong>world, hello<u></u>world";
    lOutput = DefaultHTMLValueTransformer.parseSubmittedValue(lInput, lTransformConfig).outputNodeContentsToString(false);
    assertEquals("Empty em and strong tags removed", "Hello world, hello world, helloworld", lOutput);

    lInput = "<em>italics</em>, <strong>bold</strong>";
    lOutput = DefaultHTMLValueTransformer.parseSubmittedValue(lInput, lTransformConfig).outputNodeContentsToString(false);
    assertEquals("Em and strong translated to i and b", "<i>italics</i>, <b>bold</b>", lOutput);

    lInput = "<h1>h1</h1>, <h2>h2</h2>, <h3>h3</h3>, <h4>h4</h4>, <h5>h5</h5>, <h6>h6</h6>";
    lOutput = DefaultHTMLValueTransformer.parseSubmittedValue(lInput, lTransformConfig).outputNodeContentsToString(false);
    assertEquals("Heading tags translated to paragraphs", "<p align=\"justify\">h1</p>, <p align=\"justify\">h2</p>, <p align=\"justify\">h3</p>, <p align=\"justify\">h4</p>, <p align=\"justify\">h5</p>, <p align=\"justify\">h6</p>", lOutput);

    lInput = "<div>div</div>";
    lOutput = DefaultHTMLValueTransformer.parseSubmittedValue(lInput, lTransformConfig).outputNodeContentsToString(false);
    assertEquals("Div tags translated to paragraphs", "<p align=\"justify\">div</p>", lOutput);

    lInput = "<p><p>nested paragraph</p></p>";
    lOutput = DefaultHTMLValueTransformer.parseSubmittedValue(lInput, lTransformConfig).outputNodeContentsToString(false);
    assertEquals("Nested p tags are unnested", "<p align=\"justify\">nested paragraph</p>", lOutput);

    lInput = "<p></p><p>paragraph</p>";
    lOutput = DefaultHTMLValueTransformer.parseSubmittedValue(lInput, lTransformConfig).outputNodeContentsToString(false);
    assertEquals("Empty p tags are removed", "<p align=\"justify\">paragraph</p>", lOutput);

    lInput = "<p align=\"left\">left paragraph</p><p align=\"right\">right paragraph</p><p align=\"center\">center paragraph</p><p align=\"justify\">justify paragraph</p>";
    lOutput = DefaultHTMLValueTransformer.parseSubmittedValue(lInput, lTransformConfig).outputNodeContentsToString(false);
    assertEquals("Valid paragraph alignments are preserved", "<p align=\"left\">left paragraph</p><p align=\"right\">right paragraph</p><p align=\"center\">center paragraph</p><p align=\"justify\">justify paragraph</p>", lOutput);

    lInput = "<p align=\"dodgy\">dodgy paragraph</p>";
    lOutput = DefaultHTMLValueTransformer.parseSubmittedValue(lInput, lTransformConfig).outputNodeContentsToString(false);
    assertEquals("Invalid paragraph alignments are rewritten", "<p align=\"justify\">dodgy paragraph</p>", lOutput);

    lInput = "<ul>allowed</ul> <script>not allowed</script>";
    lOutput = DefaultHTMLValueTransformer.parseSubmittedValue(lInput, lTransformConfig).outputNodeContentsToString(false);
    assertEquals("Allowed tags are preserved, disallowed tags are removed", "<ul>allowed</ul> not allowed", lOutput);

    lInput = "<!--comment--><b>text</b>";
    lOutput = DefaultHTMLValueTransformer.parseSubmittedValue(lInput, lTransformConfig).outputNodeContentsToString(false);
    assertEquals("Comment nodes are removed", "<b>text</b>", lOutput);

    lInput = "<?mypi ?><b>text</b>";
    lOutput = DefaultHTMLValueTransformer.parseSubmittedValue(lInput, lTransformConfig).outputNodeContentsToString(false);
    assertEquals("PIs are removed", "<b>text</b>", lOutput);

    //Processing should happen in this order:
    //onclick attr removed, script tag removed, h1 converted to p, p unnested from parent (*2)
    lInput = "<p><p><h1>nested <b onclick=\"xxx\">bold</b> <script>script</script> text</h1></p></p>";
    lOutput = DefaultHTMLValueTransformer.parseSubmittedValue(lInput, lTransformConfig).outputNodeContentsToString(false);
    assertEquals("Processing rules are applied recursively", "<p align=\"justify\">nested <b>bold</b> script text</p>", lOutput);
  }

  @Test
  public void testDefaultHTMLValueTransformer_MMFieldParsing()
  throws Exception {
    HTMLTransformConfig lTransformConfig = HTMLTransformConfig.DEFAULT_STANDARD_INSTANCE;

    String lInput = "<b>Field value <span style=\"background-color:yellow\">[[MY_FIELD]]</span></b>";
    String lOutput = DefaultHTMLValueTransformer.parseSubmittedValue(lInput, lTransformConfig).outputNodeContentsToString(false);
    assertEquals("MM fields are converted to MM tags", "<b>Field value <MM>MY_FIELD</MM></b>", lOutput);

    lInput = "<b>Field value <span style=\"background-color:yellow\">[[MY_FIELD]</span></b>";
    lOutput = DefaultHTMLValueTransformer.parseSubmittedValue(lInput, lTransformConfig).outputNodeContentsToString(false);
    assertEquals("Malformed MM fields are not converted to MM tags", "<b>Field value [[MY_FIELD]</b>", lOutput);

    lInput = "<b>Field value <span style=\"background-color:yellow\">[[MY_FIELD]</span></b>";
    lOutput = DefaultHTMLValueTransformer.parseSubmittedValue(lInput, lTransformConfig).outputNodeContentsToString(false);
    assertEquals("Malformed MM fields are not converted to MM tags", "<b>Field value [[MY_FIELD]</b>", lOutput);

    lInput = "<b>Field value <span style=\"background-color:yellow\">[[MY_FIELD] more text</span></b>";
    lOutput = DefaultHTMLValueTransformer.parseSubmittedValue(lInput, lTransformConfig).outputNodeContentsToString(false);
    assertEquals("Malformed MM fields are not converted to MM tags (with trailing text after malformed field)", "<b>Field value [[MY_FIELD] more text</b>", lOutput);

    lInput = "<b>Field value <span style=\"background-color:yellow\">[[MY[FIELD]]</span></b>";
    lOutput = DefaultHTMLValueTransformer.parseSubmittedValue(lInput, lTransformConfig).outputNodeContentsToString(false);
    assertEquals("Malformed MM fields are not converted to MM tags", "<b>Field value [[MY[FIELD]]</b>", lOutput);

    lInput = "Field value <b>[[MY_FIELD</b>]]";
    lOutput = DefaultHTMLValueTransformer.parseSubmittedValue(lInput, lTransformConfig).outputNodeContentsToString(false);
    assertEquals("Tags correctly reordered if MM field delimiters span across them", "Field value <b><MM>MY_FIELD</MM></b>", lOutput);

    lInput = "Field value [[<b>MY_FIELD]]</b>";
    lOutput = DefaultHTMLValueTransformer.parseSubmittedValue(lInput, lTransformConfig).outputNodeContentsToString(false);
    assertEquals("Tags correctly removed if MM field delimiters span across them", "Field value <MM>MY_FIELD</MM>", lOutput);

    //Probably shouldn't be correct
    lInput = "Field value [[]]";
    lOutput = DefaultHTMLValueTransformer.parseSubmittedValue(lInput, lTransformConfig).outputNodeContentsToString(false);
    assertEquals("Empty delimiter converted to MM tag", "Field value <MM/>", lOutput);

    lInput = "[";
    lOutput = DefaultHTMLValueTransformer.parseSubmittedValue(lInput, lTransformConfig).outputNodeContentsToString(false);
    assertEquals("Malformed MM field delimiters are not modified", "[", lOutput);

    lInput = "[]";
    lOutput = DefaultHTMLValueTransformer.parseSubmittedValue(lInput, lTransformConfig).outputNodeContentsToString(false);
    assertEquals("Malformed MM field delimiters are not modified", "[]", lOutput);

    lInput = "[[";
    lOutput = DefaultHTMLValueTransformer.parseSubmittedValue(lInput, lTransformConfig).outputNodeContentsToString(false);
    assertEquals("Malformed MM field delimiters are not modified", "[[", lOutput);

    lInput = "value [[";
    lOutput = DefaultHTMLValueTransformer.parseSubmittedValue(lInput, lTransformConfig).outputNodeContentsToString(false);
    assertEquals("Malformed MM field delimiters are not modified", "value [[", lOutput);

    lInput = "value ]]";
    lOutput = DefaultHTMLValueTransformer.parseSubmittedValue(lInput, lTransformConfig).outputNodeContentsToString(false);
    assertEquals("Malformed MM field delimiters are not modified", "value ]]", lOutput);

    lInput = "value [[]";
    lOutput = DefaultHTMLValueTransformer.parseSubmittedValue(lInput, lTransformConfig).outputNodeContentsToString(false);
    assertEquals("Malformed MM field delimiters are not modified", "value [[]", lOutput);

    lInput = "value [[] trailing";
    lOutput = DefaultHTMLValueTransformer.parseSubmittedValue(lInput, lTransformConfig).outputNodeContentsToString(false);
    assertEquals("Malformed MM field delimiters are not modified", "value [[] trailing", lOutput);
  }

  @Test
  public void testDefaultHTMLValueTransformer_AllowedTagNames()
  throws Exception {

    String lInput = "<blockquote>Hello world</blockquote>";

    String lOutput = DefaultHTMLValueTransformer.parseSubmittedValue(lInput, HTMLTransformConfig.DEFAULT_STANDARD_INSTANCE).outputNodeContentsToString(false);
    assertEquals("Element removed when disallowed by config", "Hello world", lOutput);

    lOutput = DefaultHTMLValueTransformer.parseSubmittedValue(lInput, HTMLTransformConfig.DEFAULT_EXPANDED_INSTANCE).outputNodeContentsToString(false);
    assertEquals("Element retained when allowed by config", "<blockquote>Hello world</blockquote>", lOutput);
  }

  @Test
  public void testDefaultHTMLValueTransformer_ExpandedMMFields()
  throws Exception {
    String lInput = "<b>Field value <span style=\"background-color:yellow\">[[MY_FIELD]]</span></b>";
    String lOutput = DefaultHTMLValueTransformer.parseSubmittedValue(lInput, HTMLTransformConfig.DEFAULT_EXPANDED_INSTANCE).outputNodeContentsToString(false);
    assertEquals("MM fields are not converted by expanded config", "<b>Field value [[MY_FIELD]]</b>", lOutput);
  }

  @Test
  public void testDefaultHTMLValueTransformer_MalformedInput()
  throws Exception {
    assertNull("Null value returned for invalid XML", DefaultHTMLValueTransformer.parseSubmittedValue("<malformed>hello</>",  HTMLTransformConfig.DEFAULT_STANDARD_INSTANCE));
  }
}

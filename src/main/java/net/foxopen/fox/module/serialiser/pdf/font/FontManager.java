package net.foxopen.fox.module.serialiser.pdf.font;

import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.FontSelector;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.serialiser.pdf.elementattributes.FontAttributes;

import java.util.Arrays;
import java.util.List;

/**
 * Loads fonts to be used in the document. Font selectors can then be created using the loaded fonts.
 */
public class FontManager {
  /**
   * The list of directories that can contain fonts to be registered
   */
  private static final List<String> FONT_DIRECTORY_RELATIVE_PATHS = Arrays.asList("/WEB-INF/fonts/OpenSans-print", "/WEB-INF/fonts/DejaVu-print");
  /**
   * The font family names that should be added to font selectors, in the order of priority
   */
  private static final List<String> FONT_FAMILY_NAMES = Arrays.asList("Open Sans", "DejaVu Sans");

  /**
   * Create a font manager with font directories registered
   */
  public FontManager() {
    FONT_DIRECTORY_RELATIVE_PATHS.forEach(this::registerDirectory);
  }

  /**
   * Register all fonts within a directory
   * @param pRelativePath The relative path to the font directory
   * @return The number of fonts registered within the directory
   */
  private int registerDirectory(String pRelativePath) {
    String lFullPath = FoxGlobals.getInstance().getServletContext().getRealPath(pRelativePath);
    return FontFactory.registerDirectory(lFullPath);
  }

  /**
   * Get a font with the provided font family and font attributes applied
   * @param pFontFamily The font family
   * @param pFontAttributes The font attributes to be applied
   * @return A font of the provided font family with font attributes applied
   * @throws ExInternal If the font family provided is not registered
   */
  private Font getFont(String pFontFamily, FontAttributes pFontAttributes) throws ExInternal {
    Font lFont = FontFactory.getFont(pFontFamily, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, pFontAttributes.getSize(),
                                     pFontAttributes.getStyle(), pFontAttributes.getColor());

    if (lFont.getBaseFont() == null) {
      throw new ExInternal("Could not create a font using font family '" + pFontFamily + "' as it could not be found in the registered fonts");
    }

    return lFont;
  }

  /**
   * Get a font selector with the fonts registered in the font manager added, with the provided font attributes applied
   * to each font
   * @param pFontAttributes The font attributes to apply to each selector font
   * @return A font selector with the registered fonts with the font attributes applied
   */
  public FontSelector getFontSelector(FontAttributes pFontAttributes) {
    FontSelector lFontSelector = new FontSelector();
    FONT_FAMILY_NAMES.stream()
                     .map(pFontFamily -> getFont(pFontFamily, pFontAttributes))
                     .forEach(lFontSelector::addFont);

    return lFontSelector;
  }
}

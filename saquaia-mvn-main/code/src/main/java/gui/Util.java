package gui;

import core.util.IO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

/**
 *
 * @author Martin
 */
public class Util {
    public static final Font FONT_MONO = new Font(Font.MONOSPACED, Font.PLAIN, 12);
    public static final Color COLOR_MAIN = new Color(139, 60, 127);
    public static final int BORDER_SIZE = 3;
    public static final int GAP_VERTICAL = 3;
    public static final int GAP_HORIZONTAL = 3;
    public static final int KEYBOARD_SHORTCUR_KEY_MASK = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    
    public static BufferedImage getIcon() {
        try {
            return ImageIO.read(new File(new File(IO.DATA_FOLDER, "logo"), "logo.png"));
        } catch (IOException e) {
        }
        return null;
    }
    
    public static Border getTitledBorder(String s) {
        return new TitledBorder(new LineBorder(Util.COLOR_MAIN, Util.BORDER_SIZE), s + " ");
    }
}

package ca.corbett.imageviewer.extensions.imagetransform;

import ca.corbett.imageviewer.extensions.ImageViewerExtension;
import ca.corbett.extensions.AppExtensionInfo;
import ca.corbett.extras.properties.AbstractProperty;
import ca.corbett.imageviewer.ui.MainWindow;

import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Wraps the old Transform dialog into its own extension.
 * TransformDialog was originally written for ImageViewer 1.3, but now in the
 * 2.x version, we can move some of this functionality out into extensions so
 * it can be disabled to remove it from the UI entirely if unneeded.
 *
 * @author scorbo2
 */
public class TransformImageExtension extends ImageViewerExtension {

    private final AppExtensionInfo extInfo;

    public TransformImageExtension() {
        extInfo = AppExtensionInfo.fromExtensionJar(getClass(),
            "/ca/corbett/imageviewer/extensions/imagetransform/extInfo.json");
        if (extInfo == null) {
            throw new RuntimeException("ImageTransformExtension: can't parse extInfo.json!");
        }
    }

    @Override
    public AppExtensionInfo getInfo() {
        return extInfo;
    }

    @Override
    protected List<AbstractProperty> createConfigProperties() {
        return null;
    }

    @Override
    public List<JMenuItem> getMenuItems(String topLevelMenu, MainWindow.BrowseMode browseMode) {
        if (browseMode == MainWindow.BrowseMode.IMAGE_SET) {
            return null; // we COULD allow transformations when browsing image sets...
        }
        if ("Edit".equals(topLevelMenu)) {
            List<JMenuItem> list = new ArrayList<>();
            list.add(buildMenuItem());
            return list;
        }
        return null;
    }

    @Override
    public List<JMenuItem> getPopupMenuItems(MainWindow.BrowseMode browseMode) {
        if (browseMode == MainWindow.BrowseMode.IMAGE_SET) {
            return null; // we COULD allow transformations when browsing image sets...
        }
        List<JMenuItem> list = new ArrayList<>();
        list.add(buildMenuItem());
        return list;
    }

    private JMenuItem buildMenuItem() {
        JMenuItem item = new JMenuItem(new TransformImageAction());
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK));
        return item;
    }

}
